package org.familydirectory.cdk.lambda.construct.utility;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.assets.lambda.function.models.LambdaFunctionModel;
import org.familydirectory.assets.lambda.function.stream.enums.StreamFunction;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.amplify.FamilyDirectoryAmplifyStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.services.amplify.alpha.IApp;
import software.amazon.awscdk.services.cognito.IUserPool;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.EventInvokeConfigOptions;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.s3.IBucket;
import software.constructs.Construct;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.familydirectory.assets.Constants.VERSION;
import static software.amazon.awscdk.Duration.seconds;
import static software.amazon.awscdk.Fn.importValue;
import static software.amazon.awscdk.services.iam.Effect.ALLOW;
import static software.amazon.awscdk.services.iam.PolicyStatement.Builder.create;
import static software.amazon.awscdk.services.lambda.Architecture.ARM_64;
import static software.amazon.awscdk.services.lambda.Runtime.JAVA_21;

public final
class LambdaFunctionConstructUtility {
    public static final Runtime RUNTIME = JAVA_21;
    public static final Architecture ARCHITECTURE = ARM_64;

    private
    LambdaFunctionConstructUtility () {
        super();
    }

    public static @NotNull
    <T extends LambdaFunctionModel> Map<T, Function> constructFunctionMap (final @NotNull Construct scope, final @NotNull List<T> values, final @Nullable IHostedZone hostedZone,
                                                                           final @Nullable IUserPool userPool, final @Nullable IBucket pdfBucket, final @Nullable IApp spaApp)
    {
        return values.stream()
                     .collect(Collectors.toUnmodifiableMap(f -> f, f -> {
                         final Function function = new Function(scope, f.functionName(), FunctionProps.builder()
                                                                                                      .runtime(RUNTIME)
                                                                                                      .code(getCodeAsset(f.functionName()))
                                                                                                      .handler(f.handler())
                                                                                                      .timeout(seconds(f.timeoutSeconds()))
                                                                                                      .architecture(ARCHITECTURE)
                                                                                                      .memorySize(f.memorySize())
                                                                                                      .reservedConcurrentExecutions(f instanceof StreamFunction ? 1 : null)
                                                                                                      .build());
                         function.configureAsyncInvoke(EventInvokeConfigOptions.builder()
                                                                               .retryAttempts(0)
                                                                               .build());

                         Arrays.stream(LambdaUtils.EnvVar.values())
                               .forEach(env -> {
                                   switch (env) {
                                       case COGNITO_USER_POOL_ID -> ofNullable(userPool).map(IUserPool::getUserPoolId)
                                                                                        .ifPresent(id -> function.addEnvironment(env.name(), id));
                                       case ROOT_ID -> function.addEnvironment(env.name(), DdbUtils.ROOT_MEMBER_ID);
                                       case HOSTED_ZONE_NAME -> ofNullable(hostedZone).map(IHostedZone::getZoneName)
                                                                                      .ifPresent(hostedZoneName -> function.addEnvironment(env.name(), hostedZoneName));
                                       case S3_PDF_BUCKET_NAME -> ofNullable(pdfBucket).map(IBucket::getBucketName)
                                                                                       .ifPresent(name -> function.addEnvironment(env.name(), name));
                                       case AMPLIFY_APP_ID -> ofNullable(spaApp).map(IApp::getAppId)
                                                                                .ifPresent(id -> function.addEnvironment(env.name(), id));
                                       case AMPLIFY_BRANCH_NAME -> function.addEnvironment(env.name(), FamilyDirectoryAmplifyStack.AMPLIFY_ROOT_BRANCH_NAME);
                                       default -> {
                                       }
                                   }
                               });

                         new CfnOutput(scope, f.arnExportName(), CfnOutputProps.builder()
                                                                               .value(function.getFunctionArn())
                                                                               .exportName(f.arnExportName())
                                                                               .build());
                         new CfnOutput(scope, f.roleArnExportName(), CfnOutputProps.builder()
                                                                                   .value(requireNonNull(function.getRole()).getRoleArn())
                                                                                   .exportName(f.roleArnExportName())
                                                                                   .build());

                         return function;
                     }));
    }

    @NotNull
    private static
    Code getCodeAsset (final String lambdaName) {
        final Path assetPath = Path.of(System.getProperty("user.dir"), "..", "assets", lambdaName, "build", "distributions", "%s-%s.zip".formatted(lambdaName, VERSION.toString()));
        return Code.fromAsset(assetPath.toAbsolutePath()
                                       .normalize()
                                       .toString());
    }

