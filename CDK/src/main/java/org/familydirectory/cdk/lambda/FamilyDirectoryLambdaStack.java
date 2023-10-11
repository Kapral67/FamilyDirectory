package org.familydirectory.cdk.lambda;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.lambda.function.api.enums.ApiFunction;
import org.familydirectory.assets.lambda.function.trigger.enums.TriggerFunction;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cognito.IUserPool;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.constructs.Construct;
import static java.lang.System.getProperty;
import static java.nio.file.Paths.get;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static software.amazon.awscdk.Duration.seconds;
import static software.amazon.awscdk.Fn.importValue;
import static software.amazon.awscdk.services.iam.Effect.ALLOW;
import static software.amazon.awscdk.services.iam.PolicyStatement.Builder.create;
import static software.amazon.awscdk.services.lambda.Architecture.ARM_64;
import static software.amazon.awscdk.services.lambda.Code.fromAsset;
import static software.amazon.awscdk.services.lambda.Runtime.JAVA_17;

public
class FamilyDirectoryLambdaStack extends Stack {
    private static final Number ONE_GiB_IN_MiB = 1024;
    private static final Map<DdbTable, String> DDB_TABLE_ARN_IMPORT_MAP = Map.of(DdbTable.COGNITO, importValue(DdbTable.COGNITO.arnExportName()), DdbTable.MEMBER,
                                                                                 importValue(DdbTable.MEMBER.arnExportName()), DdbTable.FAMILY, importValue(DdbTable.FAMILY.arnExportName()));

    public
    FamilyDirectoryLambdaStack (final Construct scope, final String id, final StackProps stackProps) throws IOException
    {
        super(scope, id, stackProps);

        for (final ApiFunction func : ApiFunction.values()) {
            final Function function = new Function(this, func.functionName(), FunctionProps.builder()
                                                                                           .runtime(JAVA_17)
                                                                                           .code(fromAsset(getLambdaJar(func.functionName())))
                                                                                           .handler(func.handler())
                                                                                           .timeout(seconds(60))
                                                                                           .architecture(ARM_64)
                                                                                           .memorySize(ONE_GiB_IN_MiB)
                                                                                           .reservedConcurrentExecutions(1)
                                                                                           .build());
            if (!func.actions()
                     .isEmpty())
            {
                final PolicyStatement statement = create().effect(ALLOW)
                                                          .actions(func.actions())
                                                          .resources(List.of(DDB_TABLE_ARN_IMPORT_MAP.get(DdbTable.MEMBER), DDB_TABLE_ARN_IMPORT_MAP.get(DdbTable.FAMILY)))
                                                          .build();
                requireNonNull(function.getRole()).addToPrincipalPolicy(statement);
            }
//      Allow GetItem from Cognito Table
            requireNonNull(function.getRole()).addToPrincipalPolicy(create().effect(ALLOW)
                                                                            .actions(singletonList("dynamodb:GetItem"))
                                                                            .resources(singletonList(DDB_TABLE_ARN_IMPORT_MAP.get(DdbTable.COGNITO)))
                                                                            .build());
            new CfnOutput(this, func.arnExportName(), CfnOutputProps.builder()
                                                                    .value(function.getFunctionArn())
                                                                    .exportName(func.arnExportName())
                                                                    .build());
        }

        final IUserPool userPool = UserPool.fromUserPoolId(this, FamilyDirectoryCognitoStack.COGNITO_USER_POOL_RESOURCE_ID, importValue(FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME));
        for (final TriggerFunction func : TriggerFunction.values()) {
            final Function function = new Function(this, func.functionName(), FunctionProps.builder()
                                                                                           .runtime(JAVA_17)
                                                                                           .code(fromAsset(getLambdaJar(func.functionName())))
                                                                                           .handler(func.handler())
                                                                                           .timeout(seconds(60))
                                                                                           .architecture(ARM_64)
                                                                                           .memorySize(ONE_GiB_IN_MiB)
                                                                                           .reservedConcurrentExecutions(1)
                                                                                           .build());
            requireNonNull(function.getRole()).addToPrincipalPolicy(create().effect(ALLOW)
                                                                            .actions(func.dynamoDbActions())
                                                                            .resources(List.of(DDB_TABLE_ARN_IMPORT_MAP.get(DdbTable.COGNITO), DDB_TABLE_ARN_IMPORT_MAP.get(DdbTable.MEMBER)))
                                                                            .build());
            requireNonNull(function.getRole()).addToPrincipalPolicy(create().effect(ALLOW)
                                                                            .actions(func.cognitoActions())
                                                                            .resources(singletonList(userPool.getUserPoolArn()))
                                                                            .build());
            new CfnOutput(this, func.arnExportName(), CfnOutputProps.builder()
                                                                    .value(function.getFunctionArn())
                                                                    .exportName(func.arnExportName())
                                                                    .build());
        }
    }

    @NotNull
    private static
    String getLambdaJar (final String lambdaName) throws IOException {
        final File jarDir = new File(get(getProperty("user.dir"), "..", "assets", lambdaName, "target").toUri());
        return requireNonNull(jarDir.listFiles((dir, name) -> name.toLowerCase()
                                                                  .startsWith("familydirectory") && name.toLowerCase()
                                                                                                        .endsWith(".jar")))[0].getCanonicalPath();
    }
}
