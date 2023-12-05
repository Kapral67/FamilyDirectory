package org.familydirectory.cdk.lambda.construct.utility;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.assets.lambda.function.models.LambdaFunctionModel;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.services.cognito.IUserPool;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.s3.IBucket;
import software.constructs.Construct;
import static java.lang.System.getProperty;
import static java.nio.file.Paths.get;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static software.amazon.awscdk.Fn.importValue;
import static software.amazon.awscdk.services.iam.Effect.ALLOW;
import static software.amazon.awscdk.services.iam.PolicyStatement.Builder.create;
import static software.amazon.awscdk.services.lambda.Architecture.ARM_64;
import static software.amazon.awscdk.services.lambda.Code.fromAsset;
import static software.amazon.awscdk.services.lambda.Runtime.JAVA_17;

public final
class LambdaFunctionConstructUtility {
    public static final Runtime RUNTIME = JAVA_17;
    public static final Architecture ARCHITECTURE = ARM_64;
    public static final String ROOT_ID = DdbUtils.ROOT_MEMBER_ID;

    private
    LambdaFunctionConstructUtility () {
        super();
    }

    public static @NotNull
    Map<LambdaFunctionModel, Function> constructFunctionMap (final @NotNull Construct scope, final @NotNull List<? extends LambdaFunctionModel> values, final @Nullable IHostedZone hostedZone,
                                                             final @Nullable IUserPool userPool, final @Nullable IBucket pdfBucket)
    {
        return values.stream()
                     .collect(Collectors.toUnmodifiableMap(f -> f, f -> {
                         final Function function = new Function(scope, f.functionName(), FunctionProps.builder()
                                                                                                      .runtime(RUNTIME)
                                                                                                      .code(fromAsset(getLambdaJar(f.functionName())))
                                                                                                      .handler(f.handler())
                                                                                                      .timeout(f.timeout())
                                                                                                      .architecture(ARCHITECTURE)
                                                                                                      .memorySize(f.memorySize())
                                                                                                      .build());

                         Arrays.stream(LambdaUtils.EnvVar.values())
                               .forEach(env -> {
                                   switch (env) {
                                       case COGNITO_USER_POOL_ID -> ofNullable(userPool).map(IUserPool::getUserPoolId)
                                                                                        .ifPresent(id -> function.addEnvironment(env.name(), id));
                                       case ROOT_ID -> function.addEnvironment(env.name(), ROOT_ID);
                                       case HOSTED_ZONE_NAME -> ofNullable(hostedZone).map(IHostedZone::getZoneName)
                                                                                      .ifPresent(hostedZoneName -> function.addEnvironment(env.name(), hostedZoneName));
                                       case S3_PDF_BUCKET_NAME -> ofNullable(pdfBucket).map(IBucket::getBucketName)
                                                                                       .ifPresent(name -> function.addEnvironment(env.name(), name));
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
    String getLambdaJar (final String lambdaName) {
        final File jarDir = new File(get(getProperty("user.dir"), "..", "assets", lambdaName, "target").toUri());
        try {
            return requireNonNull(jarDir.listFiles((dir, name) -> name.toLowerCase()
                                                                      .startsWith("familydirectory") && name.toLowerCase()
                                                                                                            .endsWith(".jar")))[0].getCanonicalPath();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static
    void constructFunctionPermissions (final @NotNull Construct scope, final @NotNull Map<? extends LambdaFunctionModel, Function> values, final @Nullable IUserPool userPool,
                                       final @Nullable IBucket pdfBucket)
    {
        values.forEach((k, v) -> {
//      Assign Ddb Permissions
            ofNullable(k.ddbActions()).ifPresent(map -> map.forEach((table, actions) -> {
                final String tableArn = importValue(table.arnExportName());
                v.addToRolePolicy(create().effect(ALLOW)
                                          .actions(actions)
                                          .resources(List.of(tableArn, "%s/index/*".formatted(tableArn)))
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
                                                                                      .resources(singletonList("*"))
                                                                                      .build()));

//      Assign S3 Permissions
            ofNullable(pdfBucket).map(IBucket::getBucketArn)
                                 .ifPresent(pdfBucketArn -> ofNullable(k.sssActions()).ifPresent(actions -> v.addToRolePolicy(create().effect(ALLOW)
                                                                                                                                      .actions(actions)
                                                                                                                                      .resources(singletonList("%s/*".formatted(pdfBucketArn)))
                                                                                                                                      .build())));
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
                                                           .resources(List.of(tableArn, "%s/index/*".formatted(tableArn)))
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
                                                                                                       .resources(singletonList("*"))
                                                                                                       .build()));

//      Assign S3 Permissions
            ofNullable(pdfBucket).map(IBucket::getBucketArn)
                                 .ifPresent(pdfBucketArn -> ofNullable(f.sssActions()).ifPresent(actions -> executionRole.addToPrincipalPolicy(create().effect(ALLOW)
                                                                                                                                                       .actions(actions)
                                                                                                                                                       .resources(singletonList(
                                                                                                                                                               "%s/*".formatted(pdfBucketArn)))
                                                                                                                                                       .build())));
        });
    }
}
