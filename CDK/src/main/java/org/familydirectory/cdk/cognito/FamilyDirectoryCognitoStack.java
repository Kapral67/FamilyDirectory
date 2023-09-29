package org.familydirectory.cdk.cognito;

import java.util.List;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateProps;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.cognito.AccountRecovery;
import software.amazon.awscdk.services.cognito.AdvancedSecurityMode;
import software.amazon.awscdk.services.cognito.AuthFlow;
import software.amazon.awscdk.services.cognito.AutoVerifiedAttrs;
import software.amazon.awscdk.services.cognito.ClientAttributes;
import software.amazon.awscdk.services.cognito.CustomDomainOptions;
import software.amazon.awscdk.services.cognito.KeepOriginalAttrs;
import software.amazon.awscdk.services.cognito.Mfa;
import software.amazon.awscdk.services.cognito.OAuthFlows;
import software.amazon.awscdk.services.cognito.OAuthSettings;
import software.amazon.awscdk.services.cognito.PasswordPolicy;
import software.amazon.awscdk.services.cognito.SignInAliases;
import software.amazon.awscdk.services.cognito.SignInUrlOptions;
import software.amazon.awscdk.services.cognito.StandardAttribute;
import software.amazon.awscdk.services.cognito.StandardAttributes;
import software.amazon.awscdk.services.cognito.StandardAttributesMask;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClient;
import software.amazon.awscdk.services.cognito.UserPoolClientOptions;
import software.amazon.awscdk.services.cognito.UserPoolDomain;
import software.amazon.awscdk.services.cognito.UserPoolDomainOptions;
import software.amazon.awscdk.services.cognito.UserPoolEmail;
import software.amazon.awscdk.services.cognito.UserPoolProps;
import software.amazon.awscdk.services.cognito.UserPoolTriggers;
import software.amazon.awscdk.services.cognito.UserVerificationConfig;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.ARecordProps;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.UserPoolDomainTarget;
import software.constructs.Construct;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.lang.System.getenv;
import static java.util.Collections.singletonList;
import static org.familydirectory.cdk.domain.FamilyDirectoryDomainStack.HOSTED_ZONE_ID_EXPORT_NAME;
import static org.familydirectory.cdk.domain.FamilyDirectoryDomainStack.HOSTED_ZONE_NAME;
import static org.familydirectory.cdk.domain.FamilyDirectoryDomainStack.HOSTED_ZONE_RESOURCE_ID;
import static software.amazon.awscdk.Duration.days;
import static software.amazon.awscdk.Fn.importValue;
import static software.amazon.awscdk.services.cognito.UserPoolClientIdentityProvider.COGNITO;
import static software.amazon.awscdk.services.cognito.VerificationEmailStyle.LINK;

public
class FamilyDirectoryCognitoStack extends Stack {

    public static final String COGNITO_CERTIFICATE_RESOURCE_ID = "CognitoCertificate";
    public static final String COGNITO_CERTIFICATE_NAME = format("%s-%s", HOSTED_ZONE_NAME, COGNITO_CERTIFICATE_RESOURCE_ID);
    public static final String COGNITO_CERTIFICATE_ARN_EXPORT_NAME = format("%sArn", COGNITO_CERTIFICATE_RESOURCE_ID);
    public static final String COGNITO_DOMAIN_NAME_RESOURCE_ID = "CognitoDomainName";
    public static final String COGNITO_A_RECORD_RESOURCE_ID = "CognitoARecord";
    public static final String COGNITO_USER_POOL_CLIENT_RESOURCE_ID = "UserPoolClient";
    public static final String COGNITO_USER_POOL_RESOURCE_ID = "UserPool";
    public static final String COGNITO_USER_POOL_ID_EXPORT_NAME = format("%sId", COGNITO_USER_POOL_RESOURCE_ID);
    public static final String COGNITO_USER_POOL_CLIENT_ID_EXPORT_NAME = format("%sId", COGNITO_USER_POOL_CLIENT_RESOURCE_ID);
    public static final String COGNITO_DOMAIN_NAME = format("%s.%s", getenv("ORG_FAMILYDIRECTORY_COGNITO_SUBDOMAIN_NAME"), HOSTED_ZONE_NAME);
    public static final String COGNITO_SIGNIN_URL_EXPORT_NAME = "CognitoSignInUrl";
    private static final StandardAttribute IMMUTABLE_REQUIRED_ATTRIBUTE = StandardAttribute.builder()
                                                                                           .required(TRUE)
                                                                                           .mutable(FALSE)
                                                                                           .build();
    private static final Duration TEMPORARY_PASSWORD_VALIDITY = days(15);
    private static final Number MIN_PASSWORD_LENGTH = 8;

