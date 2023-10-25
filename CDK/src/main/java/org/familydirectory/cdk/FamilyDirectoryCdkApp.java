package org.familydirectory.cdk;

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

    public static final Environment DEFAULT_ENVIRONMENT = Environment.builder()
                                                                     .account(getenv("CDK_DEFAULT_ACCOUNT"))
                                                                     .region(getenv("CDK_DEFAULT_REGION"))
                                                                     .build();

    public static final String DOMAIN_STACK_NAME = "FamilyDirectoryDomainStack";
    public static final String SES_STACK_NAME = "FamilyDirectorySesStack";
    public static final String COGNITO_STACK_NAME = "FamilyDirectoryCognitoStack";
    public static final String DDB_STACK_NAME = "FamilyDirectoryDynamoDbStack";
    public static final String LAMBDA_STACK_NAME = "FamilyDirectoryLambdaStack";
    public static final String API_STACK_NAME = "FamilyDirectoryApiGatewayStack";

    private
    FamilyDirectoryCdkApp () {
        super();
    }

    public static
    void main (final String[] args) {
        final App app = new App();

        final FamilyDirectoryDomainStack domainStack = new FamilyDirectoryDomainStack(app, DOMAIN_STACK_NAME, StackProps.builder()
                                                                                                                        .env(DEFAULT_ENVIRONMENT)
                                                                                                                        .stackName(DOMAIN_STACK_NAME)
                                                                                                                        .build());

        final FamilyDirectorySesStack sesStack = new FamilyDirectorySesStack(app, SES_STACK_NAME, StackProps.builder()
                                                                                                            .env(DEFAULT_ENVIRONMENT)
                                                                                                            .stackName(SES_STACK_NAME)
                                                                                                            .build());
        sesStack.addDependency(domainStack);

        final FamilyDirectoryCognitoStack cognitoStack = new FamilyDirectoryCognitoStack(app, COGNITO_STACK_NAME, StackProps.builder()
                                                                                                                            .env(DEFAULT_ENVIRONMENT)
                                                                                                                            .stackName(COGNITO_STACK_NAME)
                                                                                                                            .build());
        cognitoStack.addDependency(sesStack);

        final FamilyDirectoryDynamoDbStack dynamoDbStack = new FamilyDirectoryDynamoDbStack(app, DDB_STACK_NAME, StackProps.builder()
                                                                                                                           .env(DEFAULT_ENVIRONMENT)
                                                                                                                           .stackName(DDB_STACK_NAME)
                                                                                                                           .build());

        final FamilyDirectoryLambdaStack lambdaStack = new FamilyDirectoryLambdaStack(app, LAMBDA_STACK_NAME, StackProps.builder()
                                                                                                                        .env(DEFAULT_ENVIRONMENT)
                                                                                                                        .stackName(LAMBDA_STACK_NAME)
                                                                                                                        .build());
        lambdaStack.addDependency(dynamoDbStack);
        lambdaStack.addDependency(cognitoStack);

        final FamilyDirectoryApiGatewayStack apiGatewayStack = new FamilyDirectoryApiGatewayStack(app, API_STACK_NAME, StackProps.builder()
                                                                                                                                 .env(DEFAULT_ENVIRONMENT)
                                                                                                                                 .stackName(API_STACK_NAME)
                                                                                                                                 .build());
        apiGatewayStack.addDependency(lambdaStack);

        app.synth();
    }
}
