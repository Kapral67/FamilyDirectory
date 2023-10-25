package org.familydirectory.cdk.test.domain;

import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Capture;
import software.amazon.awscdk.assertions.Template;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static software.amazon.awscdk.assertions.Match.objectLike;

public
class FamilyDirectoryDomainStackTest {
    private static final String FULL_HOSTED_ZONE_NAME = "%s.".formatted(FamilyDirectoryDomainStack.HOSTED_ZONE_NAME);

    @Test
    public
    void testStack () {
        final App app = new App();

        final FamilyDirectoryDomainStack stack = new FamilyDirectoryDomainStack(app, FamilyDirectoryCdkApp.DOMAIN_STACK_NAME, StackProps.builder()
                                                                                                                                        .env(FamilyDirectoryCdkApp.DEFAULT_ENVIRONMENT)
                                                                                                                                        .stackName(FamilyDirectoryCdkApp.DOMAIN_STACK_NAME)
                                                                                                                                        .build());

        final Template template = Template.fromStack(stack);

        final Capture hostedZoneIdCapture = new Capture();
        template.hasOutput(FamilyDirectoryDomainStack.HOSTED_ZONE_ID_EXPORT_NAME, objectLike(singletonMap("Value", singletonMap("Ref", hostedZoneIdCapture))));
        assertFalse(hostedZoneIdCapture.asString()
                                       .isBlank());

        template.hasResourceProperties("AWS::Route53::HostedZone", objectLike(singletonMap("Name", FULL_HOSTED_ZONE_NAME)));

        template.hasResourceProperties("AWS::Route53::RecordSet", objectLike(singletonMap(FamilyDirectoryDomainStack.HOSTED_ZONE_ID_EXPORT_NAME, singletonMap("Ref", hostedZoneIdCapture.asString()))));
    }
}
