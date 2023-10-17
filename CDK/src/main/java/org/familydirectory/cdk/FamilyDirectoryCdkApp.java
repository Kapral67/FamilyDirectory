package org.familydirectory.cdk;

import java.io.IOException;
import org.familydirectory.cdk.apigateway.FamilyDirectoryApiGatewayStack;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.ddb.FamilyDirectoryDynamoDbStack;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.familydirectory.cdk.lambda.FamilyDirectoryLambdaStack;
import org.familydirectory.cdk.ses.FamilyDirectorySesStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import static java.lang.System.getenv;

public final
class FamilyDirectoryCdkApp {
    private
    FamilyDirectoryCdkApp () {
        super();
    }

    public static
    void main (final String[] args) throws IOException {
        final App app = new App();
        final Environment env = Environment.builder()
                                           .account(getenv("CDK_DEFAULT_ACCOUNT"))
                                           .region(getenv("CDK_DEFAULT_REGION"))
                                           .build();

        final String domainStackName = "FamilyDirectoryDomainStack";
        final FamilyDirectoryDomainStack domainStack = new FamilyDirectoryDomainStack(app, domainStackName, StackProps.builder()
                                                                                                                      .env(env)
                                                                                                                      .stackName(domainStackName)
                                                                                                                      .build());

        final String sesStackName = "FamilyDirectorySesStack";
        final FamilyDirectorySesStack sesStack = new FamilyDirectorySesStack(app, sesStackName, StackProps.builder()
                                                                                                          .env(env)
                                                                                                          .stackName(sesStackName)
                                                                                                          .build());
        sesStack.addDependency(domainStack);

        final String cognitoStackName = "FamilyDirectoryCognitoStack";
        final FamilyDirectoryCognitoStack cognitoStack = new FamilyDirectoryCognitoStack(app, cognitoStackName, StackProps.builder()
                                                                                                                          .env(env)
                                                                                                                          .stackName(cognitoStackName)
                                                                                                                          .build());
        cognitoStack.addDependency(sesStack);

        final String ddbStackName = "FamilyDirectoryDynamoDbStack";
        final FamilyDirectoryDynamoDbStack dynamoDbStack = new FamilyDirectoryDynamoDbStack(app, ddbStackName, StackProps.builder()
                                                                                                                         .env(env)
                                                                                                                         .stackName(ddbStackName)
                                                                                                                         .build());

        final String lambdaStackName = "FamilyDirectoryLambdaStack";
        final FamilyDirectoryLambdaStack lambdaStack = new FamilyDirectoryLambdaStack(app, lambdaStackName, StackProps.builder()
                                                                                                                      .env(env)
                                                                                                                      .stackName(lambdaStackName)
                                                                                                                      .build());
        lambdaStack.addDependency(dynamoDbStack);
        lambdaStack.addDependency(cognitoStack);

        final String apiGatewayStackName = "FamilyDirectoryApiGatewayStack";
        final FamilyDirectoryApiGatewayStack apiGatewayStack = new FamilyDirectoryApiGatewayStack(app, apiGatewayStackName, StackProps.builder()
                                                                                                                                      .env(env)
                                                                                                                                      .stackName(apiGatewayStackName)
                                                                                                                                      .build());
        apiGatewayStack.addDependency(lambdaStack);

        app.synth();
    }
}
