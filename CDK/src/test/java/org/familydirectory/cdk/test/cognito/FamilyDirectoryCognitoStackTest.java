package org.familydirectory.cdk.test.cognito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.familydirectory.assets.lambda.function.trigger.enums.TriggerFunction;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoUsEastOneStack;
import org.familydirectory.cdk.customresource.SSMParameterReader;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.familydirectory.cdk.lambda.construct.utility.LambdaFunctionConstructUtility;
import org.familydirectory.cdk.ses.FamilyDirectorySesStack;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Capture;
import software.amazon.awscdk.assertions.Match;
import software.amazon.awscdk.assertions.Template;
import software.amazon.awscdk.services.cognito.UserPoolClientIdentityProvider;
import software.amazon.awssdk.regions.Region;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awscdk.assertions.Match.objectEquals;
import static software.amazon.awscdk.assertions.Match.objectLike;

public
class FamilyDirectoryCognitoStackTest {

    private static final String HOSTED_ZONE_ID_PARAMETER_NAME = "%sParameter".formatted(FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME);
    private static final String COGNITO_CERTIFICATE_ARN_PARAMETER_NAME = "%sParameter".formatted(FamilyDirectoryCognitoUsEastOneStack.COGNITO_CERTIFICATE_ARN_PARAMETER_NAME);
    private static final String FULL_COGNITO_DOMAIN_NAME = "%s.".formatted(FamilyDirectoryCognitoStack.COGNITO_DOMAIN_NAME);

