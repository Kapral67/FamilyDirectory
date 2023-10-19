package org.familydirectory.cdk.lambda;

import java.util.Arrays;
import org.familydirectory.assets.lambda.function.api.enums.ApiFunction;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.lambda.construct.utility.LambdaFunctionConstructUtility;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cognito.IUserPool;
import software.amazon.awscdk.services.cognito.UserPool;
import software.constructs.Construct;
import static software.amazon.awscdk.Fn.importValue;

public
class FamilyDirectoryLambdaStack extends Stack {

    public
    FamilyDirectoryLambdaStack (final Construct scope, final String id, final StackProps stackProps)
    {
        super(scope, id, stackProps);
        final IUserPool userPool = UserPool.fromUserPoolId(this, FamilyDirectoryCognitoStack.COGNITO_USER_POOL_RESOURCE_ID, importValue(FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME));

//  API Lambda Functions
        LambdaFunctionConstructUtility.constructFunctionPermissions(this, userPool, LambdaFunctionConstructUtility.constructFunctionMap(this, Arrays.asList(ApiFunction.values())));
    }
}
