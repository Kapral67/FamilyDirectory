package org.familydirectory.cdk.test.lambda;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.assets.lambda.function.api.enums.ApiFunction;
import org.familydirectory.assets.lambda.function.models.LambdaFunctionModel;
import org.familydirectory.assets.lambda.function.stream.enums.StreamFunction;
import org.familydirectory.assets.lambda.function.trigger.enums.TriggerFunction;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.familydirectory.cdk.lambda.FamilyDirectoryLambdaStack;
import org.familydirectory.cdk.lambda.construct.utility.LambdaFunctionConstructUtility;
import org.familydirectory.cdk.sss.FamilyDirectorySssStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Capture;
import software.amazon.awscdk.assertions.Template;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
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

            final Capture policyResourcesCapture = new Capture();
            final Map<String, Map<String, Object>> functionMap = template.findResources("AWS::Lambda::Function", objectLike(Map.of("Properties", Map.of("Architectures", singletonList(
                                                                                                                                                                LambdaFunctionConstructUtility.ARCHITECTURE.toString()), "Environment", singletonMap("Variables", Map.of(LambdaUtils.EnvVar.COGNITO_USER_POOL_ID.name(),
                                                                                                                                                                                                                                                                         singletonMap("Fn::ImportValue",
                                                                                                                                                                                                                                                                                      FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME),
                                                                                                                                                                                                                                                                         LambdaUtils.EnvVar.HOSTED_ZONE_NAME.name(),
                                                                                                                                                                                                                                                                         FamilyDirectoryDomainStack.HOSTED_ZONE_NAME,
                                                                                                                                                                                                                                                                         LambdaUtils.EnvVar.ROOT_ID.name(), LambdaFunctionConstructUtility.ROOT_ID,
                                                                                                                                                                                                                                                                         LambdaUtils.EnvVar.S3_PDF_BUCKET_NAME.name(),
                                                                                                                                                                                                                                                                         singletonMap("Fn::ImportValue",
                                                                                                                                                                                                                                                                                      FamilyDirectorySssStack.S3_PDF_BUCKET_NAME_EXPORT_NAME))),
                                                                                                                                                        "Handler", function.handler(), "MemorySize",
                                                                                                                                                        function.memorySize(), "Role",
                                                                                                                                                        singletonMap("Fn::GetAtt",
                                                                                                                                                                     List.of(functionRoleIdCapture.asString(),
                                                                                                                                                                             "Arn")), "Runtime",
                                                                                                                                                        LambdaFunctionConstructUtility.RUNTIME.toString(),
                                                                                                                                                        "Timeout", function.timeoutSeconds()),
                                                                                                                                   "DependsOn", policyResourcesCapture)));
            assertEquals(1, functionMap.size());
            assertTrue(functionMap.containsKey(functionIdCapture.asString()));

            final List<Object> policyResources = policyResourcesCapture.asArray()
                                                                       .stream()
                                                                       .filter(s -> !s.equals(functionRoleIdCapture.asString()))
                                                                       .toList();
            assertEquals(1, policyResources.size());
            final String defaultPolicyId = policyResources.getFirst()
                                                          .toString();
            final Capture statementsCapture = new Capture();
            template.hasResourceProperties("AWS::IAM::Policy", objectLike(Map.of("PolicyDocument", singletonMap("Statement", statementsCapture), "PolicyName", defaultPolicyId)));
            final List<Object> statements = statementsCapture.asArray();
            verifyFunctionPolicyStatements(function, statements);
        }

        for (final TriggerFunction function : TriggerFunction.values()) {
            final Capture statementsCapture = new Capture();
            template.hasResourceProperties("AWS::IAM::Policy", objectLike(Map.of("PolicyDocument", singletonMap("Statement", statementsCapture), "Roles", singletonList(singletonMap("Fn::Select",
                                                                                                                                                                                     List.of(1,
                                                                                                                                                                                             singletonMap(
                                                                                                                                                                                                     "Fn::Split",
                                                                                                                                                                                                     List.of("/",
                                                                                                                                                                                                             singletonMap(
                                                                                                                                                                                                                     "Fn::Select",
                                                                                                                                                                                                                     List.of(5,
                                                                                                                                                                                                                             singletonMap(
                                                                                                                                                                                                                                     "Fn::Split",
                                                                                                                                                                                                                                     List.of(":",
                                                                                                                                                                                                                                             singletonMap(
                                                                                                                                                                                                                                                     "Fn::ImportValue",
                                                                                                                                                                                                                                                     function.roleArnExportName())))))))))))));
            final List<Object> statements = statementsCapture.asArray();
            verifyFunctionPolicyStatements(function, statements);
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

            final Capture policyResourcesCapture = new Capture();
            final Map<String, Map<String, Object>> functionMap = template.findResources("AWS::Lambda::Function", objectLike(Map.of("Properties", Map.of("Architectures", singletonList(
                                                                                                                                                                LambdaFunctionConstructUtility.ARCHITECTURE.toString()), "Environment", singletonMap("Variables", Map.of(LambdaUtils.EnvVar.ROOT_ID.name(), LambdaFunctionConstructUtility.ROOT_ID,
                                                                                                                                                                                                                                                                         LambdaUtils.EnvVar.S3_PDF_BUCKET_NAME.name(),
                                                                                                                                                                                                                                                                         singletonMap("Fn::ImportValue",
                                                                                                                                                                                                                                                                                      FamilyDirectorySssStack.S3_PDF_BUCKET_NAME_EXPORT_NAME))),
                                                                                                                                                        "Handler", function.handler(), "MemorySize",
                                                                                                                                                        function.memorySize(), "Role",
                                                                                                                                                        singletonMap("Fn::GetAtt",
                                                                                                                                                                     List.of(functionRoleIdCapture.asString(),
                                                                                                                                                                             "Arn")), "Runtime",
                                                                                                                                                        LambdaFunctionConstructUtility.RUNTIME.toString(),
                                                                                                                                                        "Timeout", function.timeoutSeconds()),
                                                                                                                                   "DependsOn", policyResourcesCapture)));
            assertEquals(1, functionMap.size());
            assertTrue(functionMap.containsKey(functionIdCapture.asString()));

            final List<Object> policyResources = policyResourcesCapture.asArray()
                                                                       .stream()
                                                                       .filter(s -> !s.equals(functionRoleIdCapture.asString()))
                                                                       .toList();
            assertEquals(1, policyResources.size());
            final String defaultPolicyId = policyResources.getFirst()
                                                          .toString();
            final Capture statementsCapture = new Capture();
            template.hasResourceProperties("AWS::IAM::Policy", objectLike(Map.of("PolicyDocument", singletonMap("Statement", statementsCapture), "PolicyName", defaultPolicyId)));
            final List<Object> statements = statementsCapture.asArray();
            verifyFunctionPolicyStatements(function, statements);
            verifyStreamFunctionPolicyStatements(function, statements);

            for (final DdbTable eventTable : function.streamEventSources()) {
                assertTrue(eventTable.hasStream());
                template.hasResourceProperties("AWS::Lambda::EventSourceMapping", objectLike(Map.ofEntries(entry("BatchSize", FamilyDirectoryLambdaStack.DDB_STREAM_BATCH_SIZE),
                                                                                                           entry("BisectBatchOnFunctionError",
                                                                                                                 FamilyDirectoryLambdaStack.DDB_STREAM_BISECT_BATCH_ON_ERROR),
                                                                                                           entry("Enabled", FamilyDirectoryLambdaStack.DDB_STREAM_ENABLED),
                                                                                                           entry("EventSourceArn", singletonMap("Fn::ImportValue", eventTable.streamArnExportName())),
                                                                                                           entry("FunctionName", singletonMap("Ref", functionIdCapture.asString())),
                                                                                                           entry("FunctionResponseTypes", absent()),
                                                                                                           entry("MaximumRecordAgeInSeconds", DdbUtils.DDB_STREAM_MAX_RECORD_AGE_SECONDS),
                                                                                                           entry("MaximumRetryAttempts", FamilyDirectoryLambdaStack.DDB_STREAM_RETRY_ATTEMPTS),
                                                                                                           entry("ParallelizationFactor", FamilyDirectoryLambdaStack.DDB_STREAM_PARALLELIZATION_FACTOR),
                                                                                                           entry("StartingPosition", "LATEST"))));
            }
        }
    }

    private static
    void verifyStreamFunctionPolicyStatements (final @NotNull StreamFunction function, final @NotNull List<Object> statements) {
        final String failMessage = "Function: %s | DDB_STREAM | Actions: %s".formatted(function.functionName(), FamilyDirectoryLambdaStack.DDB_STREAM_POLICY_ACTIONS.toString());
        boolean fail = true;
        for (final Object o : statements) {
            try {
                @SuppressWarnings("unchecked")
                final Map<String, Object> statement = (Map<String, Object>) o;
                if (statement.get("Effect")
                             .equals("Allow") && statement.get("Action")
                                                          .equals(FamilyDirectoryLambdaStack.DDB_STREAM_POLICY_ACTIONS))
                {
                    final Object resourceObj = statement.get("Resource");
                    final List<Map<String, String>> resourceMap = new ArrayList<>(1);
                    function.streamEventSources()
                            .forEach(src -> resourceMap.add(Map.of("Fn::ImportValue", requireNonNull(src.streamArnExportName()))));
                    if (resourceMap.size() == 1 && resourceObj.equals(resourceMap.getFirst())) {
                        fail = false;
                        break;
                    } else if (resourceObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        final List<Map<String, String>> resource = (List<Map<String, String>>) resourceObj;
                        if (resource.containsAll(resourceMap)) {
                            fail = false;
                            break;
                        }
                    }
                }
            } catch (final ClassCastException e) {
                fail(failMessage, e);
            }
        }
        if (fail) {
            fail(failMessage);
        }
    }

    private static
    void verifyFunctionPolicyStatements (final @NotNull LambdaFunctionModel function, final @NotNull List<Object> statements) {
        ofNullable(function.ddbActions()).ifPresent(map -> map.forEach((key, value) -> {
            final Map<String, Object> keyResource = singletonMap("Fn::ImportValue", key.arnExportName());
            final String failMessage = "Function: %s | DdbTable: %s | Actions: %s".formatted(function.functionName(), key.name(), value.toString());
            boolean fail = true;
            for (final Object o : statements) {
                try {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> statement = (Map<String, Object>) o;
                    final Object resourceObj = statement.get("Resource");
                    if (statement.get("Effect")
                                 .equals("Allow") && resourceObj instanceof List)
                    {
                        @SuppressWarnings("unchecked")
                        final List<Map<String, Object>> resource = (List<Map<String, Object>>) resourceObj;
                        if (resource.contains(keyResource) && resource.contains(singletonMap("Fn::Join", List.of("", List.of(keyResource, "/index/*"))))) {
                            final Object actionsObj = statement.get("Action");
                            if (actionsObj instanceof String && value.size() == 1 && actionsObj.equals(value.getFirst())) {
                                fail = false;
                                break;
                            } else if (actionsObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                final List<String> actions = (List<String>) actionsObj;
                                if (actions.containsAll(value)) {
                                    fail = false;
                                    break;
                                }
                            }
                        }
                    }
                } catch (final ClassCastException e) {
                    fail(failMessage, e);
                }
            }
            if (fail) {
                fail(failMessage);
            }
        }));
        ofNullable(function.cognitoActions()).ifPresent(cognitoActions -> {
            final String cognitoArnSuffix = ":cognito-idp:%s:%s:userpool/".formatted(FamilyDirectoryCdkApp.DEFAULT_REGION, FamilyDirectoryCdkApp.DEFAULT_ACCOUNT);
            final String failMessage = "Function: %s | COGNITO | Actions: %s".formatted(function.functionName(), cognitoActions.toString());
            boolean fail = true;
            for (final Object o : statements) {
                try {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> statement = (Map<String, Object>) o;
                    final Object resourceObj = statement.get("Resource");
                    if (statement.get("Effect")
                                 .equals("Allow") && resourceObj instanceof Map)
                    {
                        @SuppressWarnings("unchecked")
                        final List<Object> resourceFnJoin = (List<Object>) ((List<Object>) ((Map<String, Object>) resourceObj).get("Fn::Join")).get(1);
                        if ((resourceFnJoin.contains("arn:aws:%s".formatted(cognitoArnSuffix)) ||
                             resourceFnJoin.containsAll(List.of("arn:", singletonMap("Ref", "AWS::Partition"), cognitoArnSuffix))) &&
                            resourceFnJoin.contains(singletonMap("Fn::ImportValue", FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME)))
                        {
                            final Object actionsObj = statement.get("Action");
                            if (actionsObj instanceof String && cognitoActions.size() == 1 && actionsObj.equals(cognitoActions.getFirst())) {
                                fail = false;
                                break;
                            } else if (actionsObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                final List<String> actions = (List<String>) actionsObj;
                                if (actions.containsAll(cognitoActions)) {
                                    fail = false;
                                    break;
                                }
                            }
                        }
                    }
                } catch (final ClassCastException e) {
                    fail(failMessage, e);
                }
            }
            if (fail) {
                fail(failMessage);
            }
        });
        ofNullable(function.sesActions()).ifPresent(sesActions -> {
            final String failMessage = "Function: %s | SES | Actions: %s".formatted(function.functionName(), sesActions.toString());
            boolean fail = true;
            for (final Object o : statements) {
                try {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> statement = (Map<String, Object>) o;
                    if (statement.get("Effect")
                                 .equals("Allow") && statement.get("Resource")
                                                              .equals(FamilyDirectoryCdkApp.GLOBAL_RESOURCE))
                    {
                        final Object actionsObj = statement.get("Action");
                        if (actionsObj instanceof String && sesActions.size() == 1 && actionsObj.equals(sesActions.getFirst())) {
                            fail = false;
                            break;
                        } else if (actionsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            final List<String> actions = (List<String>) actionsObj;
                            if (actions.containsAll(sesActions)) {
                                fail = false;
                                break;
                            }
                        }
                    }
                } catch (final ClassCastException e) {
                    fail(failMessage, e);
                }
            }
            if (fail) {
                fail(failMessage);
            }
        });
        ofNullable(function.sssActions()).ifPresent(sssActions -> {
            final String failMessage = "Function: %s | S3 | Actions: %s".formatted(function.functionName(), sssActions.toString());
            boolean fail = true;
            for (final Object o : statements) {
                try {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> statement = (Map<String, Object>) o;
                    if (statement.get("Effect")
                                 .equals("Allow") && statement.get("Resource")
                                                              .equals(singletonMap("Fn::Join",
                                                                                   List.of("", List.of(singletonMap("Fn::ImportValue", FamilyDirectorySssStack.S3_PDF_BUCKET_ARN_EXPORT_NAME), "/*")))))
                    {
                        final Object actionsObj = statement.get("Action");
                        if (actionsObj instanceof String && sssActions.size() == 1 && actionsObj.equals(sssActions.getFirst())) {
                            fail = false;
                            break;
                        } else if (actionsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            final List<String> actions = (List<String>) actionsObj;
                            if (actions.containsAll(sssActions)) {
                                fail = false;
                                break;
                            }
                        }
                    }
                } catch (final ClassCastException e) {
                    fail(failMessage, e);
                }
            }
            if (fail) {
                fail(failMessage);
            }
        });
    }
}
