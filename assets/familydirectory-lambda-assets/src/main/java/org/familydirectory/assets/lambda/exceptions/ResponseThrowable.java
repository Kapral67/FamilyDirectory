package org.familydirectory.assets.lambda.exceptions;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public final
class ResponseThrowable extends Throwable {
    private final APIGatewayProxyResponseEvent responseEvent;

    public
    ResponseThrowable (final APIGatewayProxyResponseEvent responseEvent) {
        this.responseEvent = responseEvent;
    }

    public
    APIGatewayProxyResponseEvent getResponseEvent () {
        return this.responseEvent;
    }
}
