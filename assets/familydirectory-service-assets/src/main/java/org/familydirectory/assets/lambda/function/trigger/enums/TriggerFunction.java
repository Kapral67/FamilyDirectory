package org.familydirectory.assets.lambda.function.trigger.enums;

public
enum TriggerFunction {
    PRE_SIGN_UP("PreSignUp"),
    POST_CONFIRMATION("PostConfirmation");

    private final String functionName;

    TriggerFunction (String functionName) {
        this.functionName = "FamilyDirectoryCognito%sTrigger".formatted(functionName);
    }

    public final
    String functionName () {
        return this.functionName;
    }

    public final
    String handler () {
        return "org.familydirectory.assets.lambda.function.trigger.%s::handleRequest".formatted(this.functionName);
    }
}
