package org.familydirectory.assets.lambda.function.trigger.enums;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public
enum TriggerFunction {
    PRE_SIGN_UP("PreSignUp"),
    POST_CONFIRMATION("PostConfirmation");

    @NotNull
    private final String functionName;

    @NotNull
    private final List<String> actions = List.of("dynamodb:Query", "dynamodb:GetItem");

    TriggerFunction (final @NotNull String functionName) {
        this.functionName = "FamilyDirectoryCognito%sTrigger".formatted(functionName);
    }

    @NotNull
    public final
    String functionName () {
        return this.functionName;
    }

    @NotNull
    public final
    String handler () {
        return "org.familydirectory.assets.lambda.function.trigger.%s::handleRequest".formatted(this.functionName);
    }

    @NotNull
    public final
    List<String> actions () {
        return this.actions;
    }

    @NotNull
    public final
    String arnExportName () {
        return "%sArn".formatted(this.functionName);
    }
}