    public static
    void constructFunctionPermissions (final @NotNull Map<? extends LambdaFunctionModel, Function> values, final @Nullable IUserPool userPool, final @Nullable IBucket pdfBucket)
    {
        values.forEach((k, v) -> {
//      Assign Ddb Permissions
            ofNullable(k.ddbActions()).ifPresent(map -> map.forEach((table, actions) -> {
                final String tableArn = importValue(table.arnExportName());
                v.addToRolePolicy(create().effect(ALLOW)
                                          .actions(actions)
                                          .resources(List.of(tableArn, "%s/index/%s".formatted(tableArn, FamilyDirectoryCdkApp.GLOBAL_RESOURCE)))
                                          .build());
            }));
//      Assign Cognito Permissions
            ofNullable(userPool).map(IUserPool::getUserPoolArn)
                                .ifPresent(userPoolArn -> ofNullable(k.cognitoActions()).ifPresent(actions -> v.addToRolePolicy(create().effect(ALLOW)
                                                                                                                                        .actions(actions)
                                                                                                                                        .resources(singletonList(userPoolArn))
                                                                                                                                        .build())));

//      Assign Ses Permissions
            ofNullable(k.sesActions()).ifPresent(actions -> v.addToRolePolicy(create().effect(ALLOW)
                                                                                      .actions(actions)
                                                                                      .resources(singletonList(FamilyDirectoryCdkApp.GLOBAL_RESOURCE))
                                                                                      .build()));

//      Assign S3 Permissions
            ofNullable(pdfBucket).map(IBucket::getBucketArn)
                                 .ifPresent(pdfBucketArn -> ofNullable(k.sssActions()).ifPresent(actions -> v.addToRolePolicy(create().effect(ALLOW)
                                                                                                                                      .actions(actions)
                                                                                                                                      .resources(singletonList("%s/%s".formatted(pdfBucketArn,
                                                                                                                                                                                 FamilyDirectoryCdkApp.GLOBAL_RESOURCE)))
                                                                                                                                      .build())));

//      Assign Amplify Permissions
            ofNullable(k.amplifyActions()).ifPresent(actions -> v.addToRolePolicy(create().effect(ALLOW)
                                                                                          .actions(actions)
                                                                                          .resources(singletonList(FamilyDirectoryCdkApp.GLOBAL_RESOURCE))
                                                                                          .build()));
        });
    }

    public static
    void constructFunctionPermissions (final @NotNull Construct scope, final @NotNull List<? extends LambdaFunctionModel> values, final @Nullable IUserPool userPool, final @Nullable IBucket pdfBucket)
    {
        values.forEach(f -> {
            final IRole executionRole = Role.fromRoleArn(scope, f.roleArnExportName(), importValue(f.roleArnExportName()));

//      Assign Ddb Permissions
            ofNullable(f.ddbActions()).ifPresent(map -> map.forEach((table, actions) -> {
                final String tableArn = importValue(table.arnExportName());
                executionRole.addToPrincipalPolicy(create().effect(ALLOW)
                                                           .actions(actions)
                                                           .resources(List.of(tableArn, "%s/index/%s".formatted(tableArn, FamilyDirectoryCdkApp.GLOBAL_RESOURCE)))
                                                           .build());
            }));

//      Assign Cognito Permissions
            ofNullable(userPool).map(IUserPool::getUserPoolArn)
                                .ifPresent(userPoolArn -> ofNullable(f.cognitoActions()).ifPresent(actions -> executionRole.addToPrincipalPolicy(create().effect(ALLOW)
                                                                                                                                                         .actions(actions)
                                                                                                                                                         .resources(singletonList(userPoolArn))
                                                                                                                                                         .build())));
//      Assign Ses Permissions
            ofNullable(f.sesActions()).ifPresent(actions -> executionRole.addToPrincipalPolicy(create().effect(ALLOW)
                                                                                                       .actions(actions)
                                                                                                       .resources(singletonList(FamilyDirectoryCdkApp.GLOBAL_RESOURCE))
                                                                                                       .build()));

//      Assign S3 Permissions
            ofNullable(pdfBucket).map(IBucket::getBucketArn)
                                 .ifPresent(pdfBucketArn -> ofNullable(f.sssActions()).ifPresent(actions -> executionRole.addToPrincipalPolicy(create().effect(ALLOW)
                                                                                                                                                       .actions(actions)
                                                                                                                                                       .resources(singletonList(
                                                                                                                                                               "%s/%s".formatted(pdfBucketArn,
                                                                                                                                                                                 FamilyDirectoryCdkApp.GLOBAL_RESOURCE)))
                                                                                                                                                       .build())));

//      Assign Amplify Permissions
            ofNullable(f.amplifyActions()).ifPresent(actions -> executionRole.addToPrincipalPolicy(create().effect(ALLOW)
                                                                                                           .actions(actions)
                                                                                                           .resources(singletonList(FamilyDirectoryCdkApp.GLOBAL_RESOURCE))
                                                                                                           .build()));
        });
    }
}
