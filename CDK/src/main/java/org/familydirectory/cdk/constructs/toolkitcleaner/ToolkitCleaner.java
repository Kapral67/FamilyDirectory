package org.familydirectory.cdk.constructs.toolkitcleaner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.EnvironmentOptions;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.nodejs.NodejsFunction;
import software.amazon.awscdk.services.lambda.nodejs.NodejsFunctionProps;
import software.amazon.awscdk.services.s3.IBucket;
import software.amazon.awscdk.services.s3.assets.Asset;
import software.amazon.awscdk.services.s3.assets.AssetProps;
import software.amazon.awscdk.services.stepfunctions.DefinitionBody;
import software.amazon.awscdk.services.stepfunctions.JsonPath;
import software.amazon.awscdk.services.stepfunctions.Map;
import software.amazon.awscdk.services.stepfunctions.MapProps;
import software.amazon.awscdk.services.stepfunctions.RetryProps;
import software.amazon.awscdk.services.stepfunctions.StateMachine;
import software.amazon.awscdk.services.stepfunctions.StateMachineProps;
import software.amazon.awscdk.services.stepfunctions.StateMachineType;
import software.amazon.awscdk.services.stepfunctions.tasks.EvaluateExpression;
import software.amazon.awscdk.services.stepfunctions.tasks.EvaluateExpressionProps;
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvoke;
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvokeProps;
import software.constructs.Construct;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

public final
class ToolkitCleaner extends Construct {
    private static final Path BASE_PATH = Paths.get(System.getProperty("user.dir"))
                                               .resolve(Paths.get("assets", "toolkit-cleaner"))
                                               .toAbsolutePath()
                                               .normalize();
    private static final String HANDLER = "index.handler";
    private static final Duration THIRTY_SECONDS = Duration.seconds(30);
    private static final Duration FIVE_MINUTES = Duration.minutes(5);
    private static final boolean PAYLOAD_RESPONSE_ONLY = true;
    private static final String DRY_RUN_ENVIRONMENT_VARIABLE_KEY = "RUN";
    private static final String DRY_RUN_ENVIRONMENT_VARIABLE_VALUE = "true";
    private static final String FILE_ASSET_RESOURCE_ID = "FileAsset";
    private static final String GET_STACK_NAMES_LAMBDA_RESOURCE_ID = "GetStackNamesFunction";
    private static final String EXTRACT_TEMPLATE_HASHES_LAMBDA_RESOURCE_ID = "ExtractTemplateHashesFunction";
    private static final String CLEAN_OBJECTS_LAMBDA_RESOURCE_ID = "CleanObjectsFunction";
    private static final String GET_STACK_NAMES_TASK_RESOURCE_ID = "GetStackNames";
    private static final String EXTRACT_TEMPLATE_HASHES_TASK_RESOURCE_ID = "ExtractTemplateHashes";
    private static final String CLEAN_OBJECTS_TASK_RESOURCE_ID = "CleanObjects";
    private static final String STACKS_MAP_RESOURCE_ID = "StacksMap";
    private static final String FLATTEN_HASHES_RESOURCE_ID = "FlattenHashes";
    private static final String STATE_MACHINE_RESOURCE_ID = "StateMachine";
    private static final java.util.Map<String, String> STACKS_MAP_RESULT_SELECTOR = singletonMap("AssetHashes", JsonPath.stringAt("$"));
    private static final Number STACKS_MAP_MAX_CONCURRENCY = 1;
    private static final List<String> EXTRACT_TEMPLATE_HASHES_RETRY_ERRORS = singletonList("Throttling");
    private static final String FLATTEN_HASHES_EXPRESSION = "[...new Set(($.AssetHashes).flat())]";
    private static final String FILE_ASSET_BUCKET_NAME_KEY = "BUCKET_NAME";
    private static final String FILE_ASSET_PATH = BASE_PATH.resolve(Paths.get("dummy.txt"))
                                                           .toString();
    private static final String GET_STACK_NAMES_LAMBDA_ASSET_PATH = BASE_PATH.resolve(Paths.get("get-stack-names.lambda.ts"))
                                                                             .toString();
    private static final String EXTRACT_TEMPLATE_HASHES_LAMBDA_ASSET_PATH = BASE_PATH.resolve(Paths.get("extract-template-hashes.lambda.ts"))
                                                                                     .toString();
    private static final String CLEAN_OBJECTS_LAMBDA_ASSET_PATH = BASE_PATH.resolve(Paths.get("clean-objects.lambda.ts"))
                                                                           .toString();
    private static final List<PolicyStatement> GET_STACK_NAMES_LAMBDA_POLICIES = singletonList(PolicyStatement.Builder.create()
                                                                                                                      .effect(Effect.ALLOW)
                                                                                                                      .actions(List.of("cloudformation:DescribeStacks", "cloudformation:ListStacks"))
                                                                                                                      .resources(singletonList("*"))
                                                                                                                      .build());
    private static final List<PolicyStatement> EXTRACT_TEMPLATE_HASHES_LAMBDA_POLICIES = singletonList(PolicyStatement.Builder.create()
                                                                                                                              .effect(Effect.ALLOW)
                                                                                                                              .actions(singletonList("cloudformation:GetTemplate"))
                                                                                                                              .resources(singletonList("*"))
                                                                                                                              .build());

