package org.familydirectory.cdk.domain;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProps;
import software.constructs.Construct;
import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static java.lang.System.getenv;

public
class FamilyDirectoryDomainStack extends Stack {
    public static final String HOSTED_ZONE_RESOURCE_ID = "HostedZone";
    public static final String HOSTED_ZONE_NAME = getenv("ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME");
    public static final String HOSTED_ZONE_ID_EXPORT_NAME = format("%sId", HOSTED_ZONE_RESOURCE_ID);

    public
    FamilyDirectoryDomainStack (final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        // The HostedZone is the domain/subdomain for which the dns is controlled by Route53
        final HostedZoneProps hostedZoneProps = HostedZoneProps.builder()
                                                               .zoneName(HOSTED_ZONE_NAME)
                                                               .addTrailingDot(FALSE)
                                                               .build();
        final HostedZone hostedZone = new HostedZone(this, HOSTED_ZONE_RESOURCE_ID, hostedZoneProps);
        new CfnOutput(this, HOSTED_ZONE_ID_EXPORT_NAME, CfnOutputProps.builder()
                                                                      .value(hostedZone.getHostedZoneId())
                                                                      .exportName(HOSTED_ZONE_ID_EXPORT_NAME)
                                                                      .build());
    }
}
