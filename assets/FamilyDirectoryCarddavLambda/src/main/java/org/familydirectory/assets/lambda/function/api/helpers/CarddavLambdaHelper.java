package org.familydirectory.assets.lambda.function.api.helpers;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.familydirectory.assets.lambda.function.api.helper.ApiHelper;
import org.jetbrains.annotations.NotNull;

public final
class CarddavLambdaHelper extends ApiHelper {
    public
    CarddavLambdaHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) {
        super(logger, requestEvent);
    }


}
