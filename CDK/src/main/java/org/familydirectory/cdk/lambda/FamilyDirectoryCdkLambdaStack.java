package org.familydirectory.cdk.lambda;

import org.familydirectory.assets.lambda.LambdaFunctionAttrs;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.constructs.Construct;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;

import static org.familydirectory.assets.ddb.DdbTable.MEMBERS;
import static org.familydirectory.assets.lambda.LambdaFunctionAttrs.ADMIN_CREATE_MEMBER;
import static software.amazon.awscdk.services.iam.Effect.ALLOW;
import static software.amazon.awscdk.services.lambda.Runtime.JAVA_17;

public class FamilyDirectoryCdkLambdaStack extends Stack {
    public FamilyDirectoryCdkLambdaStack(final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        for (final LambdaFunctionAttrs functionAttrs : LambdaFunctionAttrs.values()) {
            Function function = new Function(this, functionAttrs.functionName(),
                    FunctionProps.builder().runtime(JAVA_17)
                            .code(Code.fromAsset(getLambdaJar(functionAttrs.functionName())))
                            .handler(functionAttrs.handler()).build());
            if (functionAttrs == ADMIN_CREATE_MEMBER) {
                PolicyStatement statement =
                        PolicyStatement.Builder.create().effect(ALLOW).actions(functionAttrs.actions())
                                .resources(Collections.singletonList(Fn.importValue(MEMBERS.arnExportName()))).build();
                Objects.requireNonNull(function.getRole()).addToPrincipalPolicy(statement);
            }
        }
    }

    private static String getLambdaJar(final String lambdaName) {
        final File jarDir =
                new File(Paths.get(System.getProperty("user.dir"), "..", "assets", lambdaName, "target").toUri());
        return Objects.requireNonNull(jarDir.listFiles(
                (dir, name) -> name.toLowerCase().startsWith("familydirectory") &&
                        name.toLowerCase().endsWith(".jar")))[0].getName();
    }
}
