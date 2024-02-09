package org.familydirectory.sdk.adminclient.events.toolkitcleaner;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.familydirectory.sdk.adminclient.enums.ByteDivisor;
import org.familydirectory.sdk.adminclient.enums.Commands;
import org.familydirectory.sdk.adminclient.records.ToolkitCleanerResponse;
import org.familydirectory.sdk.adminclient.utility.S3CrossRegionClient;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateRequest;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

public final
class ToolkitCleanerEvent implements Runnable {
    private static final int HASH_LENGTH = 64;
    private static final Pattern CFN_TEMPLATE_HASHES_PATTERN = Pattern.compile("[a-f0-9]{%d}".formatted(HASH_LENGTH));
    private static final String ASSET_BUCKET_PREFIX = "cdk";
    private final long[] deletedItems = {0L};
    private final long[] reclaimedBytes = {0L};
    private final @NotNull WindowBasedTextGUI gui;

    public
    ToolkitCleanerEvent (final @NotNull WindowBasedTextGUI gui) {
        super();
        this.gui = requireNonNull(gui);
    }

    @Override
    public
    void run () {
        final MessageDialog msgDialog = new MessageDialogBuilder().setTitle(Commands.TOOLKIT_CLEANER.name())
                                                                  .setText("Clean CDK S3 Assets?")
                                                                  .addButton(MessageDialogButton.Yes)
                                                                  .addButton(MessageDialogButton.No)
                                                                  .build();
        if (msgDialog.showDialog(this.gui)
                     .equals(MessageDialogButton.Yes))
        {
            final ToolkitCleanerResponse response = this.cleanObjects(this.extractTemplateHashes(this.getStackNames()));
            final StringBuilder responseBuilder = new StringBuilder("Deleted Items: %d | ".formatted(response.deletedItems()));
            final double bytes = response.reclaimed(ByteDivisor.NONE);
            if (bytes < ByteDivisor.KILO.divisor()) {
                responseBuilder.append("Reclaimed Space: %d Bytes".formatted(response.reclaimedBytes()));
            } else if (bytes < ByteDivisor.MEGA.divisor()) {
                responseBuilder.append("Reclaimed Space: %f KiB".formatted(response.reclaimed(ByteDivisor.KIBI)));
            } else if (bytes < ByteDivisor.GIGA.divisor()) {
                responseBuilder.append("Reclaimed Space: %f MiB".formatted(response.reclaimed(ByteDivisor.MEBI)));
            } else {
                responseBuilder.append("Reclaimed Space: %f GiB".formatted(response.reclaimed(ByteDivisor.GIBI)));
            }
            new MessageDialogBuilder().setTitle(Commands.TOOLKIT_CLEANER.name())
                                      .setText(responseBuilder.toString())
                                      .addButton(MessageDialogButton.OK)
                                      .build()
                                      .showDialog(this.gui);
        }
    }

    @NotNull
    private
    Set<String> getStackNames () {
        final CloudFormationClient cfnClient = SdkClientProvider.getSdkClientProvider()
                                                                .getSdkClient(CloudFormationClient.class);
        final Set<String> stacks = new HashSet<>();
        String nextToken = null;
        do {
            final DescribeStacksResponse response;
            if (isNull(nextToken)) {
                response = cfnClient.describeStacks();
            } else {
                response = cfnClient.describeStacks(DescribeStacksRequest.builder()
                                                                         .nextToken(nextToken)
                                                                         .build());
            }
            nextToken = response.nextToken();
            stacks.addAll(response.stacks()
                                  .stream()
                                  .map(Stack::stackName)
                                  .collect(Collectors.toUnmodifiableSet()));
        } while (nonNull(nextToken));
        return stacks;
    }

    @NotNull
    private
    Set<String> extractTemplateHashes (final @NotNull Set<String> stackNames) {
        final Set<String> hashes = new HashSet<>();
        stackNames.forEach(stackName -> {
            final String templateBody = SdkClientProvider.getSdkClientProvider()
                                                         .getSdkClient(CloudFormationClient.class)
                                                         .getTemplate(GetTemplateRequest.builder()
                                                                                        .stackName(stackName)
                                                                                        .build())
                                                         .templateBody();
            final Matcher hashesMatcher = CFN_TEMPLATE_HASHES_PATTERN.matcher(templateBody);
            while (hashesMatcher.find()) {
                hashes.add(hashesMatcher.group());
            }
        });
        return hashes;
    }

    @NotNull
    private
    ToolkitCleanerResponse cleanObjects (final @NotNull Set<String> assetHashes) {
        final S3Client s3Client = SdkClientProvider.getSdkClientProvider()
                                                   .getSdkClient(S3CrossRegionClient.class);
        final Set<String> buckets = s3Client.listBuckets()
                                            .buckets()
                                            .stream()
                                            .map(Bucket::name)
                                            .filter(s -> s.startsWith(ASSET_BUCKET_PREFIX))
                                            .collect(Collectors.toUnmodifiableSet());
        buckets.forEach(bucketName -> {
            String keyMarker = null;
            String versionIdMarker = null;
            do {
                final ListObjectVersionsRequest.Builder requestBuilder = ListObjectVersionsRequest.builder()
                                                                                                  .bucket(bucketName);
                final ListObjectVersionsResponse response;
                if (isNull(keyMarker) && isNull(versionIdMarker)) {
                    response = s3Client.listObjectVersions(requestBuilder.build());
                } else if (isNull(keyMarker)) {
                    response = s3Client.listObjectVersions(requestBuilder.versionIdMarker(versionIdMarker)
                                                                         .build());
                } else if (isNull(versionIdMarker)) {
                    response = s3Client.listObjectVersions(requestBuilder.keyMarker(keyMarker)
                                                                         .build());
                } else {
                    response = s3Client.listObjectVersions(ListObjectVersionsRequest.builder()
                                                                                    .bucket(bucketName)
                                                                                    .keyMarker(keyMarker)
                                                                                    .versionIdMarker(versionIdMarker)
                                                                                    .build());
                }
                keyMarker = response.nextKeyMarker();
                versionIdMarker = response.nextVersionIdMarker();
                response.versions()
                        .forEach(obj -> {
                            final String hash = obj.key()
                                                   .substring(0, HASH_LENGTH);
                            if (!assetHashes.contains(hash)) {
                                this.reclaimedBytes[0] += s3Client.headObject(HeadObjectRequest.builder()
                                                                                               .bucket(bucketName)
                                                                                               .key(obj.key())
                                                                                               .versionId(obj.versionId())
                                                                                               .build())
                                                                  .contentLength();
                                s3Client.deleteObject(DeleteObjectRequest.builder()
                                                                         .bucket(bucketName)
                                                                         .key(obj.key())
                                                                         .versionId(obj.versionId())
                                                                         .build());
                                ++this.deletedItems[0];
                            }
                        });

            } while (nonNull(keyMarker) || nonNull(versionIdMarker));
        });
        return new ToolkitCleanerResponse(this.deletedItems[0], this.reclaimedBytes[0]);
    }
}
