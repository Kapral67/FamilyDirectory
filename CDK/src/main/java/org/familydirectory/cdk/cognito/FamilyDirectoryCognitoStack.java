package org.familydirectory.cdk.cognito;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.familydirectory.assets.lambda.function.models.LambdaFunctionModel;
import org.familydirectory.assets.lambda.function.trigger.enums.TriggerFunction;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.customresource.SSMParameterReader;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.familydirectory.cdk.lambda.construct.utility.LambdaFunctionConstructUtility;
import org.familydirectory.cdk.ses.FamilyDirectorySesStack;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.ICertificate;
import software.amazon.awscdk.services.cognito.AccountRecovery;
import software.amazon.awscdk.services.cognito.AdvancedSecurityMode;
import software.amazon.awscdk.services.cognito.AuthFlow;
import software.amazon.awscdk.services.cognito.AutoVerifiedAttrs;
import software.amazon.awscdk.services.cognito.ClientAttributes;
import software.amazon.awscdk.services.cognito.CustomDomainOptions;
import software.amazon.awscdk.services.cognito.KeepOriginalAttrs;
import software.amazon.awscdk.services.cognito.Mfa;
import software.amazon.awscdk.services.cognito.OAuthFlows;
import software.amazon.awscdk.services.cognito.OAuthScope;
import software.amazon.awscdk.services.cognito.OAuthSettings;
import software.amazon.awscdk.services.cognito.PasswordPolicy;
import software.amazon.awscdk.services.cognito.SignInAliases;
import software.amazon.awscdk.services.cognito.SignInUrlOptions;
import software.amazon.awscdk.services.cognito.StandardAttribute;
import software.amazon.awscdk.services.cognito.StandardAttributes;
import software.amazon.awscdk.services.cognito.StandardAttributesMask;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClient;
import software.amazon.awscdk.services.cognito.UserPoolClientIdentityProvider;
import software.amazon.awscdk.services.cognito.UserPoolClientOptions;
import software.amazon.awscdk.services.cognito.UserPoolDomain;
import software.amazon.awscdk.services.cognito.UserPoolDomainOptions;
import software.amazon.awscdk.services.cognito.UserPoolEmail;
import software.amazon.awscdk.services.cognito.UserPoolOperation;
import software.amazon.awscdk.services.cognito.UserPoolProps;
import software.amazon.awscdk.services.cognito.UserPoolSESOptions;
import software.amazon.awscdk.services.cognito.UserVerificationConfig;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.ARecordProps;
import software.amazon.awscdk.services.route53.IPublicHostedZone;
import software.amazon.awscdk.services.route53.PublicHostedZone;
import software.amazon.awscdk.services.route53.PublicHostedZoneAttributes;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.UserPoolDomainTarget;
import software.amazon.awscdk.services.ses.EmailIdentity;
import software.amazon.awscdk.services.ses.IEmailIdentity;
import software.amazon.awscdk.services.ssm.IStringParameter;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.amazon.awssdk.regions.Region;
import software.constructs.Construct;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.System.getenv;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static software.amazon.awscdk.Duration.days;
import static software.amazon.awscdk.Fn.importValue;
import static software.amazon.awscdk.services.cognito.UserPoolClientIdentityProvider.COGNITO;
import static software.amazon.awscdk.services.cognito.VerificationEmailStyle.LINK;

