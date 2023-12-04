package org.familydirectory.cdk.domain;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.route53.PublicHostedZone;
import software.amazon.awscdk.services.route53.PublicHostedZoneProps;
import software.amazon.awscdk.services.ssm.ParameterTier;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.amazon.awscdk.services.ssm.StringParameterProps;
import software.constructs.Construct;
import static java.lang.Boolean.TRUE;
import static java.lang.System.getenv;

public
class FamilyDirectoryDomainStack extends Stack {
    public static final String HOSTED_ZONE_RESOURCE_ID = "HostedZone";
    public static final String HOSTED_ZONE_NAME = getenv("ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME");
    public static final String HOSTED_ZONE_ID_PARAMETER_NAME = "%sId".formatted(HOSTED_ZONE_RESOURCE_ID);

    public
    FamilyDirectoryDomainStack (final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

//  The HostedZone is the domain/subdomain for which the dns is controlled by Route53
        final PublicHostedZoneProps hostedZoneProps = PublicHostedZoneProps.builder()
                                                                           .zoneName(HOSTED_ZONE_NAME)
                                                                           .caaAmazon(TRUE)
                                                                           .build();
        final PublicHostedZone hostedZone = new PublicHostedZone(this, HOSTED_ZONE_RESOURCE_ID, hostedZoneProps);
        new StringParameter(this, HOSTED_ZONE_ID_PARAMETER_NAME, StringParameterProps.builder()
                                                                                     .parameterName(HOSTED_ZONE_ID_PARAMETER_NAME)
                                                                                     .stringValue(hostedZone.getHostedZoneId())
                                                                                     .tier(ParameterTier.STANDARD)
                                                                                     .build());
    }
}
