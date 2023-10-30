package org.familydirectory.cdk.test.lambda;

import java.util.List;
import java.util.Map;
import org.familydirectory.assets.lambda.function.api.enums.ApiFunction;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.familydirectory.cdk.lambda.FamilyDirectoryLambdaStack;
import org.familydirectory.cdk.lambda.construct.utility.LambdaFunctionConstructUtility;
import org.familydirectory.cdk.ses.FamilyDirectorySesStack;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Capture;
import software.amazon.awscdk.assertions.Template;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awscdk.assertions.Match.objectLike;

public
class FamilyDirectoryLambdaStackTest {

    @Test
    public
    void testStack () {
        final App app = new App();

        final FamilyDirectoryLambdaStack stack = new FamilyDirectoryLambdaStack(app, FamilyDirectoryCdkApp.LAMBDA_STACK_NAME, StackProps.builder()
                                                                                                                                        .env(FamilyDirectoryCdkApp.DEFAULT_ENV)
                                                                                                                                        .stackName(FamilyDirectoryCdkApp.LAMBDA_STACK_NAME)
                                                                                                                                        .build());

        final Template template = Template.fromStack(stack);

        for (final ApiFunction function : ApiFunction.values()) {
            final Capture functionIdCapture = new Capture();
            template.hasOutput(function.arnExportName(),
                               objectLike(Map.of("Value", singletonMap("Fn::GetAtt", List.of(functionIdCapture, "Arn")), "Export", singletonMap("Name", function.arnExportName()))));
            assertFalse(functionIdCapture.asString()
                                         .isBlank());
            final Capture functionRoleIdCapture = new Capture();
            template.hasOutput(function.roleArnExportName(),
                               objectLike(Map.of("Value", singletonMap("Fn::GetAtt", List.of(functionRoleIdCapture, "Arn")), "Export", singletonMap("Name", function.roleArnExportName()))));
            assertFalse(functionRoleIdCapture.asString()
                                             .isBlank());

            final Map<String, Map<String, Object>> functionMap = template.findResources("AWS::Lambda::Function", objectLike(singletonMap("Properties", Map.of("Architectures", singletonList(
                                                                                                                                                                      LambdaFunctionConstructUtility.ARCHITECTURE.toString()), "Environment", singletonMap("Variables", Map.of(LambdaUtils.EnvVar.ROOT_ID.name(), LambdaFunctionConstructUtility.ROOT_ID,
                                                                                                                                                                                                                                                                               LambdaUtils.EnvVar.SES_EMAIL_IDENTITY_NAME.name(),
                                                                                                                                                                                                                                                                               singletonMap("Fn::ImportValue",
                                                                                                                                                                                                                                                                                            FamilyDirectorySesStack.SES_EMAIL_IDENTITY_NAME_EXPORT_NAME),
                                                                                                                                                                                                                                                                               LambdaUtils.EnvVar.HOSTED_ZONE_NAME.name(),
                                                                                                                                                                                                                                                                               FamilyDirectoryDomainStack.HOSTED_ZONE_NAME)), "Handler",
                                                                                                                                                              function.handler(), "MemorySize",
                                                                                                                                                              function.memorySize(), "Role",
                                                                                                                                                              singletonMap("Fn::GetAtt",
                                                                                                                                                                           List.of(functionRoleIdCapture.asString(),
                                                                                                                                                                                   "Arn")), "Runtime",
                                                                                                                                                              LambdaFunctionConstructUtility.RUNTIME.toString(),
                                                                                                                                                              "Timeout", function.timeout()
                                                                                                                                                                                 .toSeconds()))));
            assertEquals(1, functionMap.size());
            assertTrue(functionMap.containsKey(functionIdCapture.asString()));
        }
    }
}
