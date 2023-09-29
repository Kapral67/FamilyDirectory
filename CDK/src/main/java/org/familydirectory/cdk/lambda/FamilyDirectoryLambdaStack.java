package org.familydirectory.cdk.lambda;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.familydirectory.assets.lambda.LambdaFunctionAttrs;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.constructs.Construct;
import static java.lang.System.getProperty;
import static java.nio.file.Paths.get;
import static java.util.Objects.requireNonNull;
import static org.familydirectory.assets.ddb.enums.DdbTable.FAMILIES;
import static org.familydirectory.assets.ddb.enums.DdbTable.MEMBERS;
import static org.familydirectory.assets.lambda.LambdaFunctionAttrs.values;
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

    public
    FamilyDirectoryLambdaStack (final Construct scope, final String id, final StackProps stackProps) throws IOException
    {
        super(scope, id, stackProps);

        for (final LambdaFunctionAttrs functionAttrs : values()) {
            final Function function = new Function(this, functionAttrs.functionName(), FunctionProps.builder()
                                                                                                    .runtime(JAVA_17)
                                                                                                    .code(fromAsset(getLambdaJar(functionAttrs.functionName())))
                                                                                                    .handler(functionAttrs.handler())
                                                                                                    .timeout(seconds(60))
                                                                                                    .architecture(ARM_64)
                                                                                                    .memorySize(ONE_GiB_IN_MiB)
                                                                                                    .reservedConcurrentExecutions(1)
                                                                                                    .build());
            if (!functionAttrs.actions()
                              .isEmpty())
            {
                final PolicyStatement statement = create().effect(ALLOW)
                                                          .actions(functionAttrs.actions())
                                                          .resources(List.of(importValue(MEMBERS.arnExportName()), importValue(FAMILIES.arnExportName())))
                                                          .build();
                requireNonNull(function.getRole()).addToPrincipalPolicy(statement);
            }
            new CfnOutput(this, functionAttrs.arnExportName(), CfnOutputProps.builder()
                                                                             .value(function.getFunctionArn())
                                                                             .exportName(functionAttrs.arnExportName())
                                                                             .build());
        }
    }

    private static
    String getLambdaJar (final String lambdaName) throws IOException {
        final File jarDir = new File(get(getProperty("user.dir"), "..", "assets", lambdaName, "target").toUri());
        return requireNonNull(jarDir.listFiles((dir, name) -> name.toLowerCase()
                                                                  .startsWith("familydirectory") && name.toLowerCase()
                                                                                                        .endsWith(".jar")))[0].getCanonicalPath();
    }
}
