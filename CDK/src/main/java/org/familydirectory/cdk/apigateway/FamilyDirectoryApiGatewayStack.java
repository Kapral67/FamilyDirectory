package org.familydirectory.cdk.apigateway;

import java.util.List;
import org.familydirectory.assets.lambda.function.api.enums.ApiFunction;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigatewayv2.alpha.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.alpha.CorsPreflightOptions;
import software.amazon.awscdk.services.apigatewayv2.alpha.DomainName;
import software.amazon.awscdk.services.apigatewayv2.alpha.DomainNameProps;
import software.amazon.awscdk.services.apigatewayv2.alpha.EndpointType;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApiProps;
import software.amazon.awscdk.services.apigatewayv2.alpha.SecurityPolicy;
import software.amazon.awscdk.services.apigatewayv2.authorizers.alpha.HttpUserPoolAuthorizer;
import software.amazon.awscdk.services.apigatewayv2.authorizers.alpha.HttpUserPoolAuthorizerProps;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.HttpLambdaIntegration;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateProps;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.cognito.IUserPoolClient;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClient;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.ARecordProps;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGatewayv2DomainProperties;
import software.constructs.Construct;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.System.getenv;
import static java.util.Collections.singletonList;
import static software.amazon.awscdk.Fn.importValue;

public
class FamilyDirectoryApiGatewayStack extends Stack {
    public static final String API_CERTIFICATE_RESOURCE_ID = "ApiCertificate";
    public static final String API_CERTIFICATE_NAME = "%s-%s".formatted(FamilyDirectoryDomainStack.HOSTED_ZONE_NAME, API_CERTIFICATE_RESOURCE_ID);
    public static final String API_CERTIFICATE_ARN_EXPORT_NAME = "%sArn".formatted(API_CERTIFICATE_RESOURCE_ID);
    public static final String API_DOMAIN_NAME_RESOURCE_ID = "ApiDomainName";
    public static final String API_DOMAIN_NAME = "%s.%s".formatted(getenv("ORG_FAMILYDIRECTORY_API_SUBDOMAIN_NAME"), FamilyDirectoryDomainStack.HOSTED_ZONE_NAME);
    public static final String API_COGNITO_AUTHORIZER_RESOURCE_ID = "HttpUserPoolAuthorizer";
    private static final String HTTP_API_RESOURCE_ID = "HttpApi";
    private static final String SECURE_URL_PREFIX = "https://";
    private static final Duration CORS_MAX_AGE = Duration.days(1);

    public
    FamilyDirectoryApiGatewayStack (final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        final IHostedZone hostedZone = HostedZone.fromHostedZoneId(this, FamilyDirectoryDomainStack.HOSTED_ZONE_RESOURCE_ID, importValue(FamilyDirectoryDomainStack.HOSTED_ZONE_ID_EXPORT_NAME));

//  Api Certificate
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

//  Api Domain
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

//  Configure CORS options for httpApi
        final CorsPreflightOptions httpApiPropsCorsOptions = CorsPreflightOptions.builder()
                                                                                 .allowCredentials(FALSE)
                                                                                 .allowMethods(ApiFunction.getAllowedMethods())
                                                                                 .allowOrigins(singletonList("%s*.%s".formatted(SECURE_URL_PREFIX, hostedZone.getZoneName())))
                                                                                 .maxAge(CORS_MAX_AGE)
                                                                                 .allowHeaders(List.of("X-Amz-Date", "Authorization", "X-Api-Key", "X-Amz-Security-Token"))
                                                                                 .build();
//  Configure HttpApi Options
        final HttpApiProps httpApiProps = HttpApiProps.builder()
                                                      .corsPreflight(httpApiPropsCorsOptions)
                                                      .createDefaultStage(FALSE)
                                                      .disableExecuteApiEndpoint(TRUE)
                                                      .build();
//  Create HttpApi
        final HttpApi httpApi = new HttpApi(this, HTTP_API_RESOURCE_ID, httpApiProps);

//  Create HttpApi Authorizer from Cognito User Pool
        final IUserPoolClient userPoolClient = UserPoolClient.fromUserPoolClientId(this, FamilyDirectoryCognitoStack.COGNITO_USER_POOL_CLIENT_RESOURCE_ID,
                                                                                   importValue(FamilyDirectoryCognitoStack.COGNITO_USER_POOL_CLIENT_ID_EXPORT_NAME));
        final HttpUserPoolAuthorizerProps httpUserPoolAuthorizerProps = HttpUserPoolAuthorizerProps.builder()
                                                                                                   .userPoolClients(singletonList(userPoolClient))
                                                                                                   .build();
        final HttpUserPoolAuthorizer userPoolAuthorizer = new HttpUserPoolAuthorizer(API_COGNITO_AUTHORIZER_RESOURCE_ID,
                                                                                     UserPool.fromUserPoolId(this, FamilyDirectoryCognitoStack.COGNITO_USER_POOL_RESOURCE_ID,
                                                                                                             importValue(FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME)),
                                                                                     httpUserPoolAuthorizerProps);

        for (final ApiFunction func : ApiFunction.values()) {
            final HttpLambdaIntegration httpLambdaIntegration = new HttpLambdaIntegration(func.httpIntegrationId(),
                                                                                          Function.fromFunctionArn(this, func.functionName(), importValue(func.arnExportName())));
//      Add Lambda as HttpIntegration to HttpApi
            httpApi.addRoutes(AddRoutesOptions.builder()
                                              .authorizationScopes(List.of("openid", "email"))
                                              .authorizer(userPoolAuthorizer)
                                              .path(func.endpoint())
                                              .methods(func.methods())
                                              .integration(httpLambdaIntegration)
                                              .build());
            /** TODO: might need a {@link software.amazon.awscdk.CfnOutput} here */
        }
    }
}
