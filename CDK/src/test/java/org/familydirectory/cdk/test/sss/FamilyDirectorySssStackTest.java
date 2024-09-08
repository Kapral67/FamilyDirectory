package org.familydirectory.cdk.test.sss;

import java.util.List;
import java.util.Map;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.sss.FamilyDirectorySssStack;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Template;
import software.amazon.awscdk.services.s3.HttpMethods;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awscdk.assertions.Match.objectLike;

public
class FamilyDirectorySssStackTest {
    @Test
    public
    void testStack () {
        final App app = new App();

        final FamilyDirectorySssStack stack = new FamilyDirectorySssStack(app, FamilyDirectoryCdkApp.SSS_STACK_NAME, StackProps.builder()
                                                                                                                               .env(FamilyDirectoryCdkApp.DEFAULT_ENV)
                                                                                                                               .stackName(FamilyDirectoryCdkApp.SSS_STACK_NAME)
                                                                                                                               .build());

        final Template template = Template.fromStack(stack);

        final Map<String, Map<String, Object>> bucketMap = template.findResources("AWS::S3::Bucket", objectLike(singletonMap("Properties", Map.of("AccessControl", "Private", "BucketEncryption",
                                                                                                                                                  singletonMap("ServerSideEncryptionConfiguration",
                                                                                                                                                               singletonList(singletonMap(
                                                                                                                                                                       "ServerSideEncryptionByDefault",
                                                                                                                                                                       singletonMap("SSEAlgorithm",
                                                                                                                                                                                    "AES256")))),
                                                                                                                                                  "CorsConfiguration", singletonMap("CorsRules",
                                                                                                                                                                                    singletonList(
                                                                                                                                                                                            Map.of("AllowedHeaders",
                                                                                                                                                                                                   FamilyDirectorySssStack.S3_PDF_BUCKET_CORS_ALLOWED_HEADERS,
                                                                                                                                                                                                   "AllowedMethods",
                                                                                                                                                                                                   FamilyDirectorySssStack.S3_PDF_BUCKET_CORS_ALLOWED_METHODS.stream()
                                                                                                                                                                                                                                                             .map(HttpMethods::name)
                                                                                                                                                                                                                                                             .toList(),
                                                                                                                                                                                                   "AllowedOrigins",
                                                                                                                                                                                                   FamilyDirectorySssStack.S3_PDF_BUCKET_CORS_ALLOWED_ORIGINS))),
                                                                                                                                                  "OwnershipControls", singletonMap("Rules",
                                                                                                                                                                                    singletonList(
                                                                                                                                                                                            singletonMap(
                                                                                                                                                                                                    "ObjectOwnership",
                                                                                                                                                                                                    "BucketOwnerEnforced"))),
                                                                                                                                                  "PublicAccessBlockConfiguration",
                                                                                                                                                  Map.of("BlockPublicAcls", true, "BlockPublicPolicy",
                                                                                                                                                         true, "IgnorePublicAcls", true,
                                                                                                                                                         "RestrictPublicBuckets", true)))));
        assertEquals(1, bucketMap.size());

        final String pdfBucketId = bucketMap.entrySet()
                                            .iterator()
                                            .next()
                                            .getKey();
        template.hasResourceProperties("AWS::S3::BucketPolicy", objectLike(Map.of("Bucket", singletonMap("Ref", pdfBucketId), "PolicyDocument", objectLike(singletonMap("Statement",
                                                                                                                                                                        List.of(Map.of("Action", "s3:*",
                                                                                                                                                                                       "Condition",
                                                                                                                                                                                       singletonMap(
                                                                                                                                                                                               "Bool",
                                                                                                                                                                                               singletonMap(
                                                                                                                                                                                                       "aws:SecureTransport",
                                                                                                                                                                                                       "false")),
                                                                                                                                                                                       "Effect", "Deny",
                                                                                                                                                                                       "Principal",
                                                                                                                                                                                       singletonMap(
                                                                                                                                                                                               "AWS",
                                                                                                                                                                                               FamilyDirectoryCdkApp.GLOBAL_RESOURCE),
                                                                                                                                                                                       "Resource",
                                                                                                                                                                                       List.of(singletonMap(
                                                                                                                                                                                                       "Fn::GetAtt",
                                                                                                                                                                                                       List.of(pdfBucketId,
                                                                                                                                                                                                               "Arn")),
                                                                                                                                                                                               singletonMap(
                                                                                                                                                                                                       "Fn::Join",
                                                                                                                                                                                                       List.of("",
                                                                                                                                                                                                               List.of(singletonMap(
                                                                                                                                                                                                                               "Fn::GetAtt",
                                                                                                                                                                                                                               List.of(pdfBucketId,
                                                                                                                                                                                                                                       "Arn")),
                                                                                                                                                                                                                       "/*"))))),
                                                                                                                                                                                Map.of("Action", "s3:*",
                                                                                                                                                                                       "Condition",
                                                                                                                                                                                       singletonMap(
                                                                                                                                                                                               "NumericLessThan",
                                                                                                                                                                                               singletonMap(
                                                                                                                                                                                                       "s3:TlsVersion",
                                                                                                                                                                                                       FamilyDirectorySssStack.S3_MINIMUM_TLS_VERSION)),
                                                                                                                                                                                       "Effect", "Deny",
                                                                                                                                                                                       "Principal",
                                                                                                                                                                                       singletonMap(
                                                                                                                                                                                               "AWS",
                                                                                                                                                                                               FamilyDirectoryCdkApp.GLOBAL_RESOURCE),
                                                                                                                                                                                       "Resource",
                                                                                                                                                                                       List.of(singletonMap(
                                                                                                                                                                                                       "Fn::GetAtt",
                                                                                                                                                                                                       List.of(pdfBucketId,
                                                                                                                                                                                                               "Arn")),
                                                                                                                                                                                               singletonMap(
                                                                                                                                                                                                       "Fn::Join",
                                                                                                                                                                                                       List.of("",
                                                                                                                                                                                                               List.of(singletonMap(
                                                                                                                                                                                                                               "Fn::GetAtt",
                                                                                                                                                                                                                               List.of(pdfBucketId,
                                                                                                                                                                                                                                       "Arn")),
                                                                                                                                                                                                                       "/*")))))))))));

        template.hasOutput(FamilyDirectorySssStack.S3_PDF_BUCKET_NAME_EXPORT_NAME,
                           objectLike(Map.of("Value", singletonMap("Ref", pdfBucketId), "Export", singletonMap("Name", FamilyDirectorySssStack.S3_PDF_BUCKET_NAME_EXPORT_NAME))));
        template.hasOutput(FamilyDirectorySssStack.S3_PDF_BUCKET_ARN_EXPORT_NAME,
                           objectLike(Map.of("Value", singletonMap("Fn::GetAtt", List.of(pdfBucketId, "Arn")), "Export", singletonMap("Name", FamilyDirectorySssStack.S3_PDF_BUCKET_ARN_EXPORT_NAME))));
    }
}
