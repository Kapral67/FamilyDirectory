package org.familydirectory.cdk;

import org.familydirectory.cdk.amplify.FamilyDirectoryAmplifyStack;
import org.familydirectory.cdk.apigateway.FamilyDirectoryApiGatewayStack;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoUsEastOneStack;
import org.familydirectory.cdk.ddb.FamilyDirectoryDynamoDbStack;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.familydirectory.cdk.lambda.FamilyDirectoryLambdaStack;
import org.familydirectory.cdk.ses.FamilyDirectorySesStack;
import org.familydirectory.cdk.sss.FamilyDirectorySssStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awssdk.regions.Region;
import static java.lang.System.getenv;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public final
class FamilyDirectoryCdkApp {
    public static final String DEFAULT_ACCOUNT;
    public static final String DEFAULT_REGION;
    public static final Environment DEFAULT_ENV;
    public static final Environment US_EAST_1_ENV;
    public static final String DOMAIN_STACK_NAME = "FamilyDirectoryDomainStack";
    public static final String SES_STACK_NAME = "FamilyDirectorySesStack";
    public static final String COGNITO_STACK_NAME = "FamilyDirectoryCognitoStack";
    public static final String COGNITO_US_EAST_1_STACK_NAME = "FamilyDirectoryCognitoUsEastOneStack";
    public static final String DDB_STACK_NAME = "FamilyDirectoryDynamoDbStack";
    public static final String SSS_STACK_NAME = "FamilyDirectorySssStack";
    public static final String LAMBDA_STACK_NAME = "FamilyDirectoryLambdaStack";
    public static final String API_STACK_NAME = "FamilyDirectoryApiGatewayStack";
    public static final String AMPLIFY_STACK_NAME = "FamilyDirectoryAmplifyStack";
    public static final String HTTPS_PREFIX = "https://";
    public static final String GLOBAL_RESOURCE = "*";

    static {
        String account = getenv("CDK_DEFAULT_ACCOUNT");
        if (isNull(account)) {
            account = requireNonNull(getenv("AWS_ACCOUNT_ID"));
        }
        DEFAULT_ACCOUNT = account;
        String region = getenv("CDK_DEFAULT_REGION");
        if (isNull(region)) {
            region = requireNonNull(getenv("AWS_REGION"));
        }
        DEFAULT_REGION = region;
        DEFAULT_ENV = Environment.builder()
                                 .account(DEFAULT_ACCOUNT)
                                 .region(DEFAULT_REGION)
                                 .build();
        US_EAST_1_ENV = Environment.builder()
                                   .account(DEFAULT_ACCOUNT)
                                   .region(Region.US_EAST_1.toString())
                                   .build();
    }

    private
    FamilyDirectoryCdkApp () {
        super();
    }

    public static
    void main (final String[] args) {
        final App app = new App();

        final FamilyDirectoryDomainStack domainStack = new FamilyDirectoryDomainStack(app, DOMAIN_STACK_NAME, StackProps.builder()
                                                                                                                        .env(DEFAULT_ENV)
                                                                                                                        .stackName(DOMAIN_STACK_NAME)
                                                                                                                        .build());

        final FamilyDirectorySesStack sesStack = new FamilyDirectorySesStack(app, SES_STACK_NAME, StackProps.builder()
                                                                                                            .env(DEFAULT_ENV)
                                                                                                            .stackName(SES_STACK_NAME)
                                                                                                            .build());
        sesStack.addDependency(domainStack);

        final FamilyDirectoryCognitoUsEastOneStack cognitoUsEastOneStack = new FamilyDirectoryCognitoUsEastOneStack(app, COGNITO_US_EAST_1_STACK_NAME, StackProps.builder()
                                                                                                                                                                 .env(US_EAST_1_ENV)
                                                                                                                                                                 .stackName(
                                                                                                                                                                         COGNITO_US_EAST_1_STACK_NAME)
                                                                                                                                                                 .build());
        cognitoUsEastOneStack.addDependency(domainStack);

        final FamilyDirectoryDynamoDbStack dynamoDbStack = new FamilyDirectoryDynamoDbStack(app, DDB_STACK_NAME, StackProps.builder()
                                                                                                                           .env(DEFAULT_ENV)
                                                                                                                           .stackName(DDB_STACK_NAME)
                                                                                                                           .build());

        final FamilyDirectoryCognitoStack cognitoStack = new FamilyDirectoryCognitoStack(app, COGNITO_STACK_NAME, StackProps.builder()
                                                                                                                            .env(DEFAULT_ENV)
                                                                                                                            .stackName(COGNITO_STACK_NAME)
                                                                                                                            .build());
        cognitoStack.addDependency(domainStack);
        cognitoStack.addDependency(sesStack);
        cognitoStack.addDependency(cognitoUsEastOneStack);
        cognitoStack.addDependency(dynamoDbStack);

        final FamilyDirectoryAmplifyStack amplifyStack = new FamilyDirectoryAmplifyStack(app, AMPLIFY_STACK_NAME, StackProps.builder()
                                                                                                                            .env(DEFAULT_ENV)
                                                                                                                            .stackName(AMPLIFY_STACK_NAME)
                                                                                                                            .build());
        amplifyStack.addDependency(domainStack);
        amplifyStack.addDependency(dynamoDbStack);
        amplifyStack.addDependency(cognitoStack);

        final FamilyDirectorySssStack sssStack = new FamilyDirectorySssStack(app, SSS_STACK_NAME, StackProps.builder()
                                                                                                            .env(DEFAULT_ENV)
                                                                                                            .stackName(SSS_STACK_NAME)
                                                                                                            .build());

        final FamilyDirectoryLambdaStack lambdaStack = new FamilyDirectoryLambdaStack(app, LAMBDA_STACK_NAME, StackProps.builder()
                                                                                                                        .env(DEFAULT_ENV)
                                                                                                                        .stackName(LAMBDA_STACK_NAME)
                                                                                                                        .build());
        lambdaStack.addDependency(domainStack);
        lambdaStack.addDependency(sesStack);
        lambdaStack.addDependency(dynamoDbStack);
        lambdaStack.addDependency(cognitoStack);
        lambdaStack.addDependency(sssStack);
        lambdaStack.addDependency(amplifyStack);

        final FamilyDirectoryApiGatewayStack apiGatewayStack = new FamilyDirectoryApiGatewayStack(app, API_STACK_NAME, StackProps.builder()
                                                                                                                                 .env(DEFAULT_ENV)
                                                                                                                                 .stackName(API_STACK_NAME)
                                                                                                                                 .build());
        apiGatewayStack.addDependency(domainStack);
        apiGatewayStack.addDependency(cognitoStack);
        apiGatewayStack.addDependency(lambdaStack);

        app.synth();
    }
}
