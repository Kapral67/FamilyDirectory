package org.familydirectory.cdk.test.lambda;

import java.util.List;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.lambda.function.api.enums.ApiFunction;
import org.familydirectory.assets.lambda.function.stream.enums.StreamFunction;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.familydirectory.cdk.lambda.FamilyDirectoryLambdaStack;
import org.familydirectory.cdk.lambda.construct.utility.LambdaFunctionConstructUtility;
import org.familydirectory.cdk.ses.FamilyDirectorySesStack;
import org.familydirectory.cdk.sss.FamilyDirectorySssStack;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Capture;
import software.amazon.awscdk.assertions.Template;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awscdk.assertions.Match.absent;
import static software.amazon.awscdk.assertions.Match.objectLike;

public
class FamilyDirectoryLambdaStackTest {

    private static final String HOSTED_ZONE_ID_PARAMETER_NAME = "%sParameter".formatted(FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME);

    @Test
    public
    void testStack () {
        final App app = new App();

        final FamilyDirectoryLambdaStack stack = new FamilyDirectoryLambdaStack(app, FamilyDirectoryCdkApp.LAMBDA_STACK_NAME, StackProps.builder()
                                                                                                                                        .env(FamilyDirectoryCdkApp.DEFAULT_ENV)
                                                                                                                                        .stackName(FamilyDirectoryCdkApp.LAMBDA_STACK_NAME)
                                                                                                                                        .build());

        final Template template = Template.fromStack(stack);

        template.hasParameter(HOSTED_ZONE_ID_PARAMETER_NAME, objectLike(Map.of("Type", "AWS::SSM::Parameter::Value<String>", "Default", FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME)));

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
                                                                                                                                                                      LambdaFunctionConstructUtility.ARCHITECTURE.toString()), "Environment", singletonMap("Variables", Map.of(LambdaUtils.EnvVar.COGNITO_USER_POOL_ID.name(),
                                                                                                                                                                                                                                                                               singletonMap("Fn::ImportValue",
                                                                                                                                                                                                                                                                                            FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME),
                                                                                                                                                                                                                                                                               LambdaUtils.EnvVar.HOSTED_ZONE_NAME.name(),
                                                                                                                                                                                                                                                                               FamilyDirectoryDomainStack.HOSTED_ZONE_NAME,
                                                                                                                                                                                                                                                                               LambdaUtils.EnvVar.ROOT_ID.name(), LambdaFunctionConstructUtility.ROOT_ID,
                                                                                                                                                                                                                                                                               LambdaUtils.EnvVar.SES_EMAIL_IDENTITY_NAME.name(),
                                                                                                                                                                                                                                                                               singletonMap("Fn::ImportValue",
                                                                                                                                                                                                                                                                                            FamilyDirectorySesStack.SES_EMAIL_IDENTITY_NAME_EXPORT_NAME),
                                                                                                                                                                                                                                                                               LambdaUtils.EnvVar.S3_PDF_BUCKET_NAME.name(),
                                                                                                                                                                                                                                                                               singletonMap("Fn::ImportValue",
                                                                                                                                                                                                                                                                                            FamilyDirectorySssStack.S3_PDF_BUCKET_NAME_EXPORT_NAME))),
                                                                                                                                                              "Handler", function.handler(),
                                                                                                                                                              "MemorySize", function.memorySize(),
                                                                                                                                                              "Role", singletonMap("Fn::GetAtt",
                                                                                                                                                                                   List.of(functionRoleIdCapture.asString(),
                                                                                                                                                                                           "Arn")),
                                                                                                                                                              "Runtime",
                                                                                                                                                              LambdaFunctionConstructUtility.RUNTIME.toString(),
                                                                                                                                                              "Timeout", function.timeout()
                                                                                                                                                                                 .toSeconds()))));
            assertEquals(1, functionMap.size());
            assertTrue(functionMap.containsKey(functionIdCapture.asString()));
        }

        for (final StreamFunction function : StreamFunction.values()) {
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
                                                                                                                                                                                                                                                                               LambdaUtils.EnvVar.S3_PDF_BUCKET_NAME.name(),
                                                                                                                                                                                                                                                                               singletonMap("Fn::ImportValue",
                                                                                                                                                                                                                                                                                            FamilyDirectorySssStack.S3_PDF_BUCKET_NAME_EXPORT_NAME))),
                                                                                                                                                              "Handler", function.handler(),
                                                                                                                                                              "MemorySize", function.memorySize(),
                                                                                                                                                              "Role", singletonMap("Fn::GetAtt",
                                                                                                                                                                                   List.of(functionRoleIdCapture.asString(),
                                                                                                                                                                                           "Arn")),
                                                                                                                                                              "Runtime",
                                                                                                                                                              LambdaFunctionConstructUtility.RUNTIME.toString(),
                                                                                                                                                              "Timeout", function.timeout()
                                                                                                                                                                                 .toSeconds()))));
            assertEquals(1, functionMap.size());
            assertTrue(functionMap.containsKey(functionIdCapture.asString()));

            for (final DdbTable eventTable : function.streamEventSources()) {
                assertTrue(eventTable.hasStream());
                template.hasResourceProperties("AWS::Lambda::EventSourceMapping", objectLike(Map.ofEntries(entry("BatchSize", FamilyDirectoryLambdaStack.DDB_STREAM_BATCH_SIZE),
                                                                                                           entry("BisectBatchOnFunctionError",
                                                                                                                 FamilyDirectoryLambdaStack.DDB_STREAM_BISECT_BATCH_ON_ERROR),
                                                                                                           entry("Enabled", FamilyDirectoryLambdaStack.DDB_STREAM_ENABLED),
                                                                                                           entry("EventSourceArn", singletonMap("Fn::ImportValue", eventTable.streamArnExportName())),
                                                                                                           entry("FunctionName", singletonMap("Ref", functionIdCapture.asString())),
                                                                                                           entry("FunctionResponseTypes", absent()), entry("MaximumBatchingWindowInSeconds",
                                                                                                                                                           FamilyDirectoryLambdaStack.DDB_STREAM_MAX_BATCH_WINDOW.toSeconds()),
                                                                                                           entry("MaximumRecordAgeInSeconds",
                                                                                                                 FamilyDirectoryLambdaStack.DDB_STREAM_MAX_RECORD_AGE.toSeconds()),
                                                                                                           entry("MaximumRetryAttempts", FamilyDirectoryLambdaStack.DDB_STREAM_RETRY_ATTEMPTS),
                                                                                                           entry("ParallelizationFactor", FamilyDirectoryLambdaStack.DDB_STREAM_PARALLELIZATION_FACTOR),
                                                                                                           entry("StartingPosition", "LATEST"))));
            }
        }
    }
}