    public
    ToolkitCleaner (final @NotNull Construct scope, final @NotNull String id) {
        super(scope, id);

        final Asset fileAsset = new Asset(this, FILE_ASSET_RESOURCE_ID, AssetProps.builder()
                                                                                  .path(FILE_ASSET_PATH)
                                                                                  .build());

        final NodejsFunction getStackNamesFunction = new NodejsFunction(this, GET_STACK_NAMES_LAMBDA_RESOURCE_ID, NodejsFunctionProps.builder()
                                                                                                                                     .runtime(Runtime.NODEJS_20_X)
                                                                                                                                     .handler(HANDLER)
                                                                                                                                     .entry(GET_STACK_NAMES_LAMBDA_ASSET_PATH)
                                                                                                                                     .timeout(THIRTY_SECONDS)
                                                                                                                                     .initialPolicy(GET_STACK_NAMES_LAMBDA_POLICIES)
                                                                                                                                     .build());
        addDefaultLambdaEnvironment(getStackNamesFunction);
        final LambdaInvoke getStackNames = new LambdaInvoke(this, GET_STACK_NAMES_TASK_RESOURCE_ID, LambdaInvokeProps.builder()
                                                                                                                     .lambdaFunction(getStackNamesFunction)
                                                                                                                     .payloadResponseOnly(PAYLOAD_RESPONSE_ONLY)
                                                                                                                     .build());

        final Map stacksMap = new Map(this, STACKS_MAP_RESOURCE_ID, MapProps.builder()
                                                                            .maxConcurrency(STACKS_MAP_MAX_CONCURRENCY)
                                                                            .resultSelector(STACKS_MAP_RESULT_SELECTOR)
                                                                            .build());

        final NodejsFunction extractTemplateHashesFunction = new NodejsFunction(this, EXTRACT_TEMPLATE_HASHES_LAMBDA_RESOURCE_ID, NodejsFunctionProps.builder()
                                                                                                                                                     .runtime(Runtime.NODEJS_20_X)
                                                                                                                                                     .handler(HANDLER)
                                                                                                                                                     .entry(EXTRACT_TEMPLATE_HASHES_LAMBDA_ASSET_PATH)
                                                                                                                                                     .timeout(THIRTY_SECONDS)
                                                                                                                                                     .initialPolicy(
                                                                                                                                                             EXTRACT_TEMPLATE_HASHES_LAMBDA_POLICIES)
                                                                                                                                                     .build());
        addDefaultLambdaEnvironment(extractTemplateHashesFunction);
        final LambdaInvoke extractTemplateHashes = new LambdaInvoke(this, EXTRACT_TEMPLATE_HASHES_TASK_RESOURCE_ID, LambdaInvokeProps.builder()
                                                                                                                                     .lambdaFunction(extractTemplateHashesFunction)
                                                                                                                                     .payloadResponseOnly(PAYLOAD_RESPONSE_ONLY)
                                                                                                                                     .build());
        extractTemplateHashes.addRetry(RetryProps.builder()
                                                 .errors(EXTRACT_TEMPLATE_HASHES_RETRY_ERRORS)
                                                 .build());

        final EvaluateExpression flattenHashes = new EvaluateExpression(this, FLATTEN_HASHES_RESOURCE_ID, EvaluateExpressionProps.builder()
                                                                                                                                 .expression(FLATTEN_HASHES_EXPRESSION)
                                                                                                                                 .build());

        final IBucket fileAssetBucket = fileAsset.getBucket();
        final NodejsFunction cleanObjectsFunction = new NodejsFunction(this, CLEAN_OBJECTS_LAMBDA_RESOURCE_ID, NodejsFunctionProps.builder()
                                                                                                                                  .runtime(Runtime.NODEJS_20_X)
                                                                                                                                  .handler(HANDLER)
                                                                                                                                  .entry(CLEAN_OBJECTS_LAMBDA_ASSET_PATH)
                                                                                                                                  .timeout(FIVE_MINUTES)
                                                                                                                                  .environment(java.util.Map.of(FILE_ASSET_BUCKET_NAME_KEY,
                                                                                                                                                                fileAssetBucket.getBucketName(),
                                                                                                                                                                DRY_RUN_ENVIRONMENT_VARIABLE_KEY,
                                                                                                                                                                DRY_RUN_ENVIRONMENT_VARIABLE_VALUE))
                                                                                                                                  .build());
        addDefaultLambdaEnvironment(cleanObjectsFunction);
        fileAssetBucket.grantRead(cleanObjectsFunction);
        fileAssetBucket.grantDelete(cleanObjectsFunction);
        final LambdaInvoke cleanObjects = new LambdaInvoke(this, CLEAN_OBJECTS_TASK_RESOURCE_ID, LambdaInvokeProps.builder()
                                                                                                                  .lambdaFunction(cleanObjectsFunction)
                                                                                                                  .payloadResponseOnly(PAYLOAD_RESPONSE_ONLY)
                                                                                                                  .build());

        new StateMachine(this, STATE_MACHINE_RESOURCE_ID, StateMachineProps.builder()
                                                                           .definitionBody(DefinitionBody.fromChainable(getStackNames.next(stacksMap.iterator(extractTemplateHashes))
                                                                                                                                     .next(flattenHashes)
                                                                                                                                     .next(cleanObjects)))
                                                                           .stateMachineType(StateMachineType.EXPRESS)
                                                                           .build());
    }

    private static
    void addDefaultLambdaEnvironment (final @NotNull Function func) {
        func.addEnvironment("AWS_NODEJS_CONNECTION_REUSE_ENABLED", "1", EnvironmentOptions.builder()
                                                                                          .removeInEdge(true)
                                                                                          .build());
    }
}
