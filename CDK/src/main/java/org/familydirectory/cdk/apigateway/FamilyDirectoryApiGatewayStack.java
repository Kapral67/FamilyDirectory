package org.familydirectory.cdk.apigateway;

import java.util.List;
import org.familydirectory.assets.lambda.function.api.enums.ApiFunction;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigatewayv2.alpha.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.alpha.CorsPreflightOptions;
import software.amazon.awscdk.services.apigatewayv2.alpha.DomainMappingOptions;
import software.amazon.awscdk.services.apigatewayv2.alpha.DomainName;
import software.amazon.awscdk.services.apigatewayv2.alpha.DomainNameProps;
import software.amazon.awscdk.services.apigatewayv2.alpha.EndpointType;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApiProps;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpStageOptions;
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
import software.amazon.awscdk.services.lambda.CfnPermission;
import software.amazon.awscdk.services.lambda.CfnPermissionProps;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.IFunction;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.ARecordProps;
import software.amazon.awscdk.services.route53.IPublicHostedZone;
import software.amazon.awscdk.services.route53.PublicHostedZone;
import software.amazon.awscdk.services.route53.PublicHostedZoneAttributes;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGatewayv2DomainProperties;
import software.amazon.awscdk.services.ssm.IStringParameter;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;
import static java.lang.Boolean.FALSE;
import static java.lang.System.getenv;
import static java.util.Collections.singletonList;
import static software.amazon.awscdk.Fn.importValue;

public
class FamilyDirectoryApiGatewayStack extends Stack {
    public static final String API_CERTIFICATE_RESOURCE_ID = "ApiCertificate";
    public static final String API_A_RECORD_RESOURCE_ID = "ApiARecord";
    public static final String API_DOMAIN_NAME = "%s.%s".formatted(getenv("ORG_FAMILYDIRECTORY_API_SUBDOMAIN_NAME"), FamilyDirectoryDomainStack.HOSTED_ZONE_NAME);
    public static final String API_CERTIFICATE_NAME = "%s-%s".formatted(API_DOMAIN_NAME, API_CERTIFICATE_RESOURCE_ID);
    public static final String API_DOMAIN_NAME_RESOURCE_ID = "ApiDomainName";
    public static final String API_COGNITO_AUTHORIZER_RESOURCE_ID = "HttpUserPoolAuthorizer";
    public static final String HTTP_API_RESOURCE_ID = "HttpApi";
    public static final Duration CORS_MAX_AGE = Duration.days(1);
    public static final boolean HTTP_API_DISABLE_EXECUTE_API_ENDPOINT = true;
    public static final boolean CORS_ALLOW_CREDENTIALS = false;
    public static final List<String> CORS_ALLOW_ORIGIN = singletonList(FamilyDirectoryCdkApp.HTTPS_PREFIX + FamilyDirectoryDomainStack.HOSTED_ZONE_NAME);
    public static final List<String> CORS_ALLOW_HEADERS = List.of("X-Amz-Date", "Authorization", "X-Api-Key", "X-Amz-Security-Token");
    public static final List<String> HTTP_API_ROUTE_AUTHORIZATION_SCOPES = List.of("openid", "email");
    public static final String HTTP_API_PUBLIC_STAGE_ID = "Prod";
    public static final boolean HTTP_API_PUBLIC_STAGE_AUTO_DEPLOY = true;

