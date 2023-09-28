package org.familydirectory.assets.domain;

import static java.lang.String.format;
import static java.lang.System.getenv;

public final
class DomainAssets {
    public static final String HOSTED_ZONE_RESOURCE_ID = "HostedZone";
    public static final String HOSTED_ZONE_NAME = getenv("ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME");
    public static final String HOSTED_ZONE_ID_EXPORT_NAME = format("%sId", HOSTED_ZONE_RESOURCE_ID);
    public static final String API_CERTIFICATE_RESOURCE_ID = "ApiCertificate";
    public static final String API_CERTIFICATE_NAME = format("%s-%s", HOSTED_ZONE_NAME, API_CERTIFICATE_RESOURCE_ID);
    public static final String API_CERTIFICATE_ARN_EXPORT_NAME = format("%sArn", API_CERTIFICATE_RESOURCE_ID);
    public static final String API_DOMAIN_NAME_RESOURCE_ID = "ApiDomainName";
    public static final String API_DOMAIN_NAME = format("%s.%s", getenv("ORG_FAMILYDIRECTORY_API_SUBDOMAIN_NAME"), HOSTED_ZONE_NAME);
    public static final String COGNITO_CERTIFICATE_RESOURCE_ID = "CognitoCertificate";
    public static final String COGNITO_CERTIFICATE_NAME = format("%s-%s", HOSTED_ZONE_NAME, COGNITO_CERTIFICATE_RESOURCE_ID);
    public static final String COGNITO_CERTIFICATE_ARN_EXPORT_NAME = format("%sArn", COGNITO_CERTIFICATE_RESOURCE_ID);
    public static final String COGNITO_DOMAIN_NAME_RESOURCE_ID = "CognitoDomainName";
    public static final String COGNITO_DOMAIN_NAME = format("%s.%s", getenv("ORG_FAMILYDIRECTORY_COGNITO_SUBDOMAIN_NAME"), HOSTED_ZONE_NAME);
    public static final String COGNITO_SIGNIN_URL_EXPORT_NAME = "CognitoSignInUrl";

    private
    DomainAssets () {
    }
}
