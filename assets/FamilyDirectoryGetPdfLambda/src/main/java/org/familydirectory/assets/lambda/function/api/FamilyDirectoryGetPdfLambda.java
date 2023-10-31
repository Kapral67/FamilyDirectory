package org.familydirectory.assets.lambda.function.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.familydirectory.assets.lambda.function.api.helper.ApiHelper;
import org.familydirectory.assets.lambda.function.api.helper.GetPdfHelper;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;
import static java.util.Collections.singletonMap;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;

public
class FamilyDirectoryGetPdfLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public final @NotNull
    APIGatewayProxyResponseEvent handleRequest (final @NotNull APIGatewayProxyRequestEvent requestEvent, final @NotNull Context context) {
        try {
            final GetPdfHelper pdfHelper = new GetPdfHelper(context.getLogger(), requestEvent);

            return new APIGatewayProxyResponseEvent().withStatusCode(SC_MOVED_TEMPORARILY)
                                                     .withHeaders(singletonMap("Location", pdfHelper.getPresignedPdfUrl()
                                                                                                    .toExternalForm()));
        } catch (final ApiHelper.ResponseException e) {
            return e.getResponseEvent();
        } catch (final Throwable e) {
            LambdaUtils.logTrace(context.getLogger(), e, FATAL);
            return new APIGatewayProxyResponseEvent().withStatusCode(SC_INTERNAL_SERVER_ERROR);
        }
    }
}
