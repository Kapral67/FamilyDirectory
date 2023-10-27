package org.familydirectory.cdk.lambda.construct.utility;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.familydirectory.assets.lambda.function.models.LambdaFunctionModel;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.familydirectory.cdk.ses.FamilyDirectorySesStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.services.cognito.IUserPool;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.constructs.Construct;
import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static java.nio.file.Paths.get;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static software.amazon.awscdk.Duration.seconds;
import static software.amazon.awscdk.Fn.importValue;
import static software.amazon.awscdk.services.iam.Effect.ALLOW;
import static software.amazon.awscdk.services.iam.PolicyStatement.Builder.create;
import static software.amazon.awscdk.services.lambda.Architecture.ARM_64;
import static software.amazon.awscdk.services.lambda.Code.fromAsset;
import static software.amazon.awscdk.services.lambda.Runtime.JAVA_17;

public final
class LambdaFunctionConstructUtility {
    private static final Number ONE_GiB_IN_MiB = 1024;

    private
    LambdaFunctionConstructUtility () {
        super();
    }

    public static @NotNull
    Map<LambdaFunctionModel, Function> constructFunctionMap (final @NotNull Construct scope, final @NotNull List<? extends LambdaFunctionModel> values, final @Nullable IUserPool userPool) {
        return values.stream()
                     .collect(Collectors.toUnmodifiableMap(f -> f, f -> {
                         final Function function = new Function(scope, f.functionName(), FunctionProps.builder()
                                                                                                      .runtime(JAVA_17)
                                                                                                      .code(fromAsset(getLambdaJar(f.functionName())))
                                                                                                      .handler(f.handler())
                                                                                                      .timeout(seconds(60))
                                                                                                      .architecture(ARM_64)
                                                                                                      .memorySize(ONE_GiB_IN_MiB)
                                                                                                      .build());

                         Arrays.stream(LambdaUtils.EnvVar.values())
                               .forEach(env -> {
                                   switch (env) {
                                       case COGNITO_USER_POOL_ID -> ofNullable(userPool).map(IUserPool::getUserPoolId)
                                                                                        .ifPresent(id -> function.addEnvironment(env.name(), id));
                                       case ROOT_ID -> function.addEnvironment(env.name(), getenv("ORG_FAMILYDIRECTORY_ROOT_MEMBER_ID"));
                                       case SES_EMAIL_IDENTITY_NAME -> function.addEnvironment(env.name(), importValue(FamilyDirectorySesStack.SES_EMAIL_IDENTITY_NAME_EXPORT_NAME));
                                       case SES_MAIL_FROM_DOMAIN -> function.addEnvironment(env.name(), FamilyDirectorySesStack.SES_MAIL_FROM_DOMAIN_NAME);
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
    void constructFunctionPermissions (final @NotNull Construct scope, final @NotNull IUserPool userPool, final @NotNull Map<? extends LambdaFunctionModel, Function> values) {
        values.forEach((k, v) -> {
//      Assign Ddb Permissions
            ofNullable(k.ddbActions()).ifPresent(map -> map.forEach((table, actions) -> v.addToRolePolicy(create().effect(ALLOW)
                                                                                                                  .actions(actions)
                                                                                                                  .resources(singletonList(importValue(table.arnExportName())))
                                                                                                                  .build())));
//      Assign Cognito Permissions
            ofNullable(k.cognitoActions()).ifPresent(actions -> v.addToRolePolicy(create().effect(ALLOW)
                                                                                          .actions(actions)
                                                                                          .resources(singletonList(userPool.getUserPoolArn()))
                                                                                          .build()));

//            ofNullable(k.cognitoActions()).ifPresent(actions -> {
//                final String policyName = "%sCognitoPolicy".formatted(k.functionName());
//                requireNonNull(v.getRole()).attachInlinePolicy(Policy.Builder.create(scope, policyName)
//                                                                             .statements(singletonList(create().effect(ALLOW)
//                                                                                                               .actions(actions)
//                                                                                                               .resources(singletonList(userPool.getUserPoolArn()))
//                                                                                                               .build()))
//                                                                             .build());
//            });

//      Assign Ses Permissions
            ofNullable(k.sesActions()).ifPresent(actions -> v.addToRolePolicy(create().effect(ALLOW)
                                                                                      .actions(actions)
                                                                                      .resources(singletonList(importValue(FamilyDirectorySesStack.SES_EMAIL_IDENTITY_ARN_EXPORT_NAME)))
                                                                                      .build()));
        });
    }

    public static
    void constructFunctionPermissions (final @NotNull Construct scope, final @NotNull IUserPool userPool, final @NotNull List<? extends LambdaFunctionModel> values) {
        values.forEach(f -> {
            final IRole executionRole = Role.fromRoleArn(scope, f.roleArnExportName(), importValue(f.roleArnExportName()));

//      Assign Ddb Permissions
            ofNullable(f.ddbActions()).ifPresent(map -> map.forEach((table, actions) -> executionRole.addToPrincipalPolicy(create().effect(ALLOW)
                                                                                                                                   .actions(actions)
                                                                                                                                   .resources(singletonList(importValue(table.arnExportName())))
                                                                                                                                   .build())));

//      Assign Cognito Permissions
            ofNullable(f.cognitoActions()).ifPresent(actions -> executionRole.addToPrincipalPolicy(create().effect(ALLOW)
                                                                                                           .actions(actions)
                                                                                                           .resources(singletonList(userPool.getUserPoolArn()))
                                                                                                           .build()));
//      Assign Ses Permissions
            ofNullable(f.sesActions()).ifPresent(actions -> executionRole.addToPrincipalPolicy(create().effect(ALLOW)
                                                                                                       .actions(actions)
                                                                                                       .resources(
                                                                                                               singletonList(importValue(FamilyDirectorySesStack.SES_EMAIL_IDENTITY_ARN_EXPORT_NAME)))
                                                                                                       .build()));
        });
    }
}
