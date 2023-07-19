package org.familydirectory.cdk.apigateway;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.DomainName;
import software.amazon.awscdk.services.apigateway.DomainNameProps;
import software.amazon.awscdk.services.apigatewayv2.alpha.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApiProps;
import software.amazon.awscdk.services.apigatewayv2.alpha.IHttpRouteAuthorizer;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.HttpLambdaIntegration;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.HttpLambdaIntegrationProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateProps;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.IFunction;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProps;
import software.constructs.Construct;

import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static java.lang.System.getenv;
import static java.util.Collections.singletonList;
import static org.familydirectory.assets.lambda.LambdaFunctionAttrs.ADMIN_CREATE_MEMBER;
import static software.amazon.awscdk.Fn.importValue;
import static software.amazon.awscdk.services.apigateway.EndpointType.REGIONAL;
import static software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod.POST;
import static software.amazon.awscdk.services.apigatewayv2.alpha.PayloadFormatVersion.VERSION_2_0;

public class FamilyDirectoryApiGatewayStack extends Stack {
    public static final String HOSTED_ZONE_RESOURCE_ID = "HostedZone";
    public static final String CERTIFICATE_RESOURCE_ID = "Certificate";
    public static final String DOMAIN_NAME_RESOURCE_ID = "DomainName";
    public static final String HTTP_API_RESOURCE_ID = "HttpApi";
    public static final String HOSTED_ZONE_NAME = getenv("ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME");
    public static final String CERTIFICATE_NAME = format("%s-%s", HOSTED_ZONE_NAME, CERTIFICATE_RESOURCE_ID);
    public static final String API_DOMAIN_NAME =
            format("%s.%s", getenv("ORG_FAMILYDIRECTORY_API_SUBDOMAIN_NAME"), HOSTED_ZONE_NAME);

    public FamilyDirectoryApiGatewayStack(final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        final HostedZone hostedZone = new HostedZone(this, HOSTED_ZONE_RESOURCE_ID,
                HostedZoneProps.builder().zoneName(HOSTED_ZONE_NAME).addTrailingDot(FALSE).build());

        final Certificate certificate = new Certificate(this, CERTIFICATE_RESOURCE_ID,
                CertificateProps.builder().certificateName(CERTIFICATE_NAME).domainName(API_DOMAIN_NAME)
                        .validation(CertificateValidation.fromDns(hostedZone)).build());

        final DomainName apiDomain = new DomainName(this, DOMAIN_NAME_RESOURCE_ID,
                DomainNameProps.builder().domainName(API_DOMAIN_NAME).certificate(certificate).endpointType(REGIONAL)
                        .build());

        /** TODO: Research potential {@link HttpApiProps} */
        final HttpApi httpApi = new HttpApi(this, HTTP_API_RESOURCE_ID);
        final IFunction adminCreateMemberLambda =
                Function.fromFunctionArn(this, ADMIN_CREATE_MEMBER.functionName(),
                        importValue(ADMIN_CREATE_MEMBER.arnExportName()));
        final HttpLambdaIntegration adminCreateMemberLambdaHttpIntegration =
                new HttpLambdaIntegration(ADMIN_CREATE_MEMBER.httpIntegrationId(), adminCreateMemberLambda,
                        HttpLambdaIntegrationProps.builder().payloadFormatVersion(VERSION_2_0).build());
        /** TODO: Research potential for {@link AddRoutesOptions.Builder#authorizationScopes(List)} &
         *  {@link AddRoutesOptions.Builder#authorizer(IHttpRouteAuthorizer)} */
        httpApi.addRoutes(AddRoutesOptions.builder().path(ADMIN_CREATE_MEMBER.endpoint()).methods(singletonList(POST))
                .integration(adminCreateMemberLambdaHttpIntegration).build());
        // TODO: might need a CfnOutput here
    }
}
