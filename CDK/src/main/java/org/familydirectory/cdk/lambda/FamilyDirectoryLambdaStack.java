package org.familydirectory.cdk.lambda;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.lambda.function.api.enums.ApiFunction;
import org.familydirectory.assets.lambda.function.models.LambdaFunctionModel;
import org.familydirectory.assets.lambda.function.stream.enums.StreamFunction;
import org.familydirectory.assets.lambda.function.trigger.enums.TriggerFunction;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.familydirectory.cdk.lambda.construct.utility.LambdaFunctionConstructUtility;
import org.familydirectory.cdk.ses.FamilyDirectorySesStack;
import org.familydirectory.cdk.sss.FamilyDirectorySssStack;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cognito.IUserPool;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableAttributes;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSource;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSourceProps;
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
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static software.amazon.awscdk.Duration.days;
import static software.amazon.awscdk.Duration.seconds;
import static software.amazon.awscdk.Fn.importValue;
import static software.amazon.awscdk.services.iam.Effect.ALLOW;
import static software.amazon.awscdk.services.lambda.StartingPosition.LATEST;

public
class FamilyDirectoryLambdaStack extends Stack {
    public static final Number DDB_STREAM_BATCH_SIZE = 1;
    public static final boolean DDB_STREAM_BISECT_BATCH_ON_ERROR = false;
    public static final boolean DDB_STREAM_REPORT_BATCH_ITEM_FAILURES = false;
    public static final Duration DDB_STREAM_MAX_BATCH_WINDOW = seconds(60);
    public static final Duration DDB_STREAM_MAX_RECORD_AGE = days(7);
    public static final Number DDB_STREAM_PARALLELIZATION_FACTOR = 1;
    public static final Number DDB_STREAM_RETRY_ATTEMPTS = 0;
    public static final boolean DDB_STREAM_ENABLED = false;

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

//  Stream Functions
        final Map<LambdaFunctionModel, Function> streamFunctionMap = LambdaFunctionConstructUtility.constructFunctionMap(this, Arrays.asList(StreamFunction.values()), null, null, null, pdfBucket);

        for (final Map.Entry<LambdaFunctionModel, Function> entry : streamFunctionMap.entrySet()) {
            final StreamFunction streamFunction = (StreamFunction) entry.getKey();
            for (final DdbTable eventTable : streamFunction.streamEventSources()) {
                final String streamArn = importValue(requireNonNull(eventTable.streamArnExportName()));
                final Function lambda = entry.getValue();
                lambda.addToRolePolicy(PolicyStatement.Builder.create()
                                                              .effect(ALLOW)
                                                              .actions(List.of("dynamodb:DescribeStream", "dynamodb:GetRecords", "dynamodb:GetShardIterator"))
                                                              .resources(singletonList(streamArn))
                                                              .build());
                lambda.addEventSource(new DynamoEventSource(Table.fromTableAttributes(this, eventTable.name(), TableAttributes.builder()
                                                                                                                              .tableArn(importValue(eventTable.arnExportName()))
                                                                                                                              .tableStreamArn(streamArn)
                                                                                                                              .build()), DynamoEventSourceProps.builder()
                                                                                                                                                               .batchSize(DDB_STREAM_BATCH_SIZE)
                                                                                                                                                               .bisectBatchOnError(
                                                                                                                                                                       DDB_STREAM_BISECT_BATCH_ON_ERROR)
                                                                                                                                                               .reportBatchItemFailures(
                                                                                                                                                                       DDB_STREAM_REPORT_BATCH_ITEM_FAILURES)
                                                                                                                                                               .maxBatchingWindow(
                                                                                                                                                                       DDB_STREAM_MAX_BATCH_WINDOW)
                                                                                                                                                               .maxRecordAge(DDB_STREAM_MAX_RECORD_AGE)
                                                                                                                                                               .parallelizationFactor(
                                                                                                                                                                       DDB_STREAM_PARALLELIZATION_FACTOR)
                                                                                                                                                               .retryAttempts(DDB_STREAM_RETRY_ATTEMPTS)
                                                                                                                                                               .startingPosition(LATEST)
                                                                                                                                                               .enabled(DDB_STREAM_ENABLED)
                                                                                                                                                               .build()));
            }
        }

        LambdaFunctionConstructUtility.constructFunctionPermissions(this, streamFunctionMap, null, null, pdfBucket);
    }
}
