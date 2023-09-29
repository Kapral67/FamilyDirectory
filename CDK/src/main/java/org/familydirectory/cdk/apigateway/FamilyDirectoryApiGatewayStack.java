package org.familydirectory.cdk.apigateway;

import java.util.List;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigatewayv2.alpha.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.alpha.CorsHttpMethod;
import software.amazon.awscdk.services.apigatewayv2.alpha.CorsPreflightOptions;
import software.amazon.awscdk.services.apigatewayv2.alpha.DomainName;
import software.amazon.awscdk.services.apigatewayv2.alpha.DomainNameProps;
import software.amazon.awscdk.services.apigatewayv2.alpha.EndpointType;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApiProps;
import software.amazon.awscdk.services.apigatewayv2.alpha.IHttpRouteAuthorizer;
import software.amazon.awscdk.services.apigatewayv2.alpha.SecurityPolicy;
import software.amazon.awscdk.services.apigatewayv2.authorizers.alpha.HttpUserPoolAuthorizer;
import software.amazon.awscdk.services.apigatewayv2.authorizers.alpha.HttpUserPoolAuthorizerProps;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.HttpLambdaIntegration;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.HttpLambdaIntegrationProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateProps;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.cognito.IUserPool;
import software.amazon.awscdk.services.cognito.IUserPoolClient;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClient;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.IFunction;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.ARecordProps;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGatewayv2DomainProperties;
import software.constructs.Construct;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.lang.System.getenv;
import static java.util.Collections.singletonList;
import static org.familydirectory.assets.lambda.LambdaFunctionAttrs.ADMIN_CREATE_MEMBER;
import static org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack.COGNITO_USER_POOL_CLIENT_ID_EXPORT_NAME;
import static org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack.COGNITO_USER_POOL_CLIENT_RESOURCE_ID;
import static org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME;
import static org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack.COGNITO_USER_POOL_RESOURCE_ID;
import static org.familydirectory.cdk.domain.FamilyDirectoryDomainStack.HOSTED_ZONE_ID_EXPORT_NAME;
import static org.familydirectory.cdk.domain.FamilyDirectoryDomainStack.HOSTED_ZONE_NAME;
import static org.familydirectory.cdk.domain.FamilyDirectoryDomainStack.HOSTED_ZONE_RESOURCE_ID;
import static software.amazon.awscdk.Fn.importValue;
import static software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod.POST;
import static software.amazon.awscdk.services.apigatewayv2.alpha.PayloadFormatVersion.VERSION_2_0;

public
class FamilyDirectoryApiGatewayStack extends Stack {
    public static final String API_CERTIFICATE_RESOURCE_ID = "ApiCertificate";
    public static final String API_CERTIFICATE_NAME = format("%s-%s", HOSTED_ZONE_NAME, API_CERTIFICATE_RESOURCE_ID);
    public static final String API_CERTIFICATE_ARN_EXPORT_NAME = format("%sArn", API_CERTIFICATE_RESOURCE_ID);
    public static final String API_DOMAIN_NAME_RESOURCE_ID = "ApiDomainName";
    public static final String API_DOMAIN_NAME = format("%s.%s", getenv("ORG_FAMILYDIRECTORY_API_SUBDOMAIN_NAME"), HOSTED_ZONE_NAME);
    public static final String API_COGNITO_AUTHORIZER_RESOURCE_ID = "HttpUserPoolAuthorizer";
    private static final String HTTP_API_RESOURCE_ID = "HttpApi";
    private static final String SECURE_URL_PREFIX = "https://";

