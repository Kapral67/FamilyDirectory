package org.familydirectory.cdk.sss;

import java.util.List;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;
import software.amazon.awscdk.services.s3.CorsRule;
import software.amazon.awscdk.services.s3.HttpMethods;
import software.constructs.Construct;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static software.amazon.awscdk.RemovalPolicy.RETAIN_ON_UPDATE_OR_DELETE;
import static software.amazon.awscdk.services.s3.BlockPublicAccess.BLOCK_ALL;
import static software.amazon.awscdk.services.s3.BucketAccessControl.PRIVATE;
import static software.amazon.awscdk.services.s3.BucketEncryption.S3_MANAGED;
import static software.amazon.awscdk.services.s3.HttpMethods.GET;
import static software.amazon.awscdk.services.s3.ObjectOwnership.BUCKET_OWNER_ENFORCED;

public
class FamilyDirectorySssStack extends Stack {
    public static final String S3_PDF_BUCKET_RESOURCE_ID = "SssPdfBucket";
    public static final String S3_PDF_BUCKET_NAME_EXPORT_NAME = "%sName".formatted(S3_PDF_BUCKET_RESOURCE_ID);
    public static final String S3_PDF_BUCKET_ARN_EXPORT_NAME = "%sArn".formatted(S3_PDF_BUCKET_RESOURCE_ID);
    public static final Number S3_MINIMUM_TLS_VERSION = 1.2;
    public static final List<String> S3_PDF_BUCKET_CORS_ALLOWED_HEADERS = singletonList("authorization");
    public static final List<String> S3_PDF_BUCKET_CORS_ALLOWED_ORIGINS = singletonList("*");
    public static final List<HttpMethods> S3_PDF_BUCKET_CORS_ALLOWED_METHODS = singletonList(GET);

    public
    FamilyDirectorySssStack (final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        final BucketProps pdfBucketProps = BucketProps.builder()
                                                      .accessControl(PRIVATE)
                                                      .autoDeleteObjects(FALSE)
                                                      .blockPublicAccess(BLOCK_ALL)
                                                      .cors(singletonList(CorsRule.builder()
                                                                                  .allowedHeaders(S3_PDF_BUCKET_CORS_ALLOWED_HEADERS)
                                                                                  .allowedOrigins(S3_PDF_BUCKET_CORS_ALLOWED_ORIGINS)
                                                                                  .allowedMethods(S3_PDF_BUCKET_CORS_ALLOWED_METHODS)
                                                                                  .build()))
                                                      .encryption(S3_MANAGED)
                                                      .enforceSsl(TRUE)
                                                      .minimumTlsVersion(S3_MINIMUM_TLS_VERSION)
                                                      .objectOwnership(BUCKET_OWNER_ENFORCED)
                                                      .publicReadAccess(FALSE)
                                                      .removalPolicy(RETAIN_ON_UPDATE_OR_DELETE)
                                                      .versioned(TRUE)
                                                      .build();

        final Bucket pdfBucket = new Bucket(this, S3_PDF_BUCKET_RESOURCE_ID, pdfBucketProps);

        new CfnOutput(this, S3_PDF_BUCKET_NAME_EXPORT_NAME, CfnOutputProps.builder()
                                                                          .exportName(S3_PDF_BUCKET_NAME_EXPORT_NAME)
                                                                          .value(pdfBucket.getBucketName())
                                                                          .build());

        new CfnOutput(this, S3_PDF_BUCKET_ARN_EXPORT_NAME, CfnOutputProps.builder()
                                                                         .exportName(S3_PDF_BUCKET_ARN_EXPORT_NAME)
                                                                         .value(pdfBucket.getBucketArn())
                                                                         .build());
    }
}
