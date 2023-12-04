package org.familydirectory.cdk.test.amplify;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.amplify.FamilyDirectoryAmplifyStack;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Capture;
import software.amazon.awscdk.assertions.Template;
import software.amazon.awscdk.services.amplify.alpha.CustomRule;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awscdk.assertions.Match.objectLike;

public
class FamilyDirectoryAmplifyStackTest {

    private static final String TABLE = "\"TableName\":\"MEMBER\"";
    private static final String KEY = "\"Key\":\\{\"id\":\\{\"S\":\"%s\"\\}\\}".formatted(DdbUtils.ROOT_MEMBER_ID);

    private static final Pattern GET_ROOT_MEMBER_REGEX = Pattern.compile(
            "\\{\"action\":\"GetItem\",\"service\":\"dynamodb\",\"parameters\":\\{(%s|%s),(%s|%s)\\},\"physicalResourceId\":\\{\"id\":\"\\d+\"\\},\"region\":\"%s\"\\}".formatted(TABLE, KEY, KEY,
                                                                                                                                                                                  TABLE,
                                                                                                                                                                                  FamilyDirectoryCdkApp.DEFAULT_REGION));

    @Test
    public
    void testStack () {
        final App app = new App();
        final FamilyDirectoryAmplifyStack stack = new FamilyDirectoryAmplifyStack(app, FamilyDirectoryCdkApp.AMPLIFY_STACK_NAME, StackProps.builder()
                                                                                                                                           .env(FamilyDirectoryCdkApp.DEFAULT_ENV)
                                                                                                                                           .stackName(FamilyDirectoryCdkApp.AMPLIFY_STACK_NAME)
                                                                                                                                           .build());
        final Template template = Template.fromStack(stack);

        final Map<String, Map<String, Object>> rootMemberSurnameResourcePolicyMap = template.findResources("AWS::IAM::Policy", singletonMap("Properties", singletonMap("PolicyDocument",
                                                                                                                                                                       singletonMap("Statement",
                                                                                                                                                                                    singletonList(
                                                                                                                                                                                            Map.of("Action",
                                                                                                                                                                                                   FamilyDirectoryAmplifyStack.AMPLIFY_ROOT_MEMBER_SURNAME_RESOURCE_POLICY_STATEMENT_ACTIONS.iterator()
                                                                                                                                                                                                                                                                                            .next(),
                                                                                                                                                                                                   "Effect",
                                                                                                                                                                                                   "Allow",
                                                                                                                                                                                                   "Resource",
                                                                                                                                                                                                   singletonMap(
                                                                                                                                                                                                           "Fn::ImportValue",
                                                                                                                                                                                                           FamilyDirectoryAmplifyStack.AMPLIFY_ROOT_MEMBER_SURNAME_RESOURCE_POLICY_STATEMENT_RESOURCE.arnExportName())))))));
        assertEquals(1, rootMemberSurnameResourcePolicyMap.size());
        final String rootMemberSurnameResourcePolicyId = rootMemberSurnameResourcePolicyMap.entrySet()
                                                                                           .iterator()
                                                                                           .next()
                                                                                           .getKey();

        final Capture rootMemberSurnameResourceCreateActionCapture = new Capture();
        final Capture rootMemberSurnameResourceUpdateActionCapture = new Capture();
        final Map<String, Map<String, Object>> rootMemberSurnameResourceMap = template.findResources("Custom::AWS", objectLike(
                Map.of("Properties", Map.of("Create", rootMemberSurnameResourceCreateActionCapture, "Update", rootMemberSurnameResourceUpdateActionCapture), "DependsOn",
                       singletonList(rootMemberSurnameResourcePolicyId))));
        assertEquals(1, rootMemberSurnameResourceMap.size());
        assertTrue(GET_ROOT_MEMBER_REGEX.matcher(rootMemberSurnameResourceCreateActionCapture.asString())
                                        .matches());
        assertTrue(GET_ROOT_MEMBER_REGEX.matcher(rootMemberSurnameResourceUpdateActionCapture.asString())
                                        .matches());
        final String rootMemberSurnameResourceId = rootMemberSurnameResourceMap.entrySet()
                                                                               .iterator()
                                                                               .next()
                                                                               .getKey();

        assertEquals(1, FamilyDirectoryAmplifyStack.AMPLIFY_CUSTOM_RULES.size());
        final CustomRule amplifyCustomRule = FamilyDirectoryAmplifyStack.AMPLIFY_CUSTOM_RULES.iterator()
                                                                                             .next();
        final Capture spaEnvironmentVariablesCapture = new Capture();
        final Map<String, Map<String, Object>> spaMap = template.findResources("AWS::Amplify::App", objectLike(singletonMap("Properties",
                                                                                                                            Map.of("BasicAuthConfig", singletonMap("EnableBasicAuth", false),
                                                                                                                                   "CustomRules", singletonList(
                                                                                                                                            Map.of("Source", amplifyCustomRule.getSource(), "Status",
                                                                                                                                                   "200", "Target", amplifyCustomRule.getTarget())),
                                                                                                                                   "EnableBranchAutoDeletion",
                                                                                                                                   FamilyDirectoryAmplifyStack.AMPLIFY_APP_AUTO_BRANCH_DELETE,
                                                                                                                                   "EnvironmentVariables", spaEnvironmentVariablesCapture, "Name",
                                                                                                                                   FamilyDirectoryAmplifyStack.AMPLIFY_APP_RESOURCE_ID, "OauthToken",
                                                                                                                                   FamilyDirectoryAmplifyStack.AMPLIFY_REPOSITORY_OAUTH_TOKEN,
                                                                                                                                   "Platform", FamilyDirectoryAmplifyStack.AMPLIFY_PLATFORM.name(),
                                                                                                                                   "Repository", "https://github.com/%s/%s".formatted(
                                                                                                                                            FamilyDirectoryAmplifyStack.AMPLIFY_REPOSITORY_OWNER,
                                                                                                                                            FamilyDirectoryAmplifyStack.AMPLIFY_REPOSITORY_NAME)))));
        assertEquals(1, spaMap.size());
        final List<Object> spaEnvironmentVariableList = spaEnvironmentVariablesCapture.asArray();
        assertEquals(FamilyDirectoryAmplifyStack.ReactEnvVar.values().length, spaEnvironmentVariableList.size());
        assertTrue(spaEnvironmentVariableList.containsAll(List.of(Map.of("Name", FamilyDirectoryAmplifyStack.ReactEnvVar.CLIENT_ID.toString(), "Value",
                                                                         singletonMap("Fn::ImportValue", FamilyDirectoryCognitoStack.COGNITO_USER_POOL_CLIENT_ID_EXPORT_NAME)),
                                                                  Map.of("Name", FamilyDirectoryAmplifyStack.ReactEnvVar.AGE_OF_MAJORITY.toString(), "Value", String.valueOf(DdbUtils.AGE_OF_MAJORITY)),
                                                                  Map.of("Name", FamilyDirectoryAmplifyStack.ReactEnvVar.SURNAME.toString(), "Value",
                                                                         singletonMap("Fn::GetAtt", List.of(rootMemberSurnameResourceId, FamilyDirectoryAmplifyStack.AMPLIFY_SURNAME_FIELD))),
                                                                  Map.of("Name", FamilyDirectoryAmplifyStack.ReactEnvVar.AUTH_DOMAIN.toString(), "Value",
                                                                         FamilyDirectoryCognitoStack.COGNITO_DOMAIN_NAME),
                                                                  Map.of("Name", FamilyDirectoryAmplifyStack.ReactEnvVar.USER_POOL_ID.toString(), "Value",
                                                                         singletonMap("Fn::ImportValue", FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME)),
                                                                  Map.of("Name", FamilyDirectoryAmplifyStack.ReactEnvVar.API_DOMAIN.toString(), "Value",
                                                                         FamilyDirectoryAmplifyStack.REACT_APP_API_DOMAIN),
                                                                  Map.of("Name", FamilyDirectoryAmplifyStack.ReactEnvVar.REDIRECT_URI.toString(), "Value",
                                                                         FamilyDirectoryAmplifyStack.REACT_APP_REDIRECT_URI),
                                                                  Map.of("Name", FamilyDirectoryAmplifyStack.ReactEnvVar.AWS_REGION.toString(), "Value", FamilyDirectoryCdkApp.DEFAULT_REGION))));
        final String spaId = spaMap.entrySet()
                                   .iterator()
                                   .next()
                                   .getKey();

        final Map<String, Map<String, Object>> spaBranchMap = template.findResources("AWS::Amplify::Branch", objectLike(singletonMap("Properties", Map.of("AppId", singletonMap("Fn::GetAtt",
                                                                                                                                                                                List.of(spaId,
                                                                                                                                                                                        "AppId")),
                                                                                                                                                          "BranchName",
                                                                                                                                                          FamilyDirectoryAmplifyStack.AMPLIFY_ROOT_BRANCH_NAME,
                                                                                                                                                          "EnableAutoBuild", true,
                                                                                                                                                          "EnablePullRequestPreview",
                                                                                                                                                          FamilyDirectoryAmplifyStack.AMPLIFY_ROOT_BRANCH_PULL_REQUEST_PREVIEW))));
        assertEquals(1, spaBranchMap.size());
        final String spaBranchId = spaBranchMap.entrySet()
                                               .iterator()
                                               .next()
                                               .getKey();

        template.hasResourceProperties("AWS::Amplify::Domain", objectLike(
                Map.of("AppId", singletonMap("Fn::GetAtt", List.of(spaId, "AppId")), "DomainName", FamilyDirectoryDomainStack.HOSTED_ZONE_NAME, "EnableAutoSubDomain", false, "SubDomainSettings",
                       singletonList(Map.of("BranchName", singletonMap("Fn::GetAtt", List.of(spaBranchId, "BranchName")), "Prefix", "")))));
    }
}
