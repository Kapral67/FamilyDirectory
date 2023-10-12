package org.familydirectory.assets.lambda.function.api.enums;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod;
import static java.util.List.of;

public
enum ApiFunction {
    //    GET_MEMBER("FamilyDirectoryGetMemberLambda", "%s.%s::handleRequest", of("dynamodb:Query", "dynamodb:GetItem"), "get"),
    CREATE_MEMBER("CreateMember", of("dynamodb:Query", "dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:UpdateItem"), of(HttpMethod.POST), "create"),
    UPDATE_MEMBER("UpdateMember", of("dynamodb:Query", "dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:UpdateItem"), of(HttpMethod.PUT), "update"),
    DELETE_MEMBER("DeleteMember", of("dynamodb:Query", "dynamodb:GetItem", "dynamodb:DeleteItem"), of(HttpMethod.DELETE), "delete");

    @NotNull
    private final String functionName;
    @NotNull
    private final List<String> actions;
    @NotNull
    private final String endpoint;
    @NotNull
    private final List<HttpMethod> methods;

    ApiFunction (final @NotNull String functionName, final @NotNull List<String> actions, final @NotNull List<HttpMethod> methods, final @NotNull String endpoint) {
        this.functionName = "FamilyDirectory%sLambda".formatted(functionName);
        this.actions = actions;
        this.methods = methods;
        this.endpoint = endpoint;
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

    @NotNull
    public final
    List<String> actions () {
        return this.actions;
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
