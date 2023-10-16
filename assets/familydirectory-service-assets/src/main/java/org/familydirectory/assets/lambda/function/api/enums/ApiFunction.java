package org.familydirectory.assets.lambda.function.api.enums;

import java.util.List;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public
enum ApiFunction {
    //    GET_MEMBER("FamilyDirectoryGetMemberLambda", "%s.%s::handleRequest", of("dynamodb:Query", "dynamodb:GetItem"), "get"),
    CREATE_MEMBER("CreateMember",
                  Map.of(DdbTable.MEMBER, List.of("dynamodb:Query", "dynamodb:GetItem", "dynamodb:PutItem"), DdbTable.FAMILY, List.of("dynamodb:GetItem", "dynamodb:UpdateItem", "dynamodb:PutItem"),
                         DdbTable.COGNITO, singletonList("dynamodb:GetItem")), null, singletonList(HttpMethod.POST), "create"),
    UPDATE_MEMBER("UpdateMember", Map.of(DdbTable.MEMBER, List.of("dynamodb:Query", "dynamodb:GetItem", "dynamodb:PutItem"), DdbTable.FAMILY, singletonList("dynamodb:GetItem"), DdbTable.COGNITO,
                                         singletonList("dynamodb:GetItem")), null, singletonList(HttpMethod.PUT), "update"),
    DELETE_MEMBER("DeleteMember", Map.of(DdbTable.MEMBER, List.of("dynamodb:Query", "dynamodb:GetItem", "dynamodb:DeleteItem"), DdbTable.FAMILY, singletonList("dynamodb:GetItem"), DdbTable.COGNITO,
                                         List.of("dynamodb:Query", "dynamodb:GetItem", "dynamodb:DeleteItem")), List.of("cognito-idp:ListUserPools", "cognito-idp:AdminDisableUser"),
                  singletonList(HttpMethod.DELETE), "delete");

    @NotNull
    private final String functionName;
    @Nullable
    private final Map<DdbTable, List<String>> ddbActions;
    @Nullable
    private final List<String> cognitoActions;
    @NotNull
    private final String endpoint;
    @NotNull
    private final List<HttpMethod> methods;

    ApiFunction (final @NotNull String functionName, final @Nullable Map<DdbTable, List<String>> ddbActions, final @Nullable List<String> cognitoActions, final @NotNull List<HttpMethod> methods,
                 final @NotNull String endpoint)
    {
        this.functionName = "FamilyDirectory%sLambda".formatted(requireNonNull(functionName));
        this.ddbActions = ddbActions;
        this.cognitoActions = cognitoActions;
        this.methods = requireNonNull(methods);
        this.endpoint = requireNonNull(endpoint);
    }

    @NotNull
    public final
    String functionName () {
        return this.functionName;
    }

    @NotNull
    public final
    String handler () {
        return "org.familydirectory.assets.lambda.function.api.%s::handleRequest".formatted(this.functionName);
    }

    @Nullable
    public final
    Map<DdbTable, List<String>> ddbActions () {
        return this.ddbActions;
    }

    @Nullable
    public final
    List<String> cognitoActions () {
        return this.cognitoActions;
    }

    @NotNull
    public final
    List<HttpMethod> methods () {
        return this.methods;
    }

    @NotNull
    public final
    String endpoint () {
        return "/%s".formatted(this.endpoint);
    }

    @NotNull
    public final
    String arnExportName () {
        return "%sArn".formatted(this.functionName);
    }

    @NotNull
    public final
    String httpIntegrationId () {
        return "%sHttpIntegration".formatted(this.functionName);
    }
}