    public
    FamilyDirectoryCognitoStack (final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        final IHostedZone hostedZone = HostedZone.fromHostedZoneId(this, HOSTED_ZONE_RESOURCE_ID, importValue(HOSTED_ZONE_ID_EXPORT_NAME));

        final UserPoolProps userPoolProps = UserPoolProps.builder()
                                                         .accountRecovery(AccountRecovery.EMAIL_ONLY)
                                                         .advancedSecurityMode(AdvancedSecurityMode.OFF)
                                                         .autoVerify(AutoVerifiedAttrs.builder()
                                                                                      .email(TRUE)
                                                                                      .phone(FALSE)
                                                                                      .build())
                                                         .deletionProtection(TRUE)
                                                         .email(UserPoolEmail.withCognito())
                                                         .enableSmsRole(FALSE)
                                                         .keepOriginal(KeepOriginalAttrs.builder()
                                                                                        .email(TRUE)
                                                                                        .phone(FALSE)
                                                                                        .build())
                                                         // TODO: CREATE PRE-SIGNUP LAMBDA
                                                         //// (MIGHT NOT BE NEEDED IF SELF-SIGN-UP IS DISABLED)
                                                         .lambdaTriggers(UserPoolTriggers.builder()
                                                                                         .preSignUp(null)
                                                                                         .build())
                                                         .mfa(Mfa.OFF)
                                                         .passwordPolicy(PasswordPolicy.builder()
                                                                                       .minLength(MIN_PASSWORD_LENGTH)
                                                                                       .requireLowercase(TRUE)
                                                                                       .requireDigits(TRUE)
                                                                                       .requireSymbols(TRUE)
                                                                                       .requireUppercase(TRUE)
                                                                                       .tempPasswordValidity(TEMPORARY_PASSWORD_VALIDITY)
                                                                                       .build())
                                                         .removalPolicy(RemovalPolicy.DESTROY)
                                                         .selfSignUpEnabled(FALSE)
                                                         .signInAliases(SignInAliases.builder()
                                                                                     .username(FALSE)
                                                                                     .preferredUsername(FALSE)
                                                                                     .phone(FALSE)
                                                                                     .email(TRUE)
                                                                                     .build())
                                                         .signInCaseSensitive(FALSE)
                                                         .standardAttributes(StandardAttributes.builder()
                                                                                               .email(IMMUTABLE_REQUIRED_ATTRIBUTE)
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
        final OAuthSettings userPoolClientOAuthSettings = OAuthSettings.builder()
                                                                       // TODO: ADD CALLBACK URLS
                                                                       .callbackUrls(List.of(""))
                                                                       // TODO: ADD LOGOUT URLS
                                                                       .logoutUrls(List.of(""))
                                                                       .flows(OAuthFlows.builder()
                                                                                        .authorizationCodeGrant(TRUE)
                                                                                        .build())
                                                                       .build();
        final ClientAttributes userPoolClientAttributes = new ClientAttributes().withStandardAttributes(StandardAttributesMask.builder()
                                                                                                                              .email(TRUE)
                                                                                                                              .emailVerified(TRUE)
                                                                                                                              .build());
        final UserPoolClientOptions userPoolClientOptions = UserPoolClientOptions.builder()
                                                                                 .authFlows(AuthFlow.builder()
                                                                                                    .userSrp(TRUE)
                                                                                                    .build())
                                                                                 .generateSecret(FALSE)
                                                                                 .oAuth(userPoolClientOAuthSettings)
                                                                                 .preventUserExistenceErrors(TRUE)
                                                                                 .readAttributes(userPoolClientAttributes)
                                                                                 .supportedIdentityProviders(singletonList(COGNITO))
                                                                                 .writeAttributes(userPoolClientAttributes)
                                                                                 .build();
        final UserPoolClient userPoolClient = userPool.addClient(COGNITO_USER_POOL_CLIENT_RESOURCE_ID, userPoolClientOptions);
        new CfnOutput(this, COGNITO_USER_POOL_CLIENT_ID_EXPORT_NAME, CfnOutputProps.builder()
                                                                                   .value(userPoolClient.getUserPoolClientId())
                                                                                   .exportName(COGNITO_USER_POOL_CLIENT_ID_EXPORT_NAME)
                                                                                   .build());
        // Cognito Certificate
        final CertificateProps cognitoCertificateProps = CertificateProps.builder()
                                                                         .certificateName(COGNITO_CERTIFICATE_NAME)
                                                                         .domainName(COGNITO_DOMAIN_NAME)
                                                                         .validation(CertificateValidation.fromDns(hostedZone))
                                                                         .build();
        final Certificate cognitoCertificate = new Certificate(this, COGNITO_CERTIFICATE_RESOURCE_ID, cognitoCertificateProps);
        new CfnOutput(this, COGNITO_CERTIFICATE_ARN_EXPORT_NAME, CfnOutputProps.builder()
                                                                               .value(cognitoCertificate.getCertificateArn())
                                                                               .exportName(COGNITO_CERTIFICATE_ARN_EXPORT_NAME)
                                                                               .build());
        final CustomDomainOptions cognitoCustomDomainOptions = CustomDomainOptions.builder()
                                                                                  .certificate(cognitoCertificate)
                                                                                  .domainName(COGNITO_DOMAIN_NAME)
                                                                                  .build();
        final UserPoolDomainOptions userPoolDomainOptions = UserPoolDomainOptions.builder()
                                                                                 .customDomain(cognitoCustomDomainOptions)
                                                                                 .build();
        final UserPoolDomain userPoolDomain = userPool.addDomain(COGNITO_DOMAIN_NAME_RESOURCE_ID, userPoolDomainOptions);
        final UserPoolDomainTarget userPoolDomainTarget = new UserPoolDomainTarget(userPoolDomain);
        // Cognito Domain
        final ARecordProps cognitoARecordProps = ARecordProps.builder()
                                                             .zone(hostedZone)
                                                             .recordName(COGNITO_DOMAIN_NAME)
                                                             .target(RecordTarget.fromAlias(userPoolDomainTarget))
                                                             .build();
        new ARecord(this, COGNITO_A_RECORD_RESOURCE_ID, cognitoARecordProps);
        final String userPoolSignInUrl = userPoolDomain.signInUrl(userPoolClient, SignInUrlOptions.builder()
                                                                                                  // TODO: CHANGE TO VIEW URL WHEN FRONTEND EXISTS
                                                                                                  .redirectUri(HOSTED_ZONE_NAME)
                                                                                                  .build());
        new CfnOutput(this, COGNITO_SIGNIN_URL_EXPORT_NAME, CfnOutputProps.builder()
                                                                          .value(userPoolSignInUrl)
                                                                          .exportName(COGNITO_SIGNIN_URL_EXPORT_NAME)
                                                                          .build());
    }
}
