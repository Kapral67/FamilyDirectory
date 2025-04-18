package org.familydirectory.assets.lambda.function.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.familydirectory.assets.lambda.function.api.helpers.CarddavLambdaHelper;
import org.jetbrains.annotations.NotNull;
import static io.milton.http.ResponseStatus.SC_INTERNAL_SERVER_ERROR;

public
class FamilyDirectoryCarddavLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    @NotNull
    public final
    APIGatewayProxyResponseEvent handleRequest (final @NotNull APIGatewayProxyRequestEvent input, final @NotNull Context context) {
        try (final CarddavLambdaHelper helper = new CarddavLambdaHelper(context.getLogger(), input)) {
            throw new UnsupportedOperationException();
        } catch (final Throwable e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(SC_INTERNAL_SERVER_ERROR);
        }
    }
}

/* Before continuing the write-it-all-yourself solution,
 * let's try and see if we can use bedelwork
 * we can decode the base64 body to byte[]
 * and feed as InputStream to some
 */