    public
    FamilyDirectoryApiGatewayStack (final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        final IStringParameter hostedZoneId = StringParameter.fromStringParameterName(this, FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME,
                                                                                      FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME);
        final PublicHostedZoneAttributes hostedZoneAttrs = PublicHostedZoneAttributes.builder()
                                                                                     .hostedZoneId(hostedZoneId.getStringValue())
                                                                                     .zoneName(FamilyDirectoryDomainStack.HOSTED_ZONE_NAME)
                                                                                     .build();
        final IPublicHostedZone hostedZone = PublicHostedZone.fromPublicHostedZoneAttributes(this, FamilyDirectoryDomainStack.HOSTED_ZONE_RESOURCE_ID, hostedZoneAttrs);

//  Api Certificate
        final CertificateProps apiCertificateProps = CertificateProps.builder()
                                                                     .certificateName(API_CERTIFICATE_NAME)
                                                                     .domainName(API_DOMAIN_NAME)
                                                                     .validation(CertificateValidation.fromDns(hostedZone))
                                                                     .build();
        final Certificate apiCertificate = new Certificate(this, API_CERTIFICATE_RESOURCE_ID, apiCertificateProps);

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
        new ARecord(this, API_A_RECORD_RESOURCE_ID, apiARecordProps);

//  Configure CORS options for httpApi
        final CorsPreflightOptions httpApiPropsCorsOptions = CorsPreflightOptions.builder()
                                                                                 .allowCredentials(CORS_ALLOW_CREDENTIALS)
                                                                                 .allowMethods(ApiFunction.getAllowedMethods()
                                                                                                          .stream()
                                                                                                          .toList())
                                                                                 .allowOrigins(CORS_ALLOW_ORIGIN)
                                                                                 .maxAge(CORS_MAX_AGE)
                                                                                 .allowHeaders(CORS_ALLOW_HEADERS)
                                                                                 .build();
//  Configure HttpApi Options
        final HttpApiProps httpApiProps = HttpApiProps.builder()
                                                      .corsPreflight(httpApiPropsCorsOptions)
                                                      .createDefaultStage(FALSE)
                                                      .disableExecuteApiEndpoint(HTTP_API_DISABLE_EXECUTE_API_ENDPOINT)
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
            final IFunction function = Function.fromFunctionArn(this, func.functionName(), importValue(func.arnExportName()));
            final HttpLambdaIntegration httpLambdaIntegration = new HttpLambdaIntegration(func.httpIntegrationId(), function);
//      Add Lambda as HttpIntegration to HttpApi
            httpApi.addRoutes(AddRoutesOptions.builder()
                                              .authorizationScopes(HTTP_API_ROUTE_AUTHORIZATION_SCOPES)
                                              .authorizer(userPoolAuthorizer)
                                              .path(func.endpoint())
                                              .methods(func.methods())
                                              .integration(httpLambdaIntegration)
                                              .build());

//      TODO: More Programmatic Solution Available Once [this issue](https://github.com/aws/aws-cdk/issues/23301) is resolved
//      Parts of Execute-Api Arn: https://docs.aws.amazon.com/apigateway/latest/developerguide/arn-format-reference.html#apigateway-execute-api-arns
            final String sourceArn = "arn:aws:execute-api:%s:%s:%s/*/*%s".formatted(FamilyDirectoryCdkApp.DEFAULT_REGION, FamilyDirectoryCdkApp.DEFAULT_ACCOUNT, httpApi.getApiId(), func.endpoint());

            new CfnPermission(this, "Allow%sInvoke%s".formatted(HTTP_API_RESOURCE_ID, func.name()), CfnPermissionProps.builder()
                                                                                                                      .action("lambda:InvokeFunction")
                                                                                                                      .functionName(function.getFunctionArn())
                                                                                                                      .principal("apigateway.amazonaws.com")
                                                                                                                      .sourceArn(sourceArn)
                                                                                                                      .build());
        }

        httpApi.addStage(HTTP_API_PUBLIC_STAGE_ID, HttpStageOptions.builder()
                                                                   .stageName(HTTP_API_PUBLIC_STAGE_ID)
                                                                   .autoDeploy(HTTP_API_PUBLIC_STAGE_AUTO_DEPLOY)
                                                                   .domainMapping(DomainMappingOptions.builder()
                                                                                                      .domainName(apiDomainName)
                                                                                                      .build())
                                                                   .build());
    }
}
