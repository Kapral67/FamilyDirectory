package org.familydirectory.assets.lambda.function.toolkitcleaner.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
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
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
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

        return stacks;
    }

    @NotNull
    public
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
    public
    ToolkitCleanerResponse cleanObjects (final @NotNull Set<String> assetHashes) {
        final Set<String> buckets = this.s3Client.listBuckets()
                                                 .buckets()
                                                 .stream()
                                                 .map(Bucket::name)
                                                 .filter(s -> s.contains(ASSET_BUCKET_PREFIX))
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
                                ++this.deletedItems[0];
                                final HeadObjectResponse headResponse = this.s3Client.headObject(HeadObjectRequest.builder()
                                                                                                                  .bucket(bucketName)
                                                                                                                  .key(obj.key())
                                                                                                                  .versionId(obj.versionId())
                                                                                                                  .build());
                                this.reclaimedBytes[0] += headResponse.contentLength();
                                this.s3Client.deleteObject(DeleteObjectRequest.builder()
                                                                              .bucket(bucketName)
                                                                              .key(obj.key())
                                                                              .versionId(obj.versionId())
                                                                              .build());
                            }
                        });

            } while (nonNull(keyMarker) && nonNull(versionIdMarker));
        });

        return new ToolkitCleanerResponse(this.deletedItems[0], this.reclaimedBytes[0]);
    }

    @Override
    public
    void close () {
        this.cfnClient.close();
        this.s3Client.close();
    }
}
