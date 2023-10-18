package org.familydirectory.cdk.lambda;

import java.io.File;
import java.io.IOException;
import org.familydirectory.assets.lambda.function.api.enums.ApiFunction;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.ses.FamilyDirectorySesStack;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cognito.IUserPool;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;
import static java.lang.System.getProperty;
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

public
class FamilyDirectoryLambdaStack extends Stack {
    public static final Number ONE_GiB_IN_MiB = 1024;
    public static final Runtime RUNTIME = JAVA_17;

    public
    FamilyDirectoryLambdaStack (final Construct scope, final String id, final StackProps stackProps)
    {
        super(scope, id, stackProps);
        final IUserPool userPool = UserPool.fromUserPoolId(this, FamilyDirectoryCognitoStack.COGNITO_USER_POOL_RESOURCE_ID, importValue(FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME));

//  API Lambda Functions
        for (final ApiFunction func : ApiFunction.values()) {
            final Function function = new Function(this, func.functionName(), FunctionProps.builder()
                                                                                           .runtime(RUNTIME)
                                                                                           .code(fromAsset(getLambdaJar(func.functionName())))
                                                                                           .handler(func.handler())
                                                                                           .timeout(seconds(60))
                                                                                           .architecture(ARM_64)
                                                                                           .memorySize(ONE_GiB_IN_MiB)
                                                                                           .reservedConcurrentExecutions(1)
                                                                                           .build());

//      Assign Ddb Permissions
            ofNullable(func.ddbActions()).ifPresent(map -> map.forEach((table, actions) -> requireNonNull(function.getRole()).addToPrincipalPolicy(create().effect(ALLOW)
                                                                                                                                                           .actions(actions)
                                                                                                                                                           .resources(singletonList(
                                                                                                                                                                   importValue(table.arnExportName())))
                                                                                                                                                           .build())));
//      Assign Cognito Permissions
            ofNullable(func.cognitoActions()).ifPresent(actions -> requireNonNull(function.getRole()).addToPrincipalPolicy(create().effect(ALLOW)
                                                                                                                                   .actions(actions)
                                                                                                                                   .resources(singletonList(userPool.getUserPoolArn()))
                                                                                                                                   .build()));
//      Assign Ses Permissions
            ofNullable(func.sesActions()).ifPresent(actions -> requireNonNull(function.getRole()).addToPrincipalPolicy(create().effect(ALLOW)
                                                                                                                               .actions(actions)
                                                                                                                               .resources(singletonList(importValue(
                                                                                                                                       FamilyDirectorySesStack.SES_EMAIL_IDENTITY_ARN_EXPORT_NAME)))
                                                                                                                               .build()));
//      Assign Route53 Permissions
            ofNullable(func.route53Actions()).ifPresent(actions -> requireNonNull(function.getRole()).addToPrincipalPolicy(create().effect(ALLOW)
                                                                                                                                   .actions(actions)
                                                                                                                                   .resources(singletonList("*"))
                                                                                                                                   .build()));

            new CfnOutput(this, func.arnExportName(), CfnOutputProps.builder()
                                                                    .value(function.getFunctionArn())
                                                                    .exportName(func.arnExportName())
                                                                    .build());
        }
    }

    @NotNull
    public static
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
}
