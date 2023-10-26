package org.familydirectory.cdk.test.ses;

import java.util.List;
import java.util.Map;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
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
import static software.amazon.awscdk.assertions.Match.objectEquals;
import static software.amazon.awscdk.assertions.Match.objectLike;

public
class FamilyDirectorySesStackTest {
    private static final String FULL_MAIL_FROM_DOMAIN = "%s.".formatted(FamilyDirectorySesStack.SES_MAIL_FROM_DOMAIN_NAME);
    private static final String MX_RECORD = "10 feedback-smtp.%s.amazonses.com".formatted(FamilyDirectoryCdkApp.DEFAULT_REGION);

    @Test
    public
    void testStack () {
        final App app = new App();

        final FamilyDirectorySesStack stack = new FamilyDirectorySesStack(app, FamilyDirectoryCdkApp.SES_STACK_NAME, StackProps.builder()
                                                                                                                               .env(FamilyDirectoryCdkApp.DEFAULT_ENV)
                                                                                                                               .stackName(FamilyDirectoryCdkApp.SES_STACK_NAME)
                                                                                                                               .build());

        final Template template = Template.fromStack(stack);

        final Capture emailIdentityArnCapture = new Capture();
        template.hasOutput(FamilyDirectorySesStack.SES_EMAIL_IDENTITY_ARN_EXPORT_NAME, objectLike(singletonMap("Value", emailIdentityArnCapture)));
        assertTrue(emailIdentityArnCapture.asString()
                                          .contains(FamilyDirectoryCdkApp.DEFAULT_ACCOUNT));
        assertTrue(emailIdentityArnCapture.asString()
                                          .contains(FamilyDirectoryCdkApp.DEFAULT_REGION));
        assertTrue(emailIdentityArnCapture.asString()
                                          .contains(FamilyDirectoryDomainStack.HOSTED_ZONE_NAME));

        final Capture emailIdentityNameCapture = new Capture();
        template.hasOutput(FamilyDirectorySesStack.SES_EMAIL_IDENTITY_NAME_EXPORT_NAME, objectLike(singletonMap("Value", singletonMap("Ref", emailIdentityNameCapture))));
        assertFalse(emailIdentityNameCapture.asString()
                                            .isBlank());

        final String hostedZoneIdParameter = "%sParameter".formatted(FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME);
        template.hasParameter(hostedZoneIdParameter, objectEquals(Map.of("Type", "AWS::SSM::Parameter::Value<String>", "Default", FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME)));

        template.hasResourceProperties("AWS::SES::ConfigurationSet", objectLike(
                Map.of("DeliveryOptions", singletonMap("TlsPolicy", "REQUIRE"), "Name", FamilyDirectorySesStack.SES_CONFIGURATION_SET_NAME, "SuppressionOptions",
                       singletonMap("SuppressedReasons", List.of("BOUNCE", "COMPLAINT")))));

        final Map<String, Map<String, Object>> emailIdentityResourceMap = template.findResources("AWS::SES::EmailIdentity", objectLike(singletonMap("Properties", Map.of("DkimAttributes",
                                                                                                                                                                         singletonMap("SigningEnabled",
                                                                                                                                                                                      true),
                                                                                                                                                                         "EmailIdentity",
                                                                                                                                                                         FamilyDirectoryDomainStack.HOSTED_ZONE_NAME,
                                                                                                                                                                         "MailFromAttributes",
                                                                                                                                                                         Map.of("BehaviorOnMxFailure",
                                                                                                                                                                                "REJECT_MESSAGE",
                                                                                                                                                                                "MailFromDomain",
                                                                                                                                                                                FamilyDirectorySesStack.SES_MAIL_FROM_DOMAIN_NAME)))));
        assertEquals(1, emailIdentityResourceMap.size());
        final String emailIdentityResource = emailIdentityResourceMap.entrySet()
                                                                     .iterator()
                                                                     .next()
                                                                     .getKey();

        for (int i = 1; i <= 3; ++i) {
            template.hasResourceProperties("AWS::Route53::RecordSet", objectLike(Map.of(FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME, singletonMap("Ref", hostedZoneIdParameter), "Name",
                                                                                        singletonMap("Fn::GetAtt", List.of(emailIdentityResource, "DkimDNSTokenName%d".formatted(i))),
                                                                                        "ResourceRecords",
                                                                                        singletonList(singletonMap("Fn::GetAtt", List.of(emailIdentityResource, "DkimDNSTokenValue%d".formatted(i)))),
                                                                                        "TTL", "1800", "Type", "CNAME")));
        }

        template.hasResourceProperties("AWS::Route53::RecordSet", objectLike(
                Map.of(FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME, singletonMap("Ref", hostedZoneIdParameter), "Name", FULL_MAIL_FROM_DOMAIN, "ResourceRecords", singletonList(MX_RECORD),
                       "TTL", "1800", "Type", "MX")));

        template.hasResourceProperties("AWS::Route53::RecordSet", objectLike(
                Map.of(FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME, singletonMap("Ref", hostedZoneIdParameter), "Name", FULL_MAIL_FROM_DOMAIN, "ResourceRecords",
                       singletonList("\"v=spf1 include:amazonses.com ~all\""), "TTL", "1800", "Type", "TXT")));
    }
}
