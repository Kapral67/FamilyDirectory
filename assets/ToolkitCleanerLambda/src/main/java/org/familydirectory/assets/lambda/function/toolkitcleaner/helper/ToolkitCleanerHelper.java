package org.familydirectory.assets.lambda.function.toolkitcleaner.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.familydirectory.assets.lambda.function.toolkitcleaner.records.ToolkitCleanerResponse;
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
import software.amazon.awssdk.utils.SdkAutoCloseable;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

public final
class ToolkitCleanerHelper implements SdkAutoCloseable {
    private static final int HASH_LENGTH = 64;
    private static final Pattern CFN_TEMPLATE_HASHES_PATTERN = Pattern.compile("[a-f0-9]{%d}".formatted(HASH_LENGTH));
    private static final String ASSET_BUCKET_PREFIX = "cdk";

    @NotNull
    private final CloudFormationClient cfnClient = CloudFormationClient.create();
    @NotNull
    private final S3Client s3Client = S3Client.create();
    private final long[] deletedItems = {0L};
    private final long[] reclaimedBytes = {0L};
    @NotNull
    private final LambdaLogger logger;

    public
    ToolkitCleanerHelper (final @NotNull LambdaLogger logger) {
        super();
        this.logger = requireNonNull(logger);
    }

    @NotNull
    public
    Set<String> getStackNames () {
        this.logger.log("[INFO]: BEGIN: ToolkitCleanerLambda getStackNames", LogLevel.INFO);
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
            if (nextToken.isBlank()) {
                nextToken = null;
            }
            stacks.addAll(response.stacks()
                                  .stream()
                                  .map(Stack::stackName)
                                  .collect(Collectors.toUnmodifiableSet()));
        } while (nonNull(nextToken));
        this.logger.log("[INFO]: END: ToolkitCleanerLambda getStackNames | StackNames: %s".formatted(stacks.toString()), LogLevel.INFO);
        return stacks;
    }

    @NotNull
    public
    Set<String> extractTemplateHashes (final @NotNull Set<String> stackNames) {
        this.logger.log("[INFO]: BEGIN: ToolkitCleanerLambda extractTemplateHashes | StackNames: %s".formatted(stackNames.toString()), LogLevel.INFO);
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
        this.logger.log("[INFO]: END: ToolkitCleanerLambda extractTemplateHashes | Hashes: %s".formatted(hashes.toString()), LogLevel.INFO);
        return hashes;
    }

    @NotNull
    public
    ToolkitCleanerResponse cleanObjects (final @NotNull Set<String> assetHashes) {
        final Set<String> buckets = this.s3Client.listBuckets()
                                                 .buckets()
                                                 .stream()
                                                 .map(Bucket::name)
                                                 .filter(s -> s.startsWith(ASSET_BUCKET_PREFIX))
                                                 .collect(Collectors.toUnmodifiableSet());
        this.logger.log("[INFO]: BEGIN: ToolkitCleanerLambda cleanObjects | AssetHashes: %s | Buckets: %s".formatted(assetHashes.toString(), buckets.toString()), LogLevel.INFO);
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
        this.logger.log("[INFO]: END ToolkitCleanerLambda cleanObjects | DeletedItems: %d | ReclaimedBytes: %s".formatted(this.deletedItems[0], this.reclaimedBytes[0]), LogLevel.INFO);
        return new ToolkitCleanerResponse(this.deletedItems[0], this.reclaimedBytes[0]);
    }

    @Override
    public
    void close () {
        this.cfnClient.close();
        this.s3Client.close();
    }
}