public
class FamilyDirectoryCognitoStack extends Stack {
    public static final String COGNITO_DOMAIN_NAME_RESOURCE_ID = "CognitoDomainName";
    public static final String COGNITO_A_RECORD_RESOURCE_ID = "CognitoARecord";
    public static final String COGNITO_USER_POOL_CLIENT_RESOURCE_ID = "UserPoolClient";
    public static final String COGNITO_USER_POOL_RESOURCE_ID = "UserPool";
    public static final String COGNITO_USER_POOL_ID_EXPORT_NAME = "%sId".formatted(COGNITO_USER_POOL_RESOURCE_ID);
    public static final String COGNITO_USER_POOL_CLIENT_ID_EXPORT_NAME = "%sId".formatted(COGNITO_USER_POOL_CLIENT_RESOURCE_ID);
    public static final String COGNITO_DOMAIN_NAME = "%s.%s".formatted(getenv("ORG_FAMILYDIRECTORY_COGNITO_SUBDOMAIN_NAME"), FamilyDirectoryDomainStack.HOSTED_ZONE_NAME);
    public static final String COGNITO_FROM_EMAIL_ADDRESS = "no-reply@%s".formatted(FamilyDirectoryDomainStack.HOSTED_ZONE_NAME);
    public static final String COGNITO_REPLY_TO_EMAIL_ADDRESS = getenv("ORG_FAMILYDIRECTORY_COGNITO_REPLY_TO_EMAIL_ADDRESS");
    public static final String COGNITO_SIGN_IN_URL_EXPORT_NAME = "CognitoSignInUrl";
    public static final List<UserPoolClientIdentityProvider> COGNITO_USER_POOL_CLIENT_IDENTITY_PROVIDERS = singletonList(COGNITO);
    public static final Number COGNITO_MIN_PASSWORD_LENGTH = 8;
    public static final boolean COGNITO_REQUIRE_LOWERCASE_IN_PASSWORD = true;
    public static final boolean COGNITO_REQUIRE_DIGITS_IN_PASSWORD = true;
    public static final boolean COGNITO_REQUIRE_SYMBOLS_IN_PASSWORD = true;
    public static final boolean COGNITO_REQUIRE_UPPERCASE_IN_PASSWORD = true;
    public static final Number COGNITO_TEMPORARY_PASSWORD_VALIDITY_DAYS = 15;
    public static final boolean COGNITO_EMAIL_REQUIRE_ATTRIBUTE = true;
    public static final boolean COGNITO_EMAIL_MUTABLE_ATTRIBUTE = true;
    public static final boolean COGNITO_SIGN_IN_CASE_SENSITIVE = false;
    public static final boolean COGNITO_SELF_SIGN_UP_ENABLED = true;
    public static final boolean COGNITO_USER_POOL_CLIENT_GENERATE_SECRET = false;
    private static final StandardAttribute EMAIL_ATTRIBUTE_PROPERTIES = StandardAttribute.builder()
                                                                                         .required(COGNITO_EMAIL_REQUIRE_ATTRIBUTE)
                                                                                         .mutable(COGNITO_EMAIL_MUTABLE_ATTRIBUTE)
                                                                                         .build();

    public
    FamilyDirectoryCognitoStack (final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);
        final IStringParameter hostedZoneId = StringParameter.fromStringParameterName(this, FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME,
                                                                                      FamilyDirectoryDomainStack.HOSTED_ZONE_ID_PARAMETER_NAME);
        final PublicHostedZoneAttributes hostedZoneAttrs = PublicHostedZoneAttributes.builder()
                                                                                     .hostedZoneId(hostedZoneId.getStringValue())
                                                                                     .zoneName(FamilyDirectoryDomainStack.HOSTED_ZONE_NAME)
                                                                                     .build();
        final IPublicHostedZone hostedZone = PublicHostedZone.fromPublicHostedZoneAttributes(this, FamilyDirectoryDomainStack.HOSTED_ZONE_RESOURCE_ID, hostedZoneAttrs);
        final IEmailIdentity emailIdentity = EmailIdentity.fromEmailIdentityName(this, FamilyDirectorySesStack.SES_EMAIL_IDENTITY_RESOURCE_ID,
                                                                                 importValue(FamilyDirectorySesStack.SES_EMAIL_IDENTITY_NAME_EXPORT_NAME));

//  Cognito Trigger Lambda Functions
        final Map<LambdaFunctionModel, Function> cognitoTriggerLambdaFunctions = LambdaFunctionConstructUtility.constructFunctionMap(this, Arrays.asList(TriggerFunction.values()), hostedZone,
                                                                                                                                     emailIdentity, null, null);