    public
    FamilyDirectoryApiGatewayStack (final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        final IHostedZone hostedZone = HostedZone.fromHostedZoneId(this, HOSTED_ZONE_RESOURCE_ID, importValue(HOSTED_ZONE_ID_EXPORT_NAME));

        // Api Certificate
        final CertificateProps apiCertificateProps = CertificateProps.builder()
                                                                     .certificateName(API_CERTIFICATE_NAME)
                                                                     .domainName(API_DOMAIN_NAME)
                                                                     .validation(CertificateValidation.fromDns(hostedZone))
                                                                     .build();
        final Certificate apiCertificate = new Certificate(this, API_CERTIFICATE_RESOURCE_ID, apiCertificateProps);
        new CfnOutput(this, API_CERTIFICATE_ARN_EXPORT_NAME, CfnOutputProps.builder()
                                                                           .value(apiCertificate.getCertificateArn())
                                                                           .exportName(API_CERTIFICATE_ARN_EXPORT_NAME)
                                                                           .build());

        // Api Domain
        final DomainNameProps apiDomainNameProps = DomainNameProps.builder()
                                                                  .domainName(API_DOMAIN_NAME)
                                                                  .certificate(apiCertificate)
                                                                  .endpointType(EndpointType.REGIONAL)
                                                                  .securityPolicy(SecurityPolicy.TLS_1_2)
                                                                  .build();
        final DomainName apiDomainName = new DomainName(this, API_DOMAIN_NAME_RESOURCE_ID, apiDomainNameProps);
        final ApiGatewayv2DomainProperties apiDomainNameProperties = new ApiGatewayv2DomainProperties(apiDomainName.getRegionalDomainName(), apiDomainName.getRegionalHostedZoneId());
        final ARecordProps apiARecordProps = ARecordProps.builder()
                                                         .zone(hostedZone)
                                                         .recordName(API_DOMAIN_NAME)
                                                         .target(RecordTarget.fromAlias(apiDomainNameProperties))
                                                         .build();
        new ARecord(this, "ApiARecord", apiARecordProps);

        /** TODO: Research what CORS options I need */
        // Configure CORS options for httpApi
        final CorsPreflightOptions httpApiPropsCorsOptions = CorsPreflightOptions.builder()
                                                                                 .allowCredentials(FALSE)
                                                                                 .allowMethods(List.of(CorsHttpMethod.POST))
                                                                                 .allowOrigins(singletonList(SECURE_URL_PREFIX + '*'))
                                                                                 .maxAge(Duration.days(1))
                                                                                 .build();
        /** TODO: Research potential {@link HttpApiProps} */
        // Configure HttpApi Options
        final HttpApiProps httpApiProps = HttpApiProps.builder()
                                                      .corsPreflight(httpApiPropsCorsOptions)
                                                      .createDefaultStage(FALSE)
                                                      .disableExecuteApiEndpoint(TRUE)
                                                      .build();
        // Create HttpApi
        final HttpApi httpApi = new HttpApi(this, HTTP_API_RESOURCE_ID, httpApiProps);

        // Get Lambda Function by arn
        final String adminCreateMemberLambdaFunctionArn = importValue(ADMIN_CREATE_MEMBER.arnExportName());
        final IFunction adminCreateMemberLambda = Function.fromFunctionArn(this, ADMIN_CREATE_MEMBER.functionName(), adminCreateMemberLambdaFunctionArn);

        // Add Lambda as HttpIntegration to HttpApi
        final HttpLambdaIntegrationProps adminCreateMemberLambdaHttpIntegrationProps = HttpLambdaIntegrationProps.builder()
                                                                                                                 .payloadFormatVersion(VERSION_2_0)
                                                                                                                 .build();
        final HttpLambdaIntegration adminCreateMemberLambdaHttpIntegration = new HttpLambdaIntegration(ADMIN_CREATE_MEMBER.httpIntegrationId(), adminCreateMemberLambda,
                                                                                                       adminCreateMemberLambdaHttpIntegrationProps);
        /** TODO: Research potential for {@link AddRoutesOptions.Builder#authorizationScopes(List)} &
         *  {@link AddRoutesOptions.Builder#authorizer(IHttpRouteAuthorizer)} */
        final IUserPoolClient userPoolClient = UserPoolClient.fromUserPoolClientId(this, COGNITO_USER_POOL_CLIENT_RESOURCE_ID, importValue(COGNITO_USER_POOL_CLIENT_ID_EXPORT_NAME));
        final HttpUserPoolAuthorizerProps userPoolAuthorizerProps = HttpUserPoolAuthorizerProps.builder()
                                                                                               .userPoolClients(singletonList(userPoolClient))
                                                                                               .build();
        final IUserPool userPool = UserPool.fromUserPoolId(this, COGNITO_USER_POOL_RESOURCE_ID, importValue(COGNITO_USER_POOL_ID_EXPORT_NAME));
        final HttpUserPoolAuthorizer userPoolAuthorizer = new HttpUserPoolAuthorizer(API_COGNITO_AUTHORIZER_RESOURCE_ID, userPool, userPoolAuthorizerProps);
        /** TODO: Figure out how to get the email address of the user who called the api from the adminCreateMember Lambda function */
        final AddRoutesOptions adminCreateMemberLambdaApiRouteOptions = AddRoutesOptions.builder()
//                                                                                      .authorizationScopes(List.of(""))
                                                                                        .authorizer(userPoolAuthorizer)
                                                                                        .path(ADMIN_CREATE_MEMBER.endpoint())
                                                                                        .methods(singletonList(POST))
                                                                                        .integration(adminCreateMemberLambdaHttpIntegration)
                                                                                        .build();
        httpApi.addRoutes(adminCreateMemberLambdaApiRouteOptions);
        /** TODO: might need a {@link software.amazon.awscdk.CfnOutput} here */
    }
}
