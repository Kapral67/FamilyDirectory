package org.familydirectory.cdk.test.cognito;

import java.util.List;
import java.util.Map;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoUsEastOneStack;
import org.familydirectory.cdk.customresource.SSMParameterReader;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.familydirectory.cdk.lambda.construct.utility.LambdaFunctionConstructUtility;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Capture;
import software.amazon.awscdk.assertions.Match;
import software.amazon.awscdk.assertions.Template;
import software.amazon.awssdk.regions.Region;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awscdk.assertions.Match.objectEquals;
import static software.amazon.awscdk.assertions.Match.objectLike;

public
class FamilyDirectoryCognitoUsEastOneStackTest {

    private static final String HOSTED_ZONE_ID_PARAMETER_NAME = "%sParameter".formatted(FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME);

    @Test
    public
    void testStack () {
        final App app = new App();

        final FamilyDirectoryCognitoUsEastOneStack stack = new FamilyDirectoryCognitoUsEastOneStack(app, FamilyDirectoryCdkApp.COGNITO_US_EAST_1_STACK_NAME, StackProps.builder()
                                                                                                                                                                       .env(FamilyDirectoryCdkApp.US_EAST_1_ENV)
                                                                                                                                                                       .stackName(
                                                                                                                                                                               FamilyDirectoryCdkApp.COGNITO_US_EAST_1_STACK_NAME)
                                                                                                                                                                       .build());

        final Template template = Template.fromStack(stack);

        final Map<String, Map<String, List<Map<String, String>>>> ssmReaderIamPolicy = singletonMap("PolicyDocument", singletonMap("Statement", singletonList(
                Map.of("Action", "ssm:GetParameter", "Effect", "Allow", "Resource", LambdaFunctionConstructUtility.GLOBAL_RESOURCE))));

        if (!FamilyDirectoryCdkApp.DEFAULT_REGION.equals(FamilyDirectoryCognitoUsEastOneStack.REGION)) {
            final Capture ssmReaderCreateCapture = new Capture();
            final Capture ssmReaderUpdateCapture = new Capture();
            final Map<String, Map<String, Object>> customResourceMap = template.findResources("Custom::AWS", objectLike(
                    singletonMap("Properties", Map.of("Create", ssmReaderCreateCapture, "Update", ssmReaderUpdateCapture))));
            assertEquals(1, customResourceMap.size());
            assertTrue(
                    SSMParameterReader.validateTemplate(ssmReaderCreateCapture.asString(), FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME, Region.of(FamilyDirectoryCdkApp.DEFAULT_REGION)));
            assertTrue(
                    SSMParameterReader.validateTemplate(ssmReaderUpdateCapture.asString(), FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME, Region.of(FamilyDirectoryCdkApp.DEFAULT_REGION)));
            final String ssmParameterReaderId = customResourceMap.entrySet()
                                                                 .iterator()
                                                                 .next()
                                                                 .getKey();
            template.hasResourceProperties("AWS::IAM::Policy", objectLike(ssmReaderIamPolicy));
            template.hasResourceProperties("AWS::CertificateManager::Certificate", objectLike(Map.of("DomainName", FamilyDirectoryCognitoStack.COGNITO_DOMAIN_NAME, "DomainValidationOptions",
                                                                                                     singletonList(Map.of("DomainName", FamilyDirectoryCognitoStack.COGNITO_DOMAIN_NAME,
                                                                                                                          FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME,
                                                                                                                          singletonMap("Fn::GetAtt", List.of(ssmParameterReaderId,
                                                                                                                                                             SSMParameterReader.SSM_PARAMETER_READER_DATA_PATH)))),
                                                                                                     "ValidationMethod", "DNS")));
        } else {
            assertTrue(template.findResources("Custom::AWS", objectLike(singletonMap("Properties", Map.of("Create", Match.anyValue(), "Update", Match.anyValue()))))
                               .isEmpty());
            assertTrue(template.findResources("AWS::IAM::Policy", objectLike(singletonMap("Properties", ssmReaderIamPolicy)))
                               .isEmpty());
            template.hasParameter(HOSTED_ZONE_ID_PARAMETER_NAME,
                                  objectEquals(Map.of("Type", "AWS::SSM::Parameter::Value<String>", "Default", FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME)));
            template.hasResourceProperties("AWS::CertificateManager::Certificate", objectLike(Map.of("DomainName", FamilyDirectoryCognitoStack.COGNITO_DOMAIN_NAME, "DomainValidationOptions",
                                                                                                     singletonList(Map.of("DomainName", FamilyDirectoryCognitoStack.COGNITO_DOMAIN_NAME,
                                                                                                                          FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME,
                                                                                                                          singletonMap("Ref", HOSTED_ZONE_ID_PARAMETER_NAME))), "ValidationMethod",
                                                                                                     "DNS")));
        }

        final Capture cognitoCertificateArnCapture = new Capture();
        template.hasResourceProperties("AWS::SSM::Parameter", objectLike(
                Map.of("Name", FamilyDirectoryCognitoUsEastOneStack.COGNITO_CERTIFICATE_ARN_PARAMETER_NAME, "Type", "String", "Value", singletonMap("Ref", cognitoCertificateArnCapture))));
        assertFalse(cognitoCertificateArnCapture.asString()
                                                .isBlank());
    }
}