        final UserPoolProps userPoolProps = UserPoolProps.builder()
                                                         .accountRecovery(AccountRecovery.EMAIL_ONLY)
                                                         .advancedSecurityMode(AdvancedSecurityMode.OFF)
                                                         .autoVerify(AutoVerifiedAttrs.builder()
                                                                                      .email(TRUE)
                                                                                      .phone(FALSE)
                                                                                      .build())
                                                         .deletionProtection(TRUE)
                                                         .email(UserPoolEmail.withSES(UserPoolSESOptions.builder()
                                                                                                        .configurationSetName(FamilyDirectorySesStack.SES_CONFIGURATION_SET_NAME)
                                                                                                        .fromEmail(COGNITO_FROM_EMAIL_ADDRESS)
                                                                                                        .replyTo(COGNITO_REPLY_TO_EMAIL_ADDRESS)
                                                                                                        .sesVerifiedDomain(FamilyDirectoryDomainStack.HOSTED_ZONE_NAME)
                                                                                                        .build()))
                                                         .enableSmsRole(FALSE)
                                                         .keepOriginal(KeepOriginalAttrs.builder()
                                                                                        .email(TRUE)
                                                                                        .phone(FALSE)
                                                                                        .build())
                                                         .mfa(Mfa.OFF)
                                                         .passwordPolicy(PasswordPolicy.builder()
                                                                                       .minLength(COGNITO_MIN_PASSWORD_LENGTH)
                                                                                       .requireLowercase(COGNITO_REQUIRE_LOWERCASE_IN_PASSWORD)
                                                                                       .requireDigits(COGNITO_REQUIRE_DIGITS_IN_PASSWORD)
                                                                                       .requireSymbols(COGNITO_REQUIRE_SYMBOLS_IN_PASSWORD)
                                                                                       .requireUppercase(COGNITO_REQUIRE_UPPERCASE_IN_PASSWORD)
                                                                                       .tempPasswordValidity(days(COGNITO_TEMPORARY_PASSWORD_VALIDITY_DAYS))
                                                                                       .build())
                                                         .removalPolicy(RemovalPolicy.DESTROY)
                                                         .selfSignUpEnabled(COGNITO_SELF_SIGN_UP_ENABLED)
                                                         .signInAliases(SignInAliases.builder()
                                                                                     .username(FALSE)
                                                                                     .preferredUsername(FALSE)
                                                                                     .phone(FALSE)
                                                                                     .email(TRUE)
                                                                                     .build())
                                                         .signInCaseSensitive(COGNITO_SIGN_IN_CASE_SENSITIVE)
                                                         .standardAttributes(StandardAttributes.builder()
                                                                                               .email(EMAIL_ATTRIBUTE_PROPERTIES)
                                                                                               .build())
                                                         .userVerification(UserVerificationConfig.builder()
                                                                                                 .emailStyle(LINK)
                                                                                                 .build())
                                                         .build();

