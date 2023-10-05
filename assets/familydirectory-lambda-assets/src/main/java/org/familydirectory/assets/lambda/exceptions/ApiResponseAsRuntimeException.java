package org.familydirectory.assets.lambda.exceptions;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public final
class ApiResponseAsRuntimeException extends RuntimeException {
    private final APIGatewayProxyResponseEvent responseEvent;

    public
    ApiResponseAsRuntimeException (final APIGatewayProxyResponseEvent responseEvent) {
        this.responseEvent = responseEvent;
    }

    public
    APIGatewayProxyResponseEvent getResponseEvent () {
        return this.responseEvent;
    }
}
