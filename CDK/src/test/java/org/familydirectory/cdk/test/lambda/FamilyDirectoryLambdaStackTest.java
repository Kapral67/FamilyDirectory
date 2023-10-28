package org.familydirectory.cdk.test.lambda;

import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.lambda.FamilyDirectoryLambdaStack;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Template;

public
class FamilyDirectoryLambdaStackTest {

    @Disabled
    @Test
    public
    void testStack () {
        final App app = new App();

        final FamilyDirectoryLambdaStack stack = new FamilyDirectoryLambdaStack(app, FamilyDirectoryCdkApp.LAMBDA_STACK_NAME, StackProps.builder()
                                                                                                                                        .env(FamilyDirectoryCdkApp.DEFAULT_ENV)
                                                                                                                                        .stackName(FamilyDirectoryCdkApp.LAMBDA_STACK_NAME)
                                                                                                                                        .build());

        final Template template = Template.fromStack(stack);
    }
}
