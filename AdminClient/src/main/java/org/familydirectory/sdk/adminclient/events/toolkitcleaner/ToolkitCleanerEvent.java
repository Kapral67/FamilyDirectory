package org.familydirectory.sdk.adminclient.events.toolkitcleaner;

import io.leego.banana.Ansi;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.enums.ByteDivisor;
import org.familydirectory.sdk.adminclient.events.model.Executable;
import org.familydirectory.sdk.adminclient.records.ToolkitCleanerResponse;
import org.familydirectory.sdk.adminclient.utility.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateRequest;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
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
class ToolkitCleanerEvent implements Executable {
    private static final int HASH_LENGTH = 64;
    private static final Pattern CFN_TEMPLATE_HASHES_PATTERN = Pattern.compile("[a-f0-9]{%d}".formatted(HASH_LENGTH));
    private static final String ASSET_BUCKET_PREFIX = "cdk";
    private final long[] deletedItems = {0L};
    private final long[] reclaimedBytes = {0L};
    private final @NotNull S3Client s3Client = S3Client.builder()
                                                       .crossRegionAccessEnabled(true)
                                                       .build();
    private final @NotNull CloudFormationClient cfnClient = CloudFormationClient.create();
    private final @NotNull Scanner scanner;

    public
    ToolkitCleanerEvent (final @NotNull Scanner scanner) {
        super();
        this.scanner = requireNonNull(scanner);
    }

    @Override
    public @NotNull
    Scanner scanner () {
        return this.scanner;
    }

    @Override
    @Deprecated
    @NotNull
    public
    MemberRecord getExistingMember (final @NotNull String message) {
        throw new UnsupportedOperationException("Existing Members Unavailable; Picket Not Implemented");
    }

    @Override
    @Deprecated
    @NotNull
    public
    List<MemberRecord> getPickerEntries () {
        throw new UnsupportedOperationException("Picker Not Implemented");
    }

    @Override
    @Deprecated
    public
    void validateMemberEmailIsUnique (final @Nullable String memberEmail) {
        throw new UnsupportedOperationException("DynamoDbClient Not Implemented");
    }

    @Override
    public
    void run () {
        Logger.customLine("Clean CDK S3 Assets? (Y/n)", Ansi.BOLD, Ansi.BLUE);
        final String choice = this.scanner.nextLine()
                                          .trim();
        if (!choice.isBlank()) {
            System.out.println();
            if (choice.equalsIgnoreCase("n")) {
                return;
            }
        }

        final ToolkitCleanerResponse response = this.cleanObjects(this.extractTemplateHashes(this.getStackNames()));

        Logger.custom("", "Deleted Items: %d | ".formatted(response.deletedItems()), Ansi.BOLD, Ansi.PURPLE);
        final double bytes = response.reclaimed(ByteDivisor.NONE);
        if (bytes < ByteDivisor.KILO.divisor()) {
            Logger.customLine("Reclaimed Space: %d Bytes".formatted(response.reclaimedBytes()), Ansi.BOLD, Ansi.PURPLE);
        } else if (bytes < ByteDivisor.MEGA.divisor()) {
            Logger.customLine("Reclaimed Space: %f KiB".formatted(response.reclaimed(ByteDivisor.KIBI)), Ansi.BOLD, Ansi.PURPLE);
        } else if (bytes < ByteDivisor.GIGA.divisor()) {
            Logger.customLine("Reclaimed Space: %f MiB".formatted(response.reclaimed(ByteDivisor.MEBI)), Ansi.BOLD, Ansi.PURPLE);
        } else {
            Logger.customLine("Reclaimed Space: %f GiB".formatted(response.reclaimed(ByteDivisor.GIBI)), Ansi.BOLD, Ansi.PURPLE);
        }

        System.out.println();
    }

    @NotNull
    private
    Set<String> getStackNames () {
        final Set<String> stacks = new HashSet<>();
        String nextToken = null;
        do {
            final DescribeStacksResponse response;
            if (isNull(nextToken)) {
                response = this.cfnClient.describeStacks();
            } else {
                response = this.cfnClient.describeStacks(DescribeStacksRequest.builder()
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
            final String templateBody = this.cfnClient.getTemplate(GetTemplateRequest.builder()
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
        final Set<String> buckets = this.s3Client.listBuckets()
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
                    response = this.s3Client.listObjectVersions(requestBuilder.build());
                } else if (isNull(keyMarker)) {
                    response = this.s3Client.listObjectVersions(requestBuilder.versionIdMarker(versionIdMarker)
                                                                              .build());
                } else if (isNull(versionIdMarker)) {
                    response = this.s3Client.listObjectVersions(requestBuilder.keyMarker(keyMarker)
                                                                              .build());
                } else {
                    response = this.s3Client.listObjectVersions(ListObjectVersionsRequest.builder()
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
                                this.reclaimedBytes[0] += this.s3Client.headObject(HeadObjectRequest.builder()
                                                                                                    .bucket(bucketName)
                                                                                                    .key(obj.key())
                                                                                                    .versionId(obj.versionId())
                                                                                                    .build())
                                                                       .contentLength();
                                this.s3Client.deleteObject(DeleteObjectRequest.builder()
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

    @Override
    @Deprecated
    @Nullable
    public
    Map<String, AttributeValue> getDdbItem (final @NotNull String primaryKey, final @NotNull DdbTable ddbTable) {
        throw new UnsupportedOperationException("DynamoDbClient Not Implemented");
    }

    @Override
    @Deprecated
    @NotNull
    public
    DynamoDbClient getDynamoDbClient () {
        throw new UnsupportedOperationException("DynamoDbClient Not Implemented");
    }

    @Override
    public
    void close () {
        this.s3Client.close();
        this.cfnClient.close();
    }
}
