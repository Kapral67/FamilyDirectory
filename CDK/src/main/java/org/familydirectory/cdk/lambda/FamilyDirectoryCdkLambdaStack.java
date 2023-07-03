package org.familydirectory.cdk.lambda;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.constructs.Construct;

import static software.amazon.awscdk.services.lambda.Runtime.JAVA_17;

public class FamilyDirectoryCdkLambdaStack extends Stack {
    public FamilyDirectoryCdkLambdaStack(final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        Function insertMember = new Function(this, "insertMember", FunctionProps.builder()
                .runtime(JAVA_17)
                .build());
    }
}