    @Test
    public
    void testStack () {
        final App app = new App();

        final FamilyDirectoryCognitoStack stack = new FamilyDirectoryCognitoStack(app, FamilyDirectoryCdkApp.COGNITO_STACK_NAME, StackProps.builder()
                                                                                                                                           .env(FamilyDirectoryCdkApp.DEFAULT_ENV)
                                                                                                                                           .stackName(FamilyDirectoryCdkApp.COGNITO_STACK_NAME)
                                                                                                                                           .build());

        final Template template = Template.fromStack(stack);

        template.hasParameter(HOSTED_ZONE_ID_PARAMETER_NAME, objectEquals(Map.of("Type", "AWS::SSM::Parameter::Value<String>", "Default", FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME)));

        final Map<TriggerFunction, String> triggerIdMap = new HashMap<>();
        final Map<TriggerFunction, String> triggerRoleIdMap = new HashMap<>();
        for (final TriggerFunction trigger : TriggerFunction.values()) {
            final Capture triggerArnCapture = new Capture();
            template.hasOutput(trigger.arnExportName(),
                               objectLike(Map.of("Value", singletonMap("Fn::GetAtt", List.of(triggerArnCapture, "Arn")), "Export", singletonMap("Name", trigger.arnExportName()))));
            assertFalse(triggerArnCapture.asString()
                                         .isBlank());
            triggerIdMap.put(trigger, triggerArnCapture.asString());
            final Capture triggerRoleArnCapture = new Capture();
            template.hasOutput(trigger.roleArnExportName(),
                               objectLike(Map.of("Value", singletonMap("Fn::GetAtt", List.of(triggerRoleArnCapture, "Arn")), "Export", singletonMap("Name", trigger.roleArnExportName()))));
            assertFalse(triggerRoleArnCapture.asString()
                                             .isBlank());
            triggerRoleIdMap.put(trigger, triggerRoleArnCapture.asString());
        }
        final Capture userPoolIdCapture = new Capture();
        template.hasOutput(FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME,
                           objectLike(Map.of("Value", singletonMap("Ref", userPoolIdCapture), "Export", singletonMap("Name", FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME))));
        assertFalse(userPoolIdCapture.asString()
                                     .isBlank());
        final Capture userPoolClientIdCapture = new Capture();
        template.hasOutput(FamilyDirectoryCognitoStack.COGNITO_USER_POOL_CLIENT_ID_EXPORT_NAME, objectLike(
                Map.of("Value", singletonMap("Ref", userPoolClientIdCapture), "Export", singletonMap("Name", FamilyDirectoryCognitoStack.COGNITO_USER_POOL_CLIENT_ID_EXPORT_NAME))));
        final Capture userPoolDomainNameCapture = new Capture();
        template.hasOutput(FamilyDirectoryCognitoStack.COGNITO_SIGN_IN_URL_EXPORT_NAME, objectLike(Map.of("Value", singletonMap("Fn::Join", List.of("", List.of(FamilyDirectoryCdkApp.HTTPS_PREFIX,
                                                                                                                                                                singletonMap("Ref",
                                                                                                                                                                             userPoolDomainNameCapture),
                                                                                                                                                                "/login" + "?client_id=",
                                                                                                                                                                singletonMap("Ref",
                                                                                                                                                                             userPoolClientIdCapture.asString()),
                                                                                                                                                                "&response_type=code&redirect_uri=%s".formatted(
                                                                                                                                                                        FamilyDirectoryCdkApp.HTTPS_PREFIX +
                                                                                                                                                                        FamilyDirectoryDomainStack.HOSTED_ZONE_NAME)))),
                                                                                                          "Export",
                                                                                                          singletonMap("Name", FamilyDirectoryCognitoStack.COGNITO_SIGN_IN_URL_EXPORT_NAME))));

        for (final TriggerFunction trigger : TriggerFunction.values()) {
            template.hasResourceProperties("AWS::Lambda::Function", objectLike(Map.of("Architectures", singletonList(LambdaFunctionConstructUtility.ARCHITECTURE.toString()), "Environment",
                                                                                      singletonMap("Variables", Map.of(LambdaUtils.EnvVar.ROOT_ID.name(), LambdaFunctionConstructUtility.ROOT_ID,
                                                                                                                       LambdaUtils.EnvVar.HOSTED_ZONE_NAME.name(),
                                                                                                                       FamilyDirectoryDomainStack.HOSTED_ZONE_NAME)), "Handler", trigger.handler(),
                                                                                      "MemorySize", trigger.memorySize(), "Role",
                                                                                      singletonMap("Fn::GetAtt", List.of(triggerRoleIdMap.get(trigger), "Arn")), "Runtime",
                                                                                      LambdaFunctionConstructUtility.RUNTIME.toString(), "Timeout", trigger.timeoutSeconds())));
        }

        final Capture sesArnCapture = new Capture();
        template.hasResourceProperties("AWS::Cognito::UserPool", objectLike(
                Map.ofEntries(entry("AccountRecoverySetting", singletonMap("RecoveryMechanisms", singletonList(Map.of("Name", "verified_email", "Priority", 1)))),
                              entry("AdminCreateUserConfig", singletonMap("AllowAdminCreateUserOnly", !FamilyDirectoryCognitoStack.COGNITO_SELF_SIGN_UP_ENABLED)),
                              entry("AutoVerifiedAttributes", singletonList("email")), entry("DeletionProtection", "ACTIVE"), entry("EmailConfiguration", Map.of("ConfigurationSet",
                                                                                                                                                                 FamilyDirectorySesStack.SES_CONFIGURATION_SET_NAME,
                                                                                                                                                                 "From",
                                                                                                                                                                 FamilyDirectoryCognitoStack.COGNITO_FROM_EMAIL_ADDRESS,
                                                                                                                                                                 "SourceArn", sesArnCapture)),
                              entry("LambdaConfig", Map.of("PostConfirmation", singletonMap("Fn::GetAtt", List.of(triggerIdMap.get(TriggerFunction.POST_CONFIRMATION), "Arn")), "PreSignUp",
                                                           singletonMap("Fn::GetAtt", List.of(triggerIdMap.get(TriggerFunction.PRE_SIGN_UP), "Arn")))), entry("MfaConfiguration", "OFF"),
                              entry("Policies", singletonMap("PasswordPolicy", Map.of("MinimumLength", FamilyDirectoryCognitoStack.COGNITO_MIN_PASSWORD_LENGTH, "RequireLowercase",
                                                                                      FamilyDirectoryCognitoStack.COGNITO_REQUIRE_LOWERCASE_IN_PASSWORD, "RequireNumbers",
                                                                                      FamilyDirectoryCognitoStack.COGNITO_REQUIRE_DIGITS_IN_PASSWORD, "RequireSymbols",
                                                                                      FamilyDirectoryCognitoStack.COGNITO_REQUIRE_SYMBOLS_IN_PASSWORD, "TemporaryPasswordValidityDays",
                                                                                      FamilyDirectoryCognitoStack.COGNITO_TEMPORARY_PASSWORD_VALIDITY_DAYS))), entry("Schema", singletonList(
                                Map.of("Mutable", FamilyDirectoryCognitoStack.COGNITO_EMAIL_MUTABLE_ATTRIBUTE, "Name", "email", "Required",
                                       FamilyDirectoryCognitoStack.COGNITO_EMAIL_REQUIRE_ATTRIBUTE))),
                              entry("UserAttributeUpdateSettings", singletonMap("AttributesRequireVerificationBeforeUpdate", singletonList("email"))),
                              entry("UserPoolAddOns", singletonMap("AdvancedSecurityMode", "OFF")), entry("UsernameAttributes", singletonList("email")),
                              entry("UsernameConfiguration", singletonMap("CaseSensitive", FamilyDirectoryCognitoStack.COGNITO_SIGN_IN_CASE_SENSITIVE)), entry("VerificationMessageTemplate",
                                                                                                                                                               Map.of("DefaultEmailOption",
                                                                                                                                                                      "CONFIRM_WITH_LINK",
                                                                                                                                                                      "EmailMessageByLink",
                                                                                                                                                                      "Verify your account by" +
                                                                                                                                                                      " clicking on {##Verify" +
                                                                                                                                                                      " Email##}", "EmailSubjectByLink",
                                                                                                                                                                      "Verify your new " +
                                                                                                                                                                      "account")))));
        assertTrue(sesArnCapture.asObject()
                                .toString()
                                .contains("ses:%s:%s:identity/%s".formatted(FamilyDirectoryCdkApp.DEFAULT_REGION, FamilyDirectoryCdkApp.DEFAULT_ACCOUNT, FamilyDirectoryDomainStack.HOSTED_ZONE_NAME)));

        for (final TriggerFunction trigger : TriggerFunction.values()) {
            template.hasResourceProperties("AWS::Lambda::Permission", objectLike(
                    Map.of("Action", "lambda:InvokeFunction", "FunctionName", singletonMap("Fn::GetAtt", List.of(triggerIdMap.get(trigger), "Arn")), "Principal", "cognito-idp.amazonaws.com",
                           "SourceArn", singletonMap("Fn::GetAtt", List.of(userPoolIdCapture.asString(), "Arn")))));
        }

        template.hasResourceProperties("AWS::Cognito::UserPoolClient", objectLike(Map.ofEntries(entry("AllowedOAuthFlows", singletonList("code")), entry("AllowedOAuthFlowsUserPoolClient", true),
                                                                                                entry("AllowedOAuthScopes", List.of("email", "openid", "aws.cognito.signin.user.admin", "profile")),
                                                                                                entry("CallbackURLs",
                                                                                                      singletonList(FamilyDirectoryCdkApp.HTTPS_PREFIX + FamilyDirectoryDomainStack.HOSTED_ZONE_NAME)),
                                                                                                entry("ExplicitAuthFlows", List.of("ALLOW_USER_SRP_AUTH", "ALLOW_REFRESH_TOKEN_AUTH")),
                                                                                                entry("LogoutURLs",
                                                                                                      singletonList(FamilyDirectoryCdkApp.HTTPS_PREFIX + FamilyDirectoryDomainStack.HOSTED_ZONE_NAME)),
                                                                                                entry("GenerateSecret", FamilyDirectoryCognitoStack.COGNITO_USER_POOL_CLIENT_GENERATE_SECRET),
                                                                                                entry("PreventUserExistenceErrors", "ENABLED"),
                                                                                                entry("ReadAttributes", List.of("email", "email_verified")), entry("SupportedIdentityProviders",
                                                                                                                                                                   FamilyDirectoryCognitoStack.COGNITO_USER_POOL_CLIENT_IDENTITY_PROVIDERS.stream()
                                                                                                                                                                                                                                          .map(UserPoolClientIdentityProvider::getName)
                                                                                                                                                                                                                                          .toList()),
                                                                                                entry("UserPoolId", singletonMap("Ref", userPoolIdCapture.asString())),
                                                                                                entry("WriteAttributes", singletonList("email")))));

        final Map<String, Map<String, List<Map<String, String>>>> ssmReaderIamPolicy = singletonMap("PolicyDocument", singletonMap("Statement", singletonList(
                Map.of("Action", "ssm:GetParameter", "Effect", "Allow", "Resource", "*"))));
        final Map<String, List<Object>> userPoolCloudFrontDomainNamePolicy = singletonMap("Fn::Join", List.of("", List.of("{\"service" + "\":\"CognitoIdentityServiceProvider\"," +
                                                                                                                          "\"action\":\"describeUserPoolDomain\"," + "\"parameters\":{\"Domain\":\"",
                                                                                                                          singletonMap("Ref", userPoolDomainNameCapture.asString()),
                                                                                                                          "\"}," + "\"physicalResourceId\":{\"id\":\"",
                                                                                                                          singletonMap("Ref", userPoolDomainNameCapture.asString()), "\"}}")));
        final Capture cognitoCertificateArnParameterId = new Capture();
        final Map<String, Map<String, Object>> userPoolCloudFrontDomainNameMap = template.findResources("Custom::UserPoolCloudFrontDomainName", objectLike(singletonMap("Properties",
                                                                                                                                                                        Map.of("ServiceToken",
                                                                                                                                                                               singletonMap(
                                                                                                                                                                                       "Fn::GetAtt",
                                                                                                                                                                                       List.of(cognitoCertificateArnParameterId,
                                                                                                                                                                                               "Arn")),
                                                                                                                                                                               "Create",
                                                                                                                                                                               userPoolCloudFrontDomainNamePolicy,
                                                                                                                                                                               "Update",
                                                                                                                                                                               userPoolCloudFrontDomainNamePolicy))));
        if (FamilyDirectoryCdkApp.DEFAULT_REGION.equals(FamilyDirectoryCognitoUsEastOneStack.REGION)) {
            assertTrue(template.findResources("Custom::AWS", objectLike(singletonMap("Properties", Map.of("Create", Match.anyValue(), "Update", Match.anyValue()))))
                               .isEmpty());
            assertTrue(template.findResources("AWS::IAM::Policy", objectLike(singletonMap("Properties", ssmReaderIamPolicy)))
                               .isEmpty());
            template.hasParameter(COGNITO_CERTIFICATE_ARN_PARAMETER_NAME,
                                  objectEquals(Map.of("Type", "AWS::SSM::Parameter::Value<String>", "Default", FamilyDirectoryCognitoUsEastOneStack.COGNITO_CERTIFICATE_ARN_PARAMETER_NAME)));
            template.hasResourceProperties("AWS::Cognito::UserPoolDomain", objectLike(
                    Map.of("CustomDomainConfig", singletonMap("CertificateArn", singletonMap("Ref", COGNITO_CERTIFICATE_ARN_PARAMETER_NAME)), "Domain", FamilyDirectoryCognitoStack.COGNITO_DOMAIN_NAME,
                           "UserPoolId", singletonMap("Ref", userPoolIdCapture.asString()))));
        } else {
            final Capture ssmReaderCreateCapture = new Capture();
            final Capture ssmReaderUpdateCapture = new Capture();
            final Map<String, Map<String, Object>> customResourceMap = template.findResources("Custom::AWS", objectLike(singletonMap("Properties", Map.of("ServiceToken", singletonMap("Fn::GetAtt",
                                                                                                                                                                                       List.of(cognitoCertificateArnParameterId.asString(),
                                                                                                                                                                                               "Arn")),
                                                                                                                                                          "Create", ssmReaderCreateCapture, "Update",
                                                                                                                                                          ssmReaderUpdateCapture))));
            assertEquals(1, customResourceMap.size());
            assertTrue(SSMParameterReader.validateTemplate(ssmReaderCreateCapture.asString(), FamilyDirectoryCognitoUsEastOneStack.COGNITO_CERTIFICATE_ARN_PARAMETER_NAME,
                                                           Region.of(FamilyDirectoryCognitoUsEastOneStack.REGION)));
            assertTrue(SSMParameterReader.validateTemplate(ssmReaderUpdateCapture.asString(), FamilyDirectoryCognitoUsEastOneStack.COGNITO_CERTIFICATE_ARN_PARAMETER_NAME,
                                                           Region.of(FamilyDirectoryCognitoUsEastOneStack.REGION)));
            final String ssmParameterReaderId = customResourceMap.entrySet()
                                                                 .iterator()
                                                                 .next()
                                                                 .getKey();
            template.hasResourceProperties("AWS::IAM::Policy", objectLike(ssmReaderIamPolicy));
            template.hasResourceProperties("AWS::Cognito::UserPoolDomain", objectLike(
                    Map.of("CustomDomainConfig", singletonMap("CertificateArn", singletonMap("Fn::GetAtt", List.of(ssmParameterReaderId, SSMParameterReader.SSM_PARAMETER_READER_DATA_PATH))), "Domain",
                           FamilyDirectoryCognitoStack.COGNITO_DOMAIN_NAME, "UserPoolId", singletonMap("Ref", userPoolIdCapture.asString()))));
        }
        assertEquals(1, userPoolCloudFrontDomainNameMap.size());

        final String userPoolCloudFrontDomainNameId = userPoolCloudFrontDomainNameMap.entrySet()
                                                                                     .iterator()
                                                                                     .next()
                                                                                     .getKey();
        template.hasResourceProperties("AWS::Route53::RecordSet", objectLike(
                Map.of("AliasTarget", Map.of("DNSName", singletonMap("Fn::GetAtt", List.of(userPoolCloudFrontDomainNameId, "DomainDescription.CloudFrontDistribution"))), "HostedZoneId",
                       singletonMap("Ref", HOSTED_ZONE_ID_PARAMETER_NAME), "Name", FULL_COGNITO_DOMAIN_NAME, "Type", "A")));
    }
}
