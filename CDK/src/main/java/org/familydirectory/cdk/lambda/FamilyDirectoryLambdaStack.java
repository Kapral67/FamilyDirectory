package org.familydirectory.cdk.lambda;

import java.util.List;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.assets.lambda.function.api.enums.ApiFunction;
import org.familydirectory.assets.lambda.function.stream.enums.StreamFunction;
import org.familydirectory.assets.lambda.function.trigger.enums.TriggerFunction;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.amplify.FamilyDirectoryAmplifyStack;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.familydirectory.cdk.lambda.construct.utility.LambdaFunctionConstructUtility;
import org.familydirectory.cdk.sss.FamilyDirectorySssStack;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.amplify.alpha.App;
import software.amazon.awscdk.services.amplify.alpha.IApp;
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
import software.amazon.awscdk.services.ssm.IStringParameter;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.amazon.awscdk.triggers.InvocationType;
import software.amazon.awscdk.triggers.Trigger;
import software.amazon.awscdk.triggers.TriggerProps;
import software.constructs.Construct;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static software.amazon.awscdk.Duration.seconds;
import static software.amazon.awscdk.Fn.importValue;
import static software.amazon.awscdk.services.iam.Effect.ALLOW;
import static software.amazon.awscdk.services.lambda.StartingPosition.LATEST;

public
class FamilyDirectoryLambdaStack extends Stack {
    public static final Number DDB_STREAM_BATCH_SIZE = 1;
    public static final boolean DDB_STREAM_BISECT_BATCH_ON_ERROR = false;
    public static final boolean DDB_STREAM_REPORT_BATCH_ITEM_FAILURES = false;
    public static final Number DDB_STREAM_PARALLELIZATION_FACTOR = 1;
    public static final Number DDB_STREAM_RETRY_ATTEMPTS = 0;
    public static final boolean DDB_STREAM_ENABLED = true;
    public static final List<String> DDB_STREAM_POLICY_ACTIONS = List.of("dynamodb:DescribeStream", "dynamodb:GetRecords", "dynamodb:GetShardIterator");
    public static final String PDF_GENERATOR_TRIGGER_RESOURCE_ID = "PdfGeneratorTrigger";

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
        final IUserPool userPool = UserPool.fromUserPoolId(this, FamilyDirectoryCognitoStack.COGNITO_USER_POOL_RESOURCE_ID, importValue(FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME));
        final IBucket pdfBucket = Bucket.fromBucketAttributes(this, FamilyDirectorySssStack.S3_PDF_BUCKET_RESOURCE_ID, BucketAttributes.builder()
                                                                                                                                       .account(FamilyDirectoryCdkApp.DEFAULT_ACCOUNT)
                                                                                                                                       .bucketArn(importValue(
                                                                                                                                               FamilyDirectorySssStack.S3_PDF_BUCKET_ARN_EXPORT_NAME))
                                                                                                                                       .bucketName(importValue(
                                                                                                                                               FamilyDirectorySssStack.S3_PDF_BUCKET_NAME_EXPORT_NAME))
                                                                                                                                       .region(FamilyDirectoryCdkApp.DEFAULT_REGION)
                                                                                                                                       .build());
        final IApp spaApp = App.fromAppId(this, FamilyDirectoryAmplifyStack.AMPLIFY_APP_RESOURCE_ID, importValue(FamilyDirectoryAmplifyStack.AMPLIFY_APP_ID_EXPORT_NAME));

//  API Lambda Functions
        LambdaFunctionConstructUtility.constructFunctionPermissions(LambdaFunctionConstructUtility.constructFunctionMap(this, List.of(ApiFunction.values()), hostedZone, userPool, pdfBucket, spaApp),
                                                                    userPool, pdfBucket);

//  Cognito Trigger Permissions
        LambdaFunctionConstructUtility.constructFunctionPermissions(this, List.of(TriggerFunction.values()), userPool, null);

//  Stream Functions
        final Map<StreamFunction, Function> streamFunctionMap = LambdaFunctionConstructUtility.constructFunctionMap(this, List.of(StreamFunction.values()), null, null, pdfBucket, null);

        for (final Map.Entry<StreamFunction, Function> entry : streamFunctionMap.entrySet()) {
            final StreamFunction streamFunction = entry.getKey();
            for (final DdbTable eventTable : streamFunction.streamEventSources()) {
                final String streamArn = importValue(requireNonNull(eventTable.streamArnExportName()));
                final Function lambda = entry.getValue();
                lambda.addToRolePolicy(PolicyStatement.Builder.create()
                                                              .effect(ALLOW)
                                                              .actions(DDB_STREAM_POLICY_ACTIONS)
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
                                                                                                                                                               .maxRecordAge(
                                                                                                                                                                       seconds(DdbUtils.DDB_STREAM_MAX_RECORD_AGE_SECONDS))
                                                                                                                                                               .parallelizationFactor(
                                                                                                                                                                       DDB_STREAM_PARALLELIZATION_FACTOR)
                                                                                                                                                               .retryAttempts(DDB_STREAM_RETRY_ATTEMPTS)
                                                                                                                                                               .startingPosition(LATEST)
                                                                                                                                                               .enabled(DDB_STREAM_ENABLED)
                                                                                                                                                               .build()));
            }
        }

        LambdaFunctionConstructUtility.constructFunctionPermissions(streamFunctionMap, null, pdfBucket);

        new Trigger(this, PDF_GENERATOR_TRIGGER_RESOURCE_ID, TriggerProps.builder()
                                                                         .handler(streamFunctionMap.get(StreamFunction.PDF_GENERATOR))
                                                                         .invocationType(InvocationType.EVENT)
                                                                         .timeout(streamFunctionMap.get(StreamFunction.PDF_GENERATOR)
                                                                                                   .getTimeout())
                                                                         .build());
    }
}
