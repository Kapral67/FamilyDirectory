package org.familydirectory.assets.lambda.function.trigger.enums;

import java.util.List;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.lambda.function.models.LambdaFunctionModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public
enum TriggerFunction implements LambdaFunctionModel {
    PRE_SIGN_UP("PreSignUp", Map.of(DdbTable.MEMBER, singletonList("dynamodb:Query"), DdbTable.COGNITO, singletonList("dynamodb:Query")), null, null),
    POST_CONFIRMATION("PostConfirmation", Map.of(DdbTable.MEMBER, singletonList("dynamodb:Query"), DdbTable.COGNITO, List.of("dynamodb:GetItem", "dynamodb:PutItem")),
                      singletonList("cognito-idp:AdminDisableUser"), List.of("ses:SendEmail", "ses:SendRawEmail"));

    @NotNull
    private final String functionName;
    @Nullable
    private final Map<DdbTable, List<String>> ddbActions;
    @Nullable
    private final List<String> cognitoActions;
    @Nullable
    private final List<String> sesActions;

    TriggerFunction (final @NotNull String functionName, final @Nullable Map<DdbTable, List<String>> ddbActions, final @Nullable List<String> cognitoActions, final @Nullable List<String> sesActions)
    {
        this.functionName = "FamilyDirectoryCognito%sTrigger".formatted(requireNonNull(functionName));
        this.ddbActions = ddbActions;
        this.cognitoActions = cognitoActions;
        this.sesActions = sesActions;
    }

    @Override
    @NotNull
    public final
    String handler () {
        return "org.familydirectory.assets.lambda.function.trigger.%s::handleRequest".formatted(this.functionName);
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
        return null;
    }

    @Override
    @NotNull
    public final
    String functionName () {
        return this.functionName;
    }
}
