package org.familydirectory.assets.lambda;

import java.util.List;
import static java.lang.String.format;
import static java.util.List.of;

public
enum LambdaFunctionAttrs {
    GET_MEMBER("FamilyDirectoryGetMemberLambda", "%s.%s::handleRequest", of("dynamodb:Query", "dynamodb:GetItem"), "get"),
    CREATE_MEMBER("FamilyDirectoryCreateMemberLambda", "%s.%s::handleRequest", of("dynamodb:Query", "dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:UpdateItem"), "create"),
    UPDATE_MEMBER("FamilyDirectoryUpdateMemberLambda", "%s.%s::handleRequest", of("dynamodb:Query", "dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:UpdateItem"), "update"),
    DELETE_MEMBER("FamilyDirectoryDeleteMemberLambda", "%s.%s::handleRequest", of("dynamodb:Query", "dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:UpdateItem"), "delete");

    private final String functionName;
    private final String handler;
    private final List<String> actions;
    private final String endpoint;

    LambdaFunctionAttrs (final String functionName, final String handler, final List<String> actions, final String endpoint) {
        this.functionName = functionName;
        this.handler = handler;
        this.actions = actions;
        this.endpoint = endpoint;
    }

    public final
    String functionName () {
        return this.functionName;
    }

    public final
    String handler () {
        return format(this.handler, this.getClass()
                                        .getPackage()
                                        .getName(), this.functionName);
    }

    public final
    List<String> actions () {
        return this.actions;
    }

    public final
    String endpoint () {
        return format("/%s", this.endpoint);
    }

    public final
    String arnExportName () {
        return format("%sArn", this.functionName);
    }

    public final
    String httpIntegrationId () {
        return format("%sHttpIntegration", this.functionName);
    }
}
