package org.familydirectory.cdk.lambda;

import org.familydirectory.assets.lambda.LambdaFunctionAttrs;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.constructs.Construct;

import java.io.File;
import java.io.IOException;

import static java.lang.System.getProperty;
import static java.nio.file.Paths.get;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.familydirectory.assets.ddb.DdbTable.MEMBERS;
import static org.familydirectory.assets.lambda.LambdaFunctionAttrs.ADMIN_CREATE_MEMBER;
import static org.familydirectory.assets.lambda.LambdaFunctionAttrs.values;
import static software.amazon.awscdk.Duration.seconds;
import static software.amazon.awscdk.Fn.importValue;
import static software.amazon.awscdk.services.iam.Effect.ALLOW;
import static software.amazon.awscdk.services.iam.PolicyStatement.Builder.create;
import static software.amazon.awscdk.services.lambda.Architecture.ARM_64;
import static software.amazon.awscdk.services.lambda.Code.fromAsset;
import static software.amazon.awscdk.services.lambda.Runtime.JAVA_17;

public class FamilyDirectoryCdkLambdaStack extends Stack {
    public FamilyDirectoryCdkLambdaStack(final Construct scope, final String id, final StackProps stackProps)
            throws IOException {
        super(scope, id, stackProps);

        for (final LambdaFunctionAttrs functionAttrs : values()) {
            final Function function = new Function(this, functionAttrs.functionName(),
                    FunctionProps.builder().runtime(JAVA_17).code(fromAsset(getLambdaJar(functionAttrs.functionName())))
                            .handler(functionAttrs.handler()).timeout(seconds(30)).architecture(ARM_64)
                            .memorySize(512/*MB*/).build());
            if (functionAttrs == ADMIN_CREATE_MEMBER) {
                final PolicyStatement statement = create().effect(ALLOW).actions(functionAttrs.actions())
                        .resources(singletonList(importValue(MEMBERS.arnExportName()))).build();
                requireNonNull(function.getRole()).addToPrincipalPolicy(statement);
            }
        }
    }

    private static String getLambdaJar(final String lambdaName) throws IOException {
        final File jarDir = new File(get(getProperty("user.dir"), "..", "assets", lambdaName, "target").toUri());
        return requireNonNull(jarDir.listFiles((dir, name) -> name.toLowerCase().startsWith("familydirectory") &&
                name.toLowerCase().endsWith(".jar")))[0].getCanonicalPath();
    }
}
