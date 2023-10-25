package org.familydirectory.cdk.test.ses;

import java.util.List;
import java.util.Map;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.familydirectory.cdk.ses.FamilyDirectorySesStack;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Capture;
import software.amazon.awscdk.assertions.Template;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awscdk.assertions.Match.objectLike;

public
class FamilyDirectorySesStackTest {
    private static final Environment SES_ENVIRONMENT = FamilyDirectoryCdkApp.DEFAULT_ENVIRONMENT;

    @Test
    public
    void testStack () {
        final App app = new App();

        final FamilyDirectorySesStack stack = new FamilyDirectorySesStack(app, FamilyDirectoryCdkApp.SES_STACK_NAME, StackProps.builder()
                                                                                                                               .env(SES_ENVIRONMENT)
                                                                                                                               .stackName(FamilyDirectoryCdkApp.SES_STACK_NAME)
                                                                                                                               .build());

        final Template template = Template.fromStack(stack);

        final Capture emailIdentityArnCapture = new Capture();
        template.hasOutput(FamilyDirectorySesStack.SES_EMAIL_IDENTITY_ARN_EXPORT_NAME, objectLike(singletonMap("Value", emailIdentityArnCapture)));
        assertTrue(emailIdentityArnCapture.asString()
                                          .contains(requireNonNull(SES_ENVIRONMENT.getAccount())));
        assertTrue(emailIdentityArnCapture.asString()
                                          .contains(requireNonNull(SES_ENVIRONMENT.getRegion())));
        assertTrue(emailIdentityArnCapture.asString()
                                          .contains(FamilyDirectoryDomainStack.HOSTED_ZONE_NAME));

        final Capture emailIdentityNameCapture = new Capture();
        template.hasOutput(FamilyDirectorySesStack.SES_EMAIL_IDENTITY_NAME_EXPORT_NAME, objectLike(singletonMap("Value", singletonMap("Ref", emailIdentityNameCapture))));
        assertFalse(emailIdentityNameCapture.asString()
                                            .isBlank());

        template.hasResourceProperties("AWS::SES::ConfigurationSet", objectLike(
                Map.of("DeliveryOptions", singletonMap("TlsPolicy", "REQUIRE"), "Name", FamilyDirectorySesStack.SES_CONFIGURATION_SET_NAME, "SuppressionOptions",
                       singletonMap("SuppressedReasons", List.of("BOUNCE", "COMPLAINT")))));

        template.hasResourceProperties("AWS::SES::EmailIdentity", objectLike(
                Map.of("DkimAttributes", singletonMap("SigningEnabled", true), "EmailIdentity", FamilyDirectoryDomainStack.HOSTED_ZONE_NAME, "MailFromAttributes",
                       Map.of("BehaviorOnMxFailure", "REJECT_MESSAGE", "MailFromDomain", FamilyDirectorySesStack.SES_MAIL_FROM_DOMAIN_NAME))));
    }
}
