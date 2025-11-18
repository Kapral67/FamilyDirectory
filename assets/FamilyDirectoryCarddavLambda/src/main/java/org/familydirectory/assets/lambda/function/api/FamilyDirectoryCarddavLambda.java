package org.familydirectory.assets.lambda.function.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.milton.http.Response;
import java.util.Map;
import java.util.Optional;
import org.familydirectory.assets.lambda.function.api.carddav.response.CarddavResponse;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.ERROR;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;
import static io.milton.http.ResponseStatus.SC_OK;

public
class FamilyDirectoryCarddavLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    @NotNull
    public final
    APIGatewayProxyResponseEvent handleRequest (final @NotNull APIGatewayProxyRequestEvent input, final @NotNull Context context) {
        try (final CarddavLambdaHelper helper = new CarddavLambdaHelper(context.getLogger(), input)) {
            return wrapResponse(helper.getResponse());
        } catch (CarddavLambdaHelper.CarddavResponseException e) {
            LambdaUtils.logTrace(context.getLogger(), e, ERROR);
            return wrapResponse(e.getResponse());
        } catch (Throwable e) {
            LambdaUtils.logTrace(context.getLogger(), e, FATAL);
            return wrapResponse(null);
        }
    }

    @NotNull
    private static
    APIGatewayProxyResponseEvent wrapResponse(@Nullable CarddavResponse response) {
        response = Optional.ofNullable(response)
                           .orElse(CarddavResponse.builder()
                                                  .status(Response.Status.SC_INTERNAL_SERVER_ERROR)
                                                  .build());
        return new APIGatewayProxyResponseEvent().withStatusCode(SC_OK)
                                                 .withHeaders(Map.of(Response.Header.CONTENT_TYPE.code, "application/json"))
                                                 .withBody(response.toString());
    }
}
