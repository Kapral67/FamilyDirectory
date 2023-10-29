package org.familydirectory.assets.lambda.function.api.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.lambda.function.models.LambdaFunctionModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.apigatewayv2.alpha.CorsHttpMethod;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public
enum ApiFunction implements LambdaFunctionModel {
    //    GET_MEMBER("FamilyDirectoryGetMemberLambda", "%s.%s::handleRequest", of("dynamodb:Query", "dynamodb:GetItem"), "get"),
    CREATE_MEMBER("CreateMember", Map.of(DdbTable.COGNITO, singletonList("dynamodb:GetItem"), DdbTable.FAMILY, List.of("dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:UpdateItem"), DdbTable.MEMBER,
                                         List.of("dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:Query")), null, null, singletonList(HttpMethod.POST), "create"),

    DELETE_MEMBER("DeleteMember",
                  Map.of(DdbTable.COGNITO, List.of("dynamodb:DeleteItem", "dynamodb:GetItem", "dynamodb:Query"), DdbTable.FAMILY, List.of("dynamodb:GetItem", "dynamodb:UpdateItem"), DdbTable.MEMBER,
                         List.of("dynamodb:DeleteItem", "dynamodb:GetItem", "dynamodb:Query")), List.of("cognito-idp:AdminDeleteUser", "cognito-idp:ListUsers"), singletonList("ses:SendEmail"),
                  singletonList(HttpMethod.DELETE), "delete"),
    UPDATE_MEMBER("UpdateMember", Map.of(DdbTable.COGNITO, singletonList("dynamodb:GetItem"), DdbTable.FAMILY, singletonList("dynamodb:GetItem"), DdbTable.MEMBER,
                                         List.of("dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:Query")), null, null, singletonList(HttpMethod.PUT), "update");

    @NotNull
    private final String functionName;
    @Nullable
    private final Map<DdbTable, List<String>> ddbActions;
    @Nullable
    private final List<String> cognitoActions;
    @Nullable
    private final List<String> sesActions;
    @NotNull
    private final String endpoint;
    @NotNull
    private final List<HttpMethod> methods;

    ApiFunction (final @NotNull String functionName, final @Nullable Map<DdbTable, List<String>> ddbActions, final @Nullable List<String> cognitoActions, final @Nullable List<String> sesActions,
                 final @NotNull List<HttpMethod> methods, final @NotNull String endpoint)
    {
        this.functionName = "FamilyDirectory%sLambda".formatted(requireNonNull(functionName));
        this.ddbActions = ddbActions;
        this.cognitoActions = cognitoActions;
        this.sesActions = sesActions;
        this.methods = requireNonNull(methods);
        this.endpoint = requireNonNull(endpoint);
    }

    @NotNull
    public static
    List<CorsHttpMethod> getAllowedMethods () {
        return Arrays.stream(values())
                     .flatMap(f -> f.methods.stream())
                     .distinct()
                     .map(m -> CorsHttpMethod.valueOf(m.name()))
                     .flatMap(m -> Stream.concat(Stream.of(m), Stream.of(CorsHttpMethod.OPTIONS)))
                     .toList();
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
    @NotNull
    public final
    String functionName () {
        return this.functionName;
    }

    @Override
    @NotNull
    public final
    Duration timeout () {
        return DEFAULT_TIMEOUT;
    }

    @Override
    @NotNull
    public final
    Number memorySize () {
        return DEFAULT_MEMORY_SIZE;
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
}
