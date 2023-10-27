package org.familydirectory.cdk.cognito;

import java.util.Optional;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.customresource.SSMParameterReader;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateProps;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneAttributes;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.ssm.ParameterTier;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.amazon.awscdk.services.ssm.StringParameterProps;
import software.amazon.awssdk.regions.Region;
import software.constructs.Construct;

public
class FamilyDirectoryCognitoUsEastOneStack extends Stack {
    public static final String COGNITO_CERTIFICATE_RESOURCE_ID = "CognitoCertificate";
    public static final String COGNITO_CERTIFICATE_NAME = "%s-%s".formatted(FamilyDirectoryDomainStack.HOSTED_ZONE_NAME, COGNITO_CERTIFICATE_RESOURCE_ID);
    public static final String COGNITO_CERTIFICATE_ARN_PARAMETER_NAME = "%sArn".formatted(COGNITO_CERTIFICATE_RESOURCE_ID);
    public static final String REGION = Region.US_EAST_1.toString();

    public
    FamilyDirectoryCognitoUsEastOneStack (final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);
        Optional.of(stackProps)
                .map(StackProps::getEnv)
                .map(Environment::getRegion)
                .filter(s -> s.equals(REGION))
                .orElseThrow();

        final String hostedZoneId;
        if (FamilyDirectoryCdkApp.DEFAULT_REGION.equals(REGION)) {
            hostedZoneId = StringParameter.fromStringParameterName(this, FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME, FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME)
                                          .getStringValue();
        } else {
            hostedZoneId = new SSMParameterReader(this, SSMParameterReader.SSM_PARAMETER_READER_RESOURCE_ID, FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME,
                                                  Region.of(FamilyDirectoryCdkApp.DEFAULT_REGION)).toString();
        }
        final HostedZoneAttributes hostedZoneAttrs = HostedZoneAttributes.builder()
                                                                         .hostedZoneId(hostedZoneId)
                                                                         .zoneName(FamilyDirectoryDomainStack.HOSTED_ZONE_NAME)
                                                                         .build();
        final IHostedZone hostedZone = HostedZone.fromHostedZoneAttributes(this, FamilyDirectoryDomainStack.HOSTED_ZONE_RESOURCE_ID, hostedZoneAttrs);

        //  Cognito Certificate
        final CertificateProps cognitoCertificateProps = CertificateProps.builder()
                                                                         .certificateName(COGNITO_CERTIFICATE_NAME)
                                                                         .domainName(FamilyDirectoryCognitoStack.COGNITO_DOMAIN_NAME)
                                                                         .validation(CertificateValidation.fromDns(hostedZone))
                                                                         .build();
        final Certificate cognitoCertificate = new Certificate(this, COGNITO_CERTIFICATE_RESOURCE_ID, cognitoCertificateProps);

        new StringParameter(this, COGNITO_CERTIFICATE_ARN_PARAMETER_NAME, StringParameterProps.builder()
                                                                                              .parameterName(COGNITO_CERTIFICATE_ARN_PARAMETER_NAME)
                                                                                              .stringValue(cognitoCertificate.getCertificateArn())
                                                                                              .tier(ParameterTier.STANDARD)
                                                                                              .build());
    }
}
