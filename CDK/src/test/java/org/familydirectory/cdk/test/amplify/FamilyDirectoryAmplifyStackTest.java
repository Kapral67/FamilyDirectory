package org.familydirectory.cdk.test.amplify;

import java.util.Map;
import java.util.regex.Pattern;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.amplify.FamilyDirectoryAmplifyStack;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Capture;
import software.amazon.awscdk.assertions.Template;
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
        // FIXME: REMOVE
        System.err.println(rootMemberSurnameResourceCreateActionCapture.asString());
        assertTrue(GET_ROOT_MEMBER_REGEX.matcher(rootMemberSurnameResourceCreateActionCapture.asString())
                                        .matches());
        assertTrue(GET_ROOT_MEMBER_REGEX.matcher(rootMemberSurnameResourceUpdateActionCapture.asString())
                                        .matches());
        final String rootMemberSurnameResourceId = rootMemberSurnameResourceMap.entrySet()
                                                                               .iterator()
                                                                               .next()
                                                                               .getKey();

    }
}
