package org.familydirectory.cdk;

import org.familydirectory.cdk.apigateway.FamilyDirectoryApiGatewayStack;
import org.familydirectory.cdk.ddb.FamilyDirectoryDynamoDbStack;
import org.familydirectory.cdk.lambda.FamilyDirectoryLambdaStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.io.IOException;

public class FamilyDirectoryCdkApp {
    public static void main(final String[] args) throws IOException {
        App app = new App();
        final Environment env = Environment.builder().account(app.getAccount()).region(app.getRegion()).build();

        final String ddbStackName = "FamilyDirectoryDynamoDbStack";
        final FamilyDirectoryDynamoDbStack dynamoDbStack = new FamilyDirectoryDynamoDbStack(app, ddbStackName,
                StackProps.builder().env(env).stackName(ddbStackName).build());

        final String lambdaStackName = "FamilyDirectoryLambdaStack";
        final FamilyDirectoryLambdaStack lambdaStack = new FamilyDirectoryLambdaStack(app, lambdaStackName,
                StackProps.builder().env(env).stackName(lambdaStackName).build());
        lambdaStack.addDependency(dynamoDbStack);

        final String apiGatewayStackName = "FamilyDirectoryApiGatewayStack";
        new FamilyDirectoryApiGatewayStack(app, apiGatewayStackName,
                StackProps.builder().env(env).stackName(apiGatewayStackName).build()).addDependency(lambdaStack);

        app.synth();
    }
}
