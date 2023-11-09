package org.familydirectory.cdk.amplify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.apigateway.FamilyDirectoryApiGatewayStack;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.familydirectory.cdk.lambda.construct.utility.LambdaFunctionConstructUtility;
import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.customresources.AwsCustomResource;
import software.amazon.awscdk.customresources.AwsCustomResourcePolicy;
import software.amazon.awscdk.customresources.AwsCustomResourceProps;
import software.amazon.awscdk.customresources.AwsSdkCall;
import software.amazon.awscdk.customresources.PhysicalResourceId;
import software.amazon.awscdk.services.amplify.alpha.App;
import software.amazon.awscdk.services.amplify.alpha.AppProps;
import software.amazon.awscdk.services.amplify.alpha.BasicAuth;
import software.amazon.awscdk.services.amplify.alpha.Branch;
import software.amazon.awscdk.services.amplify.alpha.BranchOptions;
import software.amazon.awscdk.services.amplify.alpha.CustomRule;
import software.amazon.awscdk.services.amplify.alpha.Domain;
import software.amazon.awscdk.services.amplify.alpha.GitHubSourceCodeProvider;
import software.amazon.awscdk.services.amplify.alpha.Platform;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.constructs.Construct;
import static java.lang.System.getenv;
import static java.util.Collections.singletonList;
import static software.amazon.awscdk.Fn.importValue;

public
class FamilyDirectoryAmplifyStack extends Stack {
    public static final String AMPLIFY_ROOT_MEMBER_SURNAME_RESOURCE_ID = "RootMemberSurname";
    public static final List<String> AMPLIFY_ROOT_MEMBER_SURNAME_RESOURCE_POLICY_STATEMENT_ACTIONS = singletonList("dynamodb:GetItem");
    public static final DdbTable AMPLIFY_ROOT_MEMBER_SURNAME_RESOURCE_POLICY_STATEMENT_RESOURCE = DdbTable.MEMBER;
    public static final String AMPLIFY_APP_RESOURCE_ID = "SinglePageApp";
    public static final boolean AMPLIFY_APP_AUTO_BRANCH_DELETE = false;
    public static final String REACT_APP_REDIRECT_URI = "%s%s".formatted(FamilyDirectoryCdkApp.HTTPS_PREFIX, FamilyDirectoryDomainStack.HOSTED_ZONE_NAME);
    public static final String REACT_APP_API_DOMAIN = "%s%s".formatted(FamilyDirectoryCdkApp.HTTPS_PREFIX, FamilyDirectoryApiGatewayStack.API_DOMAIN_NAME);
    public static final String AMPLIFY_ROOT_BRANCH_NAME = "main";
    public static final boolean AMPLIFY_ROOT_BRANCH_PULL_REQUEST_PREVIEW = false;
    public static final String AMPLIFY_REPOSITORY_OWNER = getenv("ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_OWNER");
    public static final String AMPLIFY_REPOSITORY_NAME = getenv("ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_NAME");
    public static final String AMPLIFY_REPOSITORY_OAUTH_TOKEN = getenv("ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_OAUTH_TOKEN");