        final UserPool userPool = new UserPool(this, COGNITO_USER_POOL_RESOURCE_ID, userPoolProps);
        new CfnOutput(this, COGNITO_USER_POOL_ID_EXPORT_NAME, CfnOutputProps.builder()
                                                                            .value(userPool.getUserPoolId())
                                                                            .exportName(COGNITO_USER_POOL_ID_EXPORT_NAME)
                                                                            .build());

//  Add Triggers to User Pool
        userPool.addTrigger(UserPoolOperation.PRE_SIGN_UP, cognitoTriggerLambdaFunctions.get(TriggerFunction.PRE_SIGN_UP));
        userPool.addTrigger(UserPoolOperation.POST_CONFIRMATION, cognitoTriggerLambdaFunctions.get(TriggerFunction.POST_CONFIRMATION));

//  Configure User Pool Client
        final UserPoolClientOptions userPoolClientOptions = UserPoolClientOptions.builder()
                                                                                 .authFlows(AuthFlow.builder()
                                                                                                    .userSrp(TRUE)
                                                                                                    .build())
                                                                                 .generateSecret(COGNITO_USER_POOL_CLIENT_GENERATE_SECRET)
                                                                                 .oAuth(OAuthSettings.builder()
                                                                                                     .callbackUrls(singletonList(FamilyDirectoryCdkApp.HTTPS_PREFIX + hostedZone.getZoneName()))
                                                                                                     .logoutUrls(singletonList(FamilyDirectoryCdkApp.HTTPS_PREFIX + hostedZone.getZoneName()))
                                                                                                     .flows(OAuthFlows.builder()
                                                                                                                      .authorizationCodeGrant(TRUE)
                                                                                                                      .build())
                                                                                                     .scopes(List.of(OAuthScope.EMAIL, OAuthScope.OPENID, OAuthScope.COGNITO_ADMIN, OAuthScope.PROFILE))
                                                                                                     .build())
                                                                                 .preventUserExistenceErrors(TRUE)
                                                                                 .readAttributes(new ClientAttributes().withStandardAttributes(StandardAttributesMask.builder()
                                                                                                                                                                     .email(TRUE)
                                                                                                                                                                     .emailVerified(TRUE)
                                                                                                                                                                     .build()))
                                                                                 .supportedIdentityProviders(COGNITO_USER_POOL_CLIENT_IDENTITY_PROVIDERS)
                                                                                 .writeAttributes(new ClientAttributes().withStandardAttributes(StandardAttributesMask.builder()
                                                                                                                                                                      .email(TRUE)
                                                                                                                                                                      .emailVerified(FALSE)
                                                                                                                                                                      .build()))
                                                                                 .build();
        final UserPoolClient userPoolClient = userPool.addClient(COGNITO_USER_POOL_CLIENT_RESOURCE_ID, userPoolClientOptions);
        new CfnOutput(this, COGNITO_USER_POOL_CLIENT_ID_EXPORT_NAME, CfnOutputProps.builder()
                                                                                   .value(userPoolClient.getUserPoolClientId())
                                                                                   .exportName(COGNITO_USER_POOL_CLIENT_ID_EXPORT_NAME)
                                                                                   .build());

//  Cognito Domain
        final String cognitoCertificateArn;
        if (ofNullable(stackProps.getEnv()).map(Environment::getRegion)
                                           .filter(region -> region.equals(FamilyDirectoryCognitoUsEastOneStack.REGION))
                                           .isPresent())
        {
            cognitoCertificateArn = StringParameter.fromStringParameterName(this, FamilyDirectoryCognitoUsEastOneStack.COGNITO_CERTIFICATE_ARN_PARAMETER_NAME,
                                                                            FamilyDirectoryCognitoUsEastOneStack.COGNITO_CERTIFICATE_ARN_PARAMETER_NAME)
                                                   .getStringValue();
        } else {
            cognitoCertificateArn = new SSMParameterReader(this, SSMParameterReader.SSM_PARAMETER_READER_RESOURCE_ID, FamilyDirectoryCognitoUsEastOneStack.COGNITO_CERTIFICATE_ARN_PARAMETER_NAME,
                                                           Region.of(FamilyDirectoryCognitoUsEastOneStack.REGION)).toString();
        }
        final ICertificate cognitoCertificate = Certificate.fromCertificateArn(this, FamilyDirectoryCognitoUsEastOneStack.COGNITO_CERTIFICATE_RESOURCE_ID, cognitoCertificateArn);
        final CustomDomainOptions cognitoCustomDomainOptions = CustomDomainOptions.builder()
                                                                                  .certificate(cognitoCertificate)
                                                                                  .domainName(COGNITO_DOMAIN_NAME)
                                                                                  .build();
        final UserPoolDomainOptions userPoolDomainOptions = UserPoolDomainOptions.builder()
                                                                                 .customDomain(cognitoCustomDomainOptions)
                                                                                 .build();
        final UserPoolDomain userPoolDomain = userPool.addDomain(COGNITO_DOMAIN_NAME_RESOURCE_ID, userPoolDomainOptions);
        final UserPoolDomainTarget userPoolDomainTarget = new UserPoolDomainTarget(userPoolDomain);

        final ARecordProps cognitoARecordProps = ARecordProps.builder()
                                                             .zone(hostedZone)
                                                             .recordName(COGNITO_DOMAIN_NAME)
                                                             .target(RecordTarget.fromAlias(userPoolDomainTarget))
                                                             .build();
        new ARecord(this, COGNITO_A_RECORD_RESOURCE_ID, cognitoARecordProps);
        final String userPoolSignInUrl = userPoolDomain.signInUrl(userPoolClient, SignInUrlOptions.builder()
                                                                                                  .redirectUri(FamilyDirectoryCdkApp.HTTPS_PREFIX + hostedZone.getZoneName())
                                                                                                  .build());
        new CfnOutput(this, COGNITO_SIGN_IN_URL_EXPORT_NAME, CfnOutputProps.builder()
                                                                           .value(userPoolSignInUrl)
                                                                           .exportName(COGNITO_SIGN_IN_URL_EXPORT_NAME)
                                                                           .build());
    }
}
