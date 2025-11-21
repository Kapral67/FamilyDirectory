package org.familydirectory.assets.lambda.function.api.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.lambda.function.models.LambdaFunctionModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.services.apigatewayv2.CorsHttpMethod;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public
enum ApiFunction implements LambdaFunctionModel {
    CREATE_MEMBER("CreateMember", Map.of(DdbTable.COGNITO, singletonList("dynamodb:GetItem"), DdbTable.FAMILY, List.of("dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:UpdateItem"), DdbTable.MEMBER, List.of("dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:Query")), null, null, null, null, singletonList(HttpMethod.POST), "create"),
    DELETE_MEMBER("DeleteMember", Map.of(DdbTable.COGNITO, List.of("dynamodb:DeleteItem", "dynamodb:GetItem", "dynamodb:Query"), DdbTable.FAMILY, List.of("dynamodb:DeleteItem", "dynamodb:GetItem", "dynamodb:UpdateItem"), DdbTable.MEMBER, List.of("dynamodb:DeleteItem", "dynamodb:GetItem")), List.of("cognito-idp:AdminDeleteUser", "cognito-idp:ListUsers"), List.of("ses:SendEmail", "ses:SendRawEmail"), null, null, singletonList(HttpMethod.POST), "delete"),
    GET_MEMBER("GetMember", Map.of(DdbTable.COGNITO, List.of("dynamodb:GetItem", "dynamodb:Query"), DdbTable.FAMILY, singletonList("dynamodb:GetItem"), DdbTable.MEMBER, singletonList("dynamodb:GetItem")), null, null, null, null, singletonList(HttpMethod.GET), "get"),
    GET_PDF("GetPdf", Map.of(DdbTable.COGNITO, singletonList("dynamodb:GetItem"), DdbTable.MEMBER, singletonList("dynamodb:GetItem")), null, null, singletonList("s3:GetObject"), null, singletonList(HttpMethod.GET), "pdf"),
    UPDATE_MEMBER("UpdateMember", Map.of(DdbTable.COGNITO, singletonList("dynamodb:GetItem"), DdbTable.FAMILY, singletonList("dynamodb:GetItem"), DdbTable.MEMBER, List.of("dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:Query")), null, null, null, List.of("amplify:GetApp", "amplify:StartJob", "amplify:UpdateApp"), singletonList(HttpMethod.PUT), "update"),
    CARDDAV("Carddav", Map.of(DdbTable.SYNC, singletonList("dynamodb:GetItem"), DdbTable.MEMBER, List.of("dynamodb:GetItem", "dynamodb:Scan"), DdbTable.COGNITO, singletonList("dynamodb:GetItem")), null, null, null, null, singletonList(HttpMethod.POST), "carddav");

    @NotNull
    private final String functionName;
    @Nullable
    private final Map<DdbTable, List<String>> ddbActions;
    @Nullable
    private final List<String> cognitoActions;
    @Nullable
    private final List<String> sesActions;
    @Nullable
    private final List<String> sssActions;
    @Nullable
    private final List<String> amplifyActions;
    @NotNull
    private final String endpoint;
    @NotNull
    private final List<HttpMethod> methods;

    ApiFunction (final @NotNull String functionName, final @Nullable Map<DdbTable, List<String>> ddbActions, final @Nullable List<String> cognitoActions, final @Nullable List<String> sesActions,
                 final @Nullable List<String> sssActions, final @Nullable List<String> amplifyActions, final @NotNull List<HttpMethod> methods, final @NotNull String endpoint)
    {
        this.functionName = "FamilyDirectory%sLambda".formatted(requireNonNull(functionName));
        this.ddbActions = ddbActions;
        this.cognitoActions = cognitoActions;
        this.sesActions = sesActions;
        this.sssActions = sssActions;
        this.amplifyActions = amplifyActions;
        this.methods = requireNonNull(methods);
        this.endpoint = requireNonNull(endpoint);
    }

    @NotNull
    public static
    Set<CorsHttpMethod> getAllowedMethods () {
        return Arrays.stream(values())
                     .flatMap(f -> f.methods.stream())
                     .map(m -> CorsHttpMethod.valueOf(m.name()))
                     .collect(Collectors.toUnmodifiableSet());
    }

    @NotNull
    public final
    List<HttpMethod> methods () {
        return this.methods;
    }

    @Override
    @NotNull
    public final
    String handler () {
        return "org.familydirectory.assets.lambda.function.api.%s::handleRequest".formatted(this.functionName);
    }

    @Override
    @Nullable
    public final
    Map<DdbTable, List<String>> ddbActions () {
        return this.ddbActions;
    }

    @Override
    @Nullable
    public final
    List<String> cognitoActions () {
        return this.cognitoActions;
    }

    @Override
    @Nullable
    public final
    List<String> sesActions () {
        return this.sesActions;
    }

    @Override
    public @Nullable
    List<String> sssActions () {
        return this.sssActions;
    }

    @Override
    public @Nullable
    List<String> amplifyActions () {
        return this.amplifyActions;
    }

    @Override
    @NotNull
    public final
    String functionName () {
        return this.functionName;
    }

    @NotNull
    public final
    String endpoint () {
        return "/%s".formatted(this.endpoint);
    }

    @NotNull
    public final
    String httpIntegrationId () {
        return "%sHttpIntegration".formatted(this.functionName);
    }

    @Override
    @NotNull
    public
    Number memorySize() {
        if (CARDDAV.equals(this)) {
            return NEW_ACCOUNT_MAX_MEMORY_SIZE;
        }
        return LambdaFunctionModel.super.memorySize();
    }
}