    public
    FamilyDirectoryAmplifyStack (final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        final AwsSdkCall rootMemberSurnameResourceSdkCall = AwsSdkCall.builder()
                                                                      .service("dynamodb")
                                                                      .action("GetItem")
                                                                      .parameters(Map.of("TableName", AMPLIFY_ROOT_MEMBER_SURNAME_RESOURCE_POLICY_STATEMENT_RESOURCE.name(), "Key",
                                                                                         Map.of(DdbTableParameter.PK.getName(), Map.of("S", LambdaFunctionConstructUtility.ROOT_ID))))
                                                                      .physicalResourceId(PhysicalResourceId.of(String.valueOf(Instant.now()
                                                                                                                                      .getEpochSecond())))
                                                                      .region(FamilyDirectoryCdkApp.DEFAULT_REGION)
                                                                      .build();
        final PolicyStatement rootMemberSurnameResourcePolicyStatement = PolicyStatement.Builder.create()
                                                                                                .actions(AMPLIFY_ROOT_MEMBER_SURNAME_RESOURCE_POLICY_STATEMENT_ACTIONS)
                                                                                                .effect(Effect.ALLOW)
                                                                                                .resources(singletonList(
                                                                                                        importValue(AMPLIFY_ROOT_MEMBER_SURNAME_RESOURCE_POLICY_STATEMENT_RESOURCE.arnExportName())))
                                                                                                .build();
        final AwsCustomResourceProps rootMemberSurnameResourceProps = AwsCustomResourceProps.builder()
                                                                                            .onCreate(rootMemberSurnameResourceSdkCall)
                                                                                            .onUpdate(rootMemberSurnameResourceSdkCall)
                                                                                            .policy(AwsCustomResourcePolicy.fromStatements(singletonList(rootMemberSurnameResourcePolicyStatement)))
                                                                                            .build();
        final AwsCustomResource rootMemberSurnameResource = new AwsCustomResource(this, AMPLIFY_ROOT_MEMBER_SURNAME_RESOURCE_ID, rootMemberSurnameResourceProps);

        final String rootMemberSurname = rootMemberSurnameResource.getResponseField("Item.%s.S".formatted(MemberTableParameter.LAST_NAME.jsonFieldName()));

//      TODO: Disable BasicAuth once stable
        final BasicAuth devAuth = BasicAuth.fromCredentials(getenv("DEV_ORG_FAMILYDIRECTORY_USERNAME"), SecretValue.unsafePlainText(getenv("DEV_ORG_FAMILYDIRECTORY_PASSWORD")));
        final AppProps spaProps = AppProps.builder()
//                                        TODO: Enable AutoBranchCreation once stable
//                                        .autoBranchCreation()
//                                        TODO: Enable AutoBranchDeletion once stable
                                          .autoBranchDeletion(AMPLIFY_APP_AUTO_BRANCH_DELETE)
//                                        TODO: Disable BasicAuth once stable
                                          .basicAuth(devAuth)
                                          .environmentVariables(Map.of("REACT_APP_REDIRECT_URI", REACT_APP_REDIRECT_URI, "REACT_APP_API_DOMAIN", REACT_APP_API_DOMAIN, "REACT_APP_AUTH_DOMAIN",
                                                                       FamilyDirectoryCognitoStack.COGNITO_DOMAIN_NAME, "REACT_APP_CLIENT_ID",
                                                                       importValue(FamilyDirectoryCognitoStack.COGNITO_USER_POOL_CLIENT_ID_EXPORT_NAME), "REACT_APP_SURNAME", rootMemberSurname,
                                                                       "REACT_APP_AWS_REGION", FamilyDirectoryCdkApp.DEFAULT_REGION, "REACT_APP_USER_POOL_ID",
                                                                       importValue(FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME)))
                                          .platform(Platform.WEB)
                                          .sourceCodeProvider(GitHubSourceCodeProvider.Builder.create()
                                                                                              .owner(AMPLIFY_REPOSITORY_OWNER)
                                                                                              .repository(AMPLIFY_REPOSITORY_NAME)
                                                                                              .oauthToken(SecretValue.unsafePlainText(AMPLIFY_REPOSITORY_OAUTH_TOKEN))
                                                                                              .build())
                                          .build();
        final App spa = new App(this, AMPLIFY_APP_RESOURCE_ID, spaProps);
        spa.addCustomRule(CustomRule.SINGLE_PAGE_APPLICATION_REDIRECT);
        final Domain spaRootDomain = spa.addDomain(FamilyDirectoryDomainStack.HOSTED_ZONE_NAME);
        final Branch spaRootBranch = spa.addBranch(AMPLIFY_ROOT_BRANCH_NAME, BranchOptions.builder()
                                                                                          .basicAuth(devAuth)
                                                                                          .pullRequestPreview(AMPLIFY_ROOT_BRANCH_PULL_REQUEST_PREVIEW)
                                                                                          .build());
        spaRootDomain.mapRoot(spaRootBranch);
    }
}