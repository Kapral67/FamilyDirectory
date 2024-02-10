package org.familydirectory.cdk.amplify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.cdk.FamilyDirectoryCdkApp;
import org.familydirectory.cdk.apigateway.FamilyDirectoryApiGatewayStack;
import org.familydirectory.cdk.cognito.FamilyDirectoryCognitoStack;
import org.familydirectory.cdk.domain.FamilyDirectoryDomainStack;
import org.familydirectory.cdk.lambda.construct.utility.LambdaFunctionConstructUtility;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
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
import static org.familydirectory.assets.Constants.VERSION_STR;
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
    public static final Platform AMPLIFY_PLATFORM = Platform.WEB;
    public static final List<CustomRule> AMPLIFY_CUSTOM_RULES = singletonList(CustomRule.SINGLE_PAGE_APPLICATION_REDIRECT);
    public static final String AMPLIFY_SURNAME_FIELD = "Item.%s.S".formatted(MemberTableParameter.LAST_NAME.jsonFieldName());

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

        final String rootMemberSurname = rootMemberSurnameResource.getResponseField(AMPLIFY_SURNAME_FIELD);

        final AppProps spaProps = AppProps.builder()
                                          .autoBranchDeletion(AMPLIFY_APP_AUTO_BRANCH_DELETE)
                                          .customRules(AMPLIFY_CUSTOM_RULES)
                                          .environmentVariables(
                                                  Map.ofEntries(Map.entry(ReactEnvVar.BACKEND_VERSION.toString(), VERSION_STR), Map.entry(ReactEnvVar.REDIRECT_URI.toString(), REACT_APP_REDIRECT_URI),
                                                                Map.entry(ReactEnvVar.API_DOMAIN.toString(), REACT_APP_API_DOMAIN),
                                                                Map.entry(ReactEnvVar.AUTH_DOMAIN.toString(), FamilyDirectoryCognitoStack.COGNITO_DOMAIN_NAME),
                                                                Map.entry(ReactEnvVar.CLIENT_ID.toString(), importValue(FamilyDirectoryCognitoStack.COGNITO_USER_POOL_CLIENT_ID_EXPORT_NAME)),
                                                                Map.entry(ReactEnvVar.SURNAME.toString(), rootMemberSurname),
                                                                Map.entry(ReactEnvVar.AWS_REGION.toString(), FamilyDirectoryCdkApp.DEFAULT_REGION),
                                                                Map.entry(ReactEnvVar.USER_POOL_ID.toString(), importValue(FamilyDirectoryCognitoStack.COGNITO_USER_POOL_ID_EXPORT_NAME)),
                                                                Map.entry(ReactEnvVar.AGE_OF_MAJORITY.toString(), String.valueOf(DdbUtils.AGE_OF_MAJORITY))))
                                          .platform(AMPLIFY_PLATFORM)
                                          .sourceCodeProvider(GitHubSourceCodeProvider.Builder.create()
                                                                                              .owner(AMPLIFY_REPOSITORY_OWNER)
                                                                                              .repository(AMPLIFY_REPOSITORY_NAME)
                                                                                              .oauthToken(SecretValue.unsafePlainText(AMPLIFY_REPOSITORY_OAUTH_TOKEN))
                                                                                              .build())
                                          .build();
        final App spa = new App(this, AMPLIFY_APP_RESOURCE_ID, spaProps);
        final Domain spaRootDomain = spa.addDomain(FamilyDirectoryDomainStack.HOSTED_ZONE_NAME);
        final Branch spaRootBranch = spa.addBranch(AMPLIFY_ROOT_BRANCH_NAME, BranchOptions.builder()
                                                                                          .pullRequestPreview(AMPLIFY_ROOT_BRANCH_PULL_REQUEST_PREVIEW)
                                                                                          .build());
        spaRootDomain.mapRoot(spaRootBranch);
    }

    public
    enum ReactEnvVar {
        BACKEND_VERSION,
        REDIRECT_URI,
        API_DOMAIN,
        AUTH_DOMAIN,
        CLIENT_ID,
        SURNAME,
        AWS_REGION,
        USER_POOL_ID,
        AGE_OF_MAJORITY;

        public static final String PREFIX = "REACT_APP_";

        @Override
        @Contract(pure = true)
        @NotNull
        public final
        String toString () {
            return PREFIX + this.name();
        }
    }
}
