package org.familydirectory.cdk.ses;

import java.util.Optional;
import java.util.function.Predicate;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Environment;
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
import static java.util.Objects.requireNonNull;
import static software.amazon.awscdk.Fn.importValue;

public
class FamilyDirectorySesStack extends Stack {

    public static final String SES_CONFIGURATION_SET_RESOURCE_ID = "ConfigurationSet";
    public static final String SES_CONFIGURATION_SET_NAME = "DefaultSES%s".formatted(SES_CONFIGURATION_SET_RESOURCE_ID);
    public static final String SES_EMAIL_IDENTITY_RESOURCE_ID = "EmailIdentity";
    public static final String SES_EMAIL_IDENTITY_ARN_EXPORT_NAME = "%sArn".formatted(SES_EMAIL_IDENTITY_RESOURCE_ID);

    public
    FamilyDirectorySesStack (final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);
        final Environment env = requireNonNull(stackProps.getEnv());
        final String region = Optional.ofNullable(env.getRegion())
                                      .filter(Predicate.not(String::isBlank))
                                      .filter(s -> !s.equals("Aws.REGION"))
                                      .orElseThrow();
        final String account = Optional.ofNullable(env.getAccount())
                                       .filter(Predicate.not(String::isBlank))
                                       .filter(s -> !s.equals("Aws.ACCOUNT_ID"))
                                       .orElseThrow();

//  PUBLIC HOSTED ZONE
        final IPublicHostedZone hostedZone = PublicHostedZone.fromPublicHostedZoneId(this, FamilyDirectoryDomainStack.HOSTED_ZONE_RESOURCE_ID,
                                                                                     importValue(FamilyDirectoryDomainStack.HOSTED_ZONE_ID_EXPORT_NAME));

//  CONFIGURATION SET
        final ConfigurationSetProps configurationSetProps = ConfigurationSetProps.builder()
                                                                                 .configurationSetName(SES_CONFIGURATION_SET_NAME)
                                                                                 .suppressionReasons(SuppressionReasons.BOUNCES_AND_COMPLAINTS)
                                                                                 .tlsPolicy(ConfigurationSetTlsPolicy.REQUIRE)
                                                                                 .build();
        final ConfigurationSet configurationSet = new ConfigurationSet(this, SES_CONFIGURATION_SET_RESOURCE_ID, configurationSetProps);

//  EMAIL IDENTITY
        final EmailIdentityProps emailIdentityProps = EmailIdentityProps.builder()
                                                                        .configurationSet(configurationSet)
                                                                        .dkimSigning(TRUE)
                                                                        .identity(Identity.publicHostedZone(hostedZone))
                                                                        .mailFromBehaviorOnMxFailure(MailFromBehaviorOnMxFailure.REJECT_MESSAGE)
                                                                        .mailFromDomain(hostedZone.getZoneName())
                                                                        .build();
        new EmailIdentity(this, SES_EMAIL_IDENTITY_RESOURCE_ID, emailIdentityProps);
        new CfnOutput(this, SES_EMAIL_IDENTITY_ARN_EXPORT_NAME, CfnOutputProps.builder()
                                                                              .value("arn:aws:ses:%s:%s:identity/%s".formatted(region, account, hostedZone.getZoneName()))
                                                                              .exportName(SES_EMAIL_IDENTITY_ARN_EXPORT_NAME)
                                                                              .build());

//      TODO: <a href="https://docs.aws.amazon.com/cdk/api/v2/java/software/amazon/awscdk/services/ses/package-summary.html#email-receiving-heading"> EMAIL RECEIVING </a>
    }
}
