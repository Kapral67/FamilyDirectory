package org.familydirectory.cdk;

import org.familydirectory.cdk.ddb.FamilyDirectoryCdkDynamoDbStack;
import org.familydirectory.cdk.lambda.FamilyDirectoryCdkLambdaStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.io.IOException;

public class FamilyDirectoryCdkApp {
    public static void main(final String[] args) throws IOException {
        App app = new App();
        final Environment env = Environment.builder().account(app.getAccount()).region(app.getRegion()).build();

        final String ddbStackName = "FamilyDirectoryDynamoDbStack";
        new FamilyDirectoryCdkDynamoDbStack(app, ddbStackName,
                StackProps.builder().env(env).stackName(ddbStackName).build());

        final String lambdaStackName = "FamilyDirectoryLambdaStack";
        new FamilyDirectoryCdkLambdaStack(app, lambdaStackName,
                StackProps.builder().env(env).stackName(lambdaStackName).build());

        app.synth();
    }
}
