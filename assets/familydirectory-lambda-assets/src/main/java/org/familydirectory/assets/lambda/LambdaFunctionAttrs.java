package org.familydirectory.assets.lambda;

import java.util.List;

public enum LambdaFunctionAttrs {
    ADMIN_CREATE_MEMBER("FamilyDirectoryAdminCreateMemberLambda", "%s.%s::handleRequest",
            List.of("dynamodb:PutItem"));

    private final String functionName;
    private final String handler;
    private final List<String> actions;

    LambdaFunctionAttrs(final String functionName, final String handler, final List<String> actions) {
        this.functionName = functionName;
        this.handler = handler;
        this.actions = actions;
    }

    public final String functionName() {
        return this.functionName;
    }

    public final String handler() {
        return String.format(this.handler, this.getClass().getPackage().getName(), this.functionName);
    }

    public final List<String> actions() {
        return this.actions;
    }
}
