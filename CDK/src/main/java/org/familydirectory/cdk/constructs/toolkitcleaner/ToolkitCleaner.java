package org.familydirectory.cdk.constructs.toolkitcleaner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecr.assets.DockerImageAssetProps;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.RuleProps;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.SfnStateMachine;
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
import software.amazon.awscdk.services.stepfunctions.Parallel;
import software.amazon.awscdk.services.stepfunctions.RetryProps;
import software.amazon.awscdk.services.stepfunctions.StateMachine;
import software.amazon.awscdk.services.stepfunctions.StateMachineProps;
import software.amazon.awscdk.services.stepfunctions.tasks.EvaluateExpression;
import software.amazon.awscdk.services.stepfunctions.tasks.EvaluateExpressionProps;
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvoke;
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvokeProps;
import software.constructs.Construct;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.nonNull;

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
    private static final String RETAIN_ASSETS_NEWER_THAN_KEY = "RETAIN_MILLISECONDS";
    private static final String FILE_ASSET_RESOURCE_ID = "FileAsset";
    private static final String DOCKER_IMAGE_ASSET_RESOURCE_ID = "DockerImageAsset";
    private static final String GET_STACK_NAMES_LAMBDA_RESOURCE_ID = "GetStackNamesFunction";
    private static final String EXTRACT_TEMPLATE_HASHES_LAMBDA_RESOURCE_ID = "ExtractTemplateHashesFunction";
    private static final String CLEAN_OBJECTS_LAMBDA_RESOURCE_ID = "CleanObjectsFunction";
    private static final String CLEAN_IMAGES_LAMBDA_RESOURCE_ID = "CleanImagesFunction";
    private static final String GET_STACK_NAMES_TASK_RESOURCE_ID = "GetStackNames";
    private static final String EXTRACT_TEMPLATE_HASHES_TASK_RESOURCE_ID = "ExtractTemplateHashes";
    private static final String CLEAN_OBJECTS_TASK_RESOURCE_ID = "CleanObjects";
    private static final String CLEAN_IMAGES_TASK_RESOURCE_ID = "CleanImages";
    private static final String STACKS_MAP_RESOURCE_ID = "StacksMap";
    private static final String FLATTEN_HASHES_RESOURCE_ID = "FlattenHashes";
    private static final String SUM_RECLAIMED_RESOURCE_ID = "EvaluateExpression";
    private static final String STATE_MACHINE_RESOURCE_ID = "StateMachine";
    private static final String PARALLEL_CLEAN_RESOURCE_ID = "Clean";
    private static final String STATE_MACHINE_RULE_RESOURCE_ID = "Rule";
    private static final java.util.Map<String, String> STACKS_MAP_RESULT_SELECTOR = singletonMap("AssetHashes", JsonPath.stringAt("$"));
    private static final Number STACKS_MAP_MAX_CONCURRENCY = 1;
    private static final List<String> EXTRACT_TEMPLATE_HASHES_RETRY_ERRORS = singletonList("Throttling");
    private static final String FLATTEN_HASHES_EXPRESSION = "[...new Set(($.AssetHashes).flat())]";
    private static final String FILE_ASSET_BUCKET_NAME_KEY = "BUCKET_NAME";
    private static final String DOCKER_IMAGE_ASSET_REPOSITORY_NAME_KEY = "REPOSITORY_NAME";
    private static final String SUM_RECLAIMED_EXPRESSION = "({ Deleted: $[0].Deleted + $[1].Deleted, Reclaimed: $[0].Reclaimed + $[1].Reclaimed })";
    private static final String FILE_ASSET_PATH = BASE_PATH.resolve(Paths.get("docker", "dummy.txt"))
                                                           .toString();
    private static final String DOCKER_IMAGE_ASSET_PATH = BASE_PATH.resolve(Paths.get("docker"))
                                                                   .toString();
    private static final String GET_STACK_NAMES_LAMBDA_ASSET_PATH = BASE_PATH.resolve(Paths.get("get-stack-names.lambda.ts"))
                                                                             .toString();
    private static final String EXTRACT_TEMPLATE_HASHES_LAMBDA_ASSET_PATH = BASE_PATH.resolve(Paths.get("extract-template-hashes.lambda.ts"))
                                                                                     .toString();
    private static final String CLEAN_OBJECTS_LAMBDA_ASSET_PATH = BASE_PATH.resolve(Paths.get("clean-objects.lambda.ts"))
                                                                           .toString();
    private static final String CLEAN_IMAGES_LAMBDA_ASSET_PATH = BASE_PATH.resolve(Paths.get("clean-images.lambda.ts"))
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
    private static final List<String> CLEAN_IMAGES_LAMBDA_PERMISSIONS = List.of("ecr:DescribeImages", "ecr:BatchDeleteImage");

    public
    ToolkitCleaner (final @NotNull Construct scope, final @NotNull String id, final @NotNull ToolkitCleanerProps props) {
        super(scope, id);

        final Asset fileAsset = new Asset(this, FILE_ASSET_RESOURCE_ID, AssetProps.builder()
                                                                                  .path(FILE_ASSET_PATH)
                                                                                  .build());

        final DockerImageAsset dockerImageAsset = new DockerImageAsset(this, DOCKER_IMAGE_ASSET_RESOURCE_ID, DockerImageAssetProps.builder()
                                                                                                                                  .directory(DOCKER_IMAGE_ASSET_PATH)
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
                                                                                                                                  .environment(singletonMap(FILE_ASSET_BUCKET_NAME_KEY,
                                                                                                                                                            fileAssetBucket.getBucketName()))
                                                                                                                                  .build());
        addDefaultLambdaEnvironment(cleanObjectsFunction);
        fileAssetBucket.grantRead(cleanObjectsFunction);
        fileAssetBucket.grantDelete(cleanObjectsFunction);
        final LambdaInvoke cleanObjects = new LambdaInvoke(this, CLEAN_OBJECTS_TASK_RESOURCE_ID, LambdaInvokeProps.builder()
                                                                                                                  .lambdaFunction(cleanObjectsFunction)
                                                                                                                  .payloadResponseOnly(PAYLOAD_RESPONSE_ONLY)
                                                                                                                  .build());

        final IRepository dockerImageAssetRepository = dockerImageAsset.getRepository();
        final NodejsFunction cleanImagesFunction = new NodejsFunction(this, CLEAN_IMAGES_LAMBDA_RESOURCE_ID, NodejsFunctionProps.builder()
                                                                                                                                .runtime(Runtime.NODEJS_20_X)
                                                                                                                                .handler(HANDLER)
                                                                                                                                .entry(CLEAN_IMAGES_LAMBDA_ASSET_PATH)
                                                                                                                                .timeout(FIVE_MINUTES)
                                                                                                                                .environment(singletonMap(DOCKER_IMAGE_ASSET_REPOSITORY_NAME_KEY,
                                                                                                                                                          dockerImageAssetRepository.getRepositoryName()))
                                                                                                                                .build());
        addDefaultLambdaEnvironment(cleanImagesFunction);
        dockerImageAssetRepository.grant(cleanImagesFunction, CLEAN_IMAGES_LAMBDA_PERMISSIONS.toArray(new String[0]));
        final LambdaInvoke cleanImages = new LambdaInvoke(this, CLEAN_IMAGES_TASK_RESOURCE_ID, LambdaInvokeProps.builder()
                                                                                                                .lambdaFunction(cleanImagesFunction)
                                                                                                                .payloadResponseOnly(PAYLOAD_RESPONSE_ONLY)
                                                                                                                .build());

        if (!props.getDryRun()) {
            cleanObjectsFunction.addEnvironment(DRY_RUN_ENVIRONMENT_VARIABLE_KEY, DRY_RUN_ENVIRONMENT_VARIABLE_VALUE);
            cleanImagesFunction.addEnvironment(DRY_RUN_ENVIRONMENT_VARIABLE_KEY, DRY_RUN_ENVIRONMENT_VARIABLE_VALUE);
        }

        if (nonNull(props.getRetainAssetsNewerThan())) {
            final String millis = String.valueOf(props.getRetainAssetsNewerThan()
                                                      .toMilliseconds()
                                                      .longValue());
            cleanObjectsFunction.addEnvironment(RETAIN_ASSETS_NEWER_THAN_KEY, millis);
            cleanImagesFunction.addEnvironment(RETAIN_ASSETS_NEWER_THAN_KEY, millis);
        }

        final EvaluateExpression sumReclaimed = new EvaluateExpression(this, SUM_RECLAIMED_RESOURCE_ID, EvaluateExpressionProps.builder()
                                                                                                                               .expression(SUM_RECLAIMED_EXPRESSION)
                                                                                                                               .build());

        final StateMachine stateMachine = new StateMachine(this, STATE_MACHINE_RESOURCE_ID, StateMachineProps.builder()
                                                                                                             .definitionBody(DefinitionBody.fromChainable(
                                                                                                                     getStackNames.next(stacksMap.iterator(extractTemplateHashes))
                                                                                                                                  .next(flattenHashes)
                                                                                                                                  .next(Parallel.Builder.create(this, PARALLEL_CLEAN_RESOURCE_ID)
                                                                                                                                                        .build()
                                                                                                                                                        .branch(cleanObjects)
                                                                                                                                                        .branch(cleanImages))
                                                                                                                                  .next(sumReclaimed)))
                                                                                                             .build());

        final Schedule schedule = props.getSchedule();
        final Rule rule = new Rule(this, STATE_MACHINE_RULE_RESOURCE_ID, RuleProps.builder()
                                                                                  .enabled(nonNull(schedule))
                                                                                  .schedule(nonNull(schedule)
                                                                                                    ? schedule
                                                                                                    : ToolkitCleanerProps.DEFAULT_SCHEDULE)
                                                                                  .build());
        rule.addTarget(new SfnStateMachine(stateMachine));
    }

    private static
    void addDefaultLambdaEnvironment (final @NotNull Function func) {
        func.addEnvironment("AWS_NODEJS_CONNECTION_REUSE_ENABLED", "1", EnvironmentOptions.builder()
                                                                                          .removeInEdge(true)
                                                                                          .build());
    }
}
