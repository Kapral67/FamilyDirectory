package org.familydirectory.assets.lambda.function.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.familydirectory.assets.lambda.function.api.carddav.response.CarddavResponse;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.ERROR;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;

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
            // TODO
            return wrapResponse(null);
        }
    }

    @NotNull
    private static
    APIGatewayProxyResponseEvent wrapResponse(@Nullable CarddavResponse response) {
        // TODO: this method cannot throw!
        return new APIGatewayProxyResponseEvent();
    }
}
