package org.familydirectory.cdk.test.apigateway;

import java.util.List;
import java.util.Map;
import org.familydirectory.assets.lambda.function.api.enums.ApiFunction;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.apigateway.FamilyDirectoryApiGatewayStack;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Template;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awscdk.assertions.Match.objectLike;

public
class FamilyDirectoryApiGatewayStackTest {

    private static final String HOSTED_ZONE_ID_PARAMETER_NAME = "%sParameter".formatted(FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME);
    private static final String FULL_API_DOMAIN_NAME = "%s.".formatted(FamilyDirectoryApiGatewayStack.API_DOMAIN_NAME);

    @Test
    public
    void testStack () {
        final App app = new App();

        final FamilyDirectoryApiGatewayStack stack = new FamilyDirectoryApiGatewayStack(app, FamilyDirectoryCdkApp.API_STACK_NAME, StackProps.builder()
                                                                                                                                             .env(FamilyDirectoryCdkApp.DEFAULT_ENV)
                                                                                                                                             .stackName(FamilyDirectoryCdkApp.API_STACK_NAME)
                                                                                                                                             .build());

        final Template template = Template.fromStack(stack);

        template.hasParameter(HOSTED_ZONE_ID_PARAMETER_NAME, objectLike(Map.of("Type", "AWS::SSM::Parameter::Value<String>", "Default", FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME)));

        final Map<String, Map<String, Object>> certificateMap = template.findResources("AWS::CertificateManager::Certificate", objectLike(singletonMap("Properties", Map.of("DomainName",
                                                                                                                                                                            FamilyDirectoryApiGatewayStack.API_DOMAIN_NAME,
                                                                                                                                                                            "DomainValidationOptions",
                                                                                                                                                                            singletonList(
                                                                                                                                                                                    Map.of("DomainName",
                                                                                                                                                                                           FamilyDirectoryApiGatewayStack.API_DOMAIN_NAME,
                                                                                                                                                                                           FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME,
                                                                                                                                                                                           singletonMap(
                                                                                                                                                                                                   "Ref",
                                                                                                                                                                                                   HOSTED_ZONE_ID_PARAMETER_NAME))),
                                                                                                                                                                            "Tags", singletonList(
                        Map.of("Key", "Name", "Value", FamilyDirectoryApiGatewayStack.API_CERTIFICATE_NAME)), "ValidationMethod", "DNS"))));
        assertEquals(1, certificateMap.size());
        final String certificateId = certificateMap.entrySet()
                                                   .iterator()
                                                   .next()
                                                   .getKey();

        final Map<String, Map<String, Object>> domainNameMap = template.findResources("AWS::ApiGatewayV2::DomainName", objectLike(singletonMap("Properties", Map.of("DomainName",
                                                                                                                                                                    FamilyDirectoryApiGatewayStack.API_DOMAIN_NAME,
                                                                                                                                                                    "DomainNameConfigurations",
                                                                                                                                                                    singletonList(
                                                                                                                                                                            Map.of("CertificateArn",
                                                                                                                                                                                   singletonMap("Ref",
                                                                                                                                                                                                certificateId),
                                                                                                                                                                                   "EndpointType",
                                                                                                                                                                                   "REGIONAL",
                                                                                                                                                                                   "SecurityPolicy",
                                                                                                                                                                                   "TLS_1_2"))))));
        assertEquals(1, domainNameMap.size());
        final String domainNameId = domainNameMap.entrySet()
                                                 .iterator()
                                                 .next()
                                                 .getKey();

        template.hasResourceProperties("AWS::Route53::RecordSet", objectLike(Map.of("AliasTarget",
                                                                                    Map.of("DNSName", singletonMap("Fn::GetAtt", List.of(domainNameId, "RegionalDomainName")), "HostedZoneId",
                                                                                           singletonMap("Fn::GetAtt", List.of(domainNameId, "RegionalHostedZoneId"))),
                                                                                    FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME, singletonMap("Ref", HOSTED_ZONE_ID_PARAMETER_NAME),
                                                                                    "Name", FULL_API_DOMAIN_NAME, "Type", "A")));

        final Map<String, Map<String, Object>> apiMap = template.findResources("AWS::ApiGatewayV2::Api", objectLike(singletonMap("Properties", Map.of("CorsConfiguration", Map.of("AllowCredentials",
                                                                                                                                                                                  FamilyDirectoryApiGatewayStack.CORS_ALLOW_CREDENTIALS,
                                                                                                                                                                                  "AllowHeaders",
                                                                                                                                                                                  FamilyDirectoryApiGatewayStack.CORS_ALLOW_HEADERS,
                                                                                                                                                                                  "AllowMethods",
                                                                                                                                                                                  ApiFunction.getAllowedMethods()
                                                                                                                                                                                             .stream()
                                                                                                                                                                                             .toList(),
                                                                                                                                                                                  "AllowOrigins",
                                                                                                                                                                                  FamilyDirectoryApiGatewayStack.CORS_ALLOW_ORIGIN,
                                                                                                                                                                                  "MaxAge",
                                                                                                                                                                                  FamilyDirectoryApiGatewayStack.CORS_MAX_AGE.toSeconds()),
                                                                                                                                                      "DisableExecuteApiEndpoint",
                                                                                                                                                      FamilyDirectoryApiGatewayStack.HTTP_API_DISABLE_EXECUTE_API_ENDPOINT,
                                                                                                                                                      "Name",
                                                                                                                                                      FamilyDirectoryApiGatewayStack.HTTP_API_RESOURCE_ID,
                                                                                                                                                      "ProtocolType", "HTTP"))));
        assertEquals(1, apiMap.size());
        final String apiId = apiMap.entrySet()
                                   .iterator()
                                   .next()
                                   .getKey();

        final Map<String, Map<String, Object>> authorizerMap = template.findResources("AWS::ApiGatewayV2::Authorizer", objectLike(singletonMap("Properties", Map.of("ApiId", singletonMap("Ref", apiId),
                                                                                                                                                                    "AuthorizerType", "JWT",
                                                                                                                                                                    "IdentitySource", singletonList(
                        "$request.header.Authorization"), "JwtConfiguration", Map.of("Audience", singletonList(
                        singletonMap("Fn::ImportValue", FamilyDirectoryCognitoStack.COGNITO_USER_POOL_CLIENT_ID_EXPORT_NAME)), "Issuer", singletonMap("Fn::Join", List.of("",
                                                                                                                                                                          List.of(("https://cognito" +
                                                                                                                                                                                   "-idp.%s.amazonaws" +
                                                                                                                                                                                   ".com/").formatted(
                                                                                                                                                                                          FamilyDirectoryCdkApp.DEFAULT_REGION),
                                                                                                                                                                                  singletonMap(
                                                                                                                                                                                          "Fn::ImportValue",
                                                                                                                                                                                          FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME))))),
                                                                                                                                                                    "Name",
                                                                                                                                                                    FamilyDirectoryApiGatewayStack.API_COGNITO_AUTHORIZER_RESOURCE_ID))));
        assertEquals(1, authorizerMap.size());
        final String authorizerId = authorizerMap.entrySet()
                                                 .iterator()
                                                 .next()
                                                 .getKey();

        for (final ApiFunction func : ApiFunction.values()) {
            final Map<String, Map<String, Object>> integrationMap = template.findResources("AWS::ApiGatewayV2::Integration", objectLike(singletonMap("Properties",
                                                                                                                                                     Map.of("ApiId", singletonMap("Ref", apiId),
                                                                                                                                                            "IntegrationType", "AWS_PROXY",
                                                                                                                                                            "IntegrationUri",
                                                                                                                                                            singletonMap("Fn::ImportValue",
                                                                                                                                                                         func.arnExportName()),
                                                                                                                                                            "PayloadFormatVersion", "2.0"))));
            assertEquals(1, integrationMap.size());
            final String integrationId = integrationMap.entrySet()
                                                       .iterator()
                                                       .next()
                                                       .getKey();

            template.hasResourceProperties("AWS::ApiGatewayV2::Route", objectLike(
                    Map.of("ApiId", singletonMap("Ref", apiId), "AuthorizationScopes", FamilyDirectoryApiGatewayStack.HTTP_API_ROUTE_AUTHORIZATION_SCOPES, "AuthorizationType", "JWT", "AuthorizerId",
                           singletonMap("Ref", authorizerId), "RouteKey", "%s %s".formatted(func.methods()
                                                                                                .getFirst()
                                                                                                .name(), func.endpoint()), "Target",
                           singletonMap("Fn::Join", List.of("", List.of("integrations/", singletonMap("Ref", integrationId)))))));

            template.hasResourceProperties("AWS::Lambda::Permission", objectLike(
                    Map.of("Action", FamilyDirectoryApiGatewayStack.LAMBDA_INVOKE_PERMISSION_ACTION, "FunctionName", singletonMap("Fn::ImportValue", func.arnExportName()), "Principal",
                           FamilyDirectoryApiGatewayStack.API_GATEWAY_PERMISSION_PRINCIPAL, "SourceArn", singletonMap("Fn::Join", List.of("",
                                                                                                                                          List.of(FamilyDirectoryApiGatewayStack.EXECUTE_API_ARN_PREFIX,
                                                                                                                                                  singletonMap("Ref", apiId),
                                                                                                                                                  FamilyDirectoryApiGatewayStack.getExecuteApiSuffix(
                                                                                                                                                          func)))))));
        }

        template.hasResourceProperties("AWS::ApiGatewayV2::Stage", objectLike(
                Map.of("ApiId", singletonMap("Ref", apiId), "AutoDeploy", FamilyDirectoryApiGatewayStack.HTTP_API_PUBLIC_STAGE_AUTO_DEPLOY, "StageName",
                       FamilyDirectoryApiGatewayStack.HTTP_API_PUBLIC_STAGE_ID)));

        template.hasResourceProperties("AWS::ApiGatewayV2::ApiMapping", objectLike(
                Map.of("ApiId", singletonMap("Ref", apiId), "DomainName", singletonMap("Ref", domainNameId), "Stage", FamilyDirectoryApiGatewayStack.HTTP_API_PUBLIC_STAGE_ID)));
    }
}
