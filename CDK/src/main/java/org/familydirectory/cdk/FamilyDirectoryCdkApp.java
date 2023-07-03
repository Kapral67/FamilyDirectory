package org.familydirectory.cdk;

import org.familydirectory.cdk.ddb.FamilyDirectoryCdkDynamoDbStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class FamilyDirectoryCdkApp {
    private static final Environment ENVIRONMENT = Environment.builder()
            .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
            .region(System.getenv("CDK_DEFAULT_REGION"))
            .build();
    private static final StackProps STACK_PROPS = StackProps.builder().env(ENVIRONMENT).build();
    
    public static void main(final String[] args) {
        App app = new App();

        new FamilyDirectoryCdkDynamoDbStack(app, "FamilyDirectoryDynamoDbStack", STACK_PROPS);

        app.synth();
    }
}

