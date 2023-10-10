package org.familydirectory.cdk.ses;

import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.route53.IPublicHostedZone;
import software.amazon.awscdk.services.route53.PublicHostedZone;
import software.amazon.awscdk.services.ses.ConfigurationSet;
import software.amazon.awscdk.services.ses.ConfigurationSetProps;
import software.amazon.awscdk.services.ses.ConfigurationSetTlsPolicy;
import software.amazon.awscdk.services.ses.EmailIdentity;
import software.amazon.awscdk.services.ses.EmailIdentityProps;
import software.amazon.awscdk.services.ses.Identity;
import software.amazon.awscdk.services.ses.MailFromBehaviorOnMxFailure;
import software.amazon.awscdk.services.ses.SuppressionReasons;
import software.constructs.Construct;
import static java.lang.Boolean.TRUE;
import static software.amazon.awscdk.Fn.importValue;

public
class FamilyDirectorySesStack extends Stack {

    public static final String CONFIGURATION_SET_RESOURCE_ID = "ConfigurationSet";
    public static final String CONFIGURATION_SET_NAME = "DefaultSES%s".formatted(CONFIGURATION_SET_RESOURCE_ID);
    public static final String EMAIL_IDENTITY_RESOURCE_ID = "EmailIdentity";

    public
    FamilyDirectorySesStack (final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

//  PUBLIC HOSTED ZONE
        final IPublicHostedZone hostedZone = PublicHostedZone.fromPublicHostedZoneId(this, FamilyDirectoryDomainStack.HOSTED_ZONE_RESOURCE_ID,
                                                                                     importValue(FamilyDirectoryDomainStack.HOSTED_ZONE_ID_EXPORT_NAME));

//  CONFIGURATION SET
        final ConfigurationSetProps configurationSetProps = ConfigurationSetProps.builder()
                                                                                 .configurationSetName(CONFIGURATION_SET_NAME)
                                                                                 .suppressionReasons(SuppressionReasons.BOUNCES_AND_COMPLAINTS)
                                                                                 .tlsPolicy(ConfigurationSetTlsPolicy.REQUIRE)
                                                                                 .build();
        final ConfigurationSet configurationSet = new ConfigurationSet(this, CONFIGURATION_SET_RESOURCE_ID, configurationSetProps);

//  EMAIL IDENTITY
        final EmailIdentityProps emailIdentityProps = EmailIdentityProps.builder()
                                                                        .configurationSet(configurationSet)
                                                                        .dkimSigning(TRUE)
                                                                        .identity(Identity.publicHostedZone(hostedZone))
                                                                        .mailFromBehaviorOnMxFailure(MailFromBehaviorOnMxFailure.REJECT_MESSAGE)
                                                                        .mailFromDomain(hostedZone.getZoneName())
                                                                        .build();
        new EmailIdentity(this, EMAIL_IDENTITY_RESOURCE_ID, emailIdentityProps);

//      TODO: <a href="https://docs.aws.amazon.com/cdk/api/v2/java/software/amazon/awscdk/services/ses/package-summary.html#email-receiving-heading"> EMAIL RECEIVING </a>
    }
}
