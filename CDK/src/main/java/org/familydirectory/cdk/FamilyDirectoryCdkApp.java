package org.familydirectory.cdk;

import org.familydirectory.cdk.ddb.FamilyDirectoryCdkDynamoDbStack;
import org.familydirectory.cdk.lambda.FamilyDirectoryCdkLambdaStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class FamilyDirectoryCdkApp {
    public static void main(final String[] args) {
        App app = new App();
        final Environment default_env = Environment.builder().account(app.getAccount()).region(app.getRegion()).build();

        final String ddbStackName = "FamilyDirectoryDynamoDbStack";
        new FamilyDirectoryCdkDynamoDbStack(app, ddbStackName,
                StackProps.builder().env(default_env).stackName(ddbStackName).build());

        final String lambdaStackName = "FamilyDirectoryLambdaStack";
        new FamilyDirectoryCdkLambdaStack(app, lambdaStackName,
                StackProps.builder().env(default_env).stackName(lambdaStackName).build());

        app.synth();
    }
}
