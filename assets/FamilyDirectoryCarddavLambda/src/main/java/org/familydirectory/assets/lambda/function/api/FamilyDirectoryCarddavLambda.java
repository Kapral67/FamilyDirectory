package org.familydirectory.assets.lambda.function.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.jetbrains.annotations.NotNull;
import static io.milton.http.ResponseStatus.SC_INTERNAL_SERVER_ERROR;

public
class FamilyDirectoryCarddavLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    @NotNull
    public final
    APIGatewayProxyResponseEvent handleRequest (APIGatewayProxyRequestEvent input, Context context) {
        return new APIGatewayProxyResponseEvent().withStatusCode(SC_INTERNAL_SERVER_ERROR);
    }
}
