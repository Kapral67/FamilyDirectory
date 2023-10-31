package org.familydirectory.cdk.lambda;

import java.util.Arrays;
import java.util.List;
import org.familydirectory.assets.lambda.function.api.enums.ApiFunction;
import org.familydirectory.assets.lambda.function.trigger.enums.TriggerFunction;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.familydirectory.cdk.lambda.construct.utility.LambdaFunctionConstructUtility;
import org.familydirectory.cdk.ses.FamilyDirectorySesStack;
import org.familydirectory.cdk.sss.FamilyDirectorySssStack;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cognito.IUserPool;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.route53.IPublicHostedZone;
import software.amazon.awscdk.services.route53.PublicHostedZone;
import software.amazon.awscdk.services.route53.PublicHostedZoneAttributes;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketAttributes;
import software.amazon.awscdk.services.s3.IBucket;
import software.amazon.awscdk.services.ses.EmailIdentity;
import software.amazon.awscdk.services.ses.IEmailIdentity;
import software.amazon.awscdk.services.ssm.IStringParameter;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;
import static software.amazon.awscdk.Fn.importValue;

public
class FamilyDirectoryLambdaStack extends Stack {

    public
    FamilyDirectoryLambdaStack (final Construct scope, final String id, final StackProps stackProps)
    {
        super(scope, id, stackProps);
        final IStringParameter hostedZoneId = StringParameter.fromStringParameterName(this, FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME,
                                                                                      FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME);
        final PublicHostedZoneAttributes hostedZoneAttrs = PublicHostedZoneAttributes.builder()
                                                                                     .hostedZoneId(hostedZoneId.getStringValue())
                                                                                     .zoneName(FamilyDirectoryDomainStack.HOSTED_ZONE_NAME)
                                                                                     .build();
        final IPublicHostedZone hostedZone = PublicHostedZone.fromPublicHostedZoneAttributes(this, FamilyDirectoryDomainStack.HOSTED_ZONE_RESOURCE_ID, hostedZoneAttrs);
        final IEmailIdentity emailIdentity = EmailIdentity.fromEmailIdentityName(this, FamilyDirectorySesStack.SES_EMAIL_IDENTITY_RESOURCE_ID,
                                                                                 importValue(FamilyDirectorySesStack.SES_EMAIL_IDENTITY_NAME_EXPORT_NAME));
        final IUserPool userPool = UserPool.fromUserPoolId(this, FamilyDirectoryCognitoStack.COGNITO_USER_POOL_RESOURCE_ID, importValue(FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME));
        final IBucket pdfBucket = Bucket.fromBucketAttributes(this, FamilyDirectorySssStack.S3_PDF_BUCKET_RESOURCE_ID, BucketAttributes.builder()
                                                                                                                                       .account(FamilyDirectoryCdkApp.DEFAULT_ACCOUNT)
                                                                                                                                       .bucketArn(importValue(
                                                                                                                                               FamilyDirectorySssStack.S3_PDF_BUCKET_ARN_EXPORT_NAME))
                                                                                                                                       .bucketName(importValue(
                                                                                                                                               FamilyDirectorySssStack.S3_PDF_BUCKET_NAME_EXPORT_NAME))
                                                                                                                                       .region(FamilyDirectoryCdkApp.DEFAULT_REGION)
                                                                                                                                       .build());

//  API Lambda Functions
        LambdaFunctionConstructUtility.constructFunctionPermissions(this,
                                                                    LambdaFunctionConstructUtility.constructFunctionMap(this, Arrays.asList(ApiFunction.values()), hostedZone, emailIdentity, userPool,
                                                                                                                        pdfBucket), emailIdentity, userPool, pdfBucket);

//  Cognito Trigger Permissions
        LambdaFunctionConstructUtility.constructFunctionPermissions(this, List.of(TriggerFunction.values()), emailIdentity, userPool, null);
    }
}
