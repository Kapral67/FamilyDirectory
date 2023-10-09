package org.familydirectory.assets.lambda.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.familydirectory.assets.lambda.functions.helper.ApiHelper;
import org.familydirectory.assets.lambda.functions.helper.UpdateHelper;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;

public
class FamilyDirectoryUpdateMemberLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public
    APIGatewayProxyResponseEvent handleRequest (APIGatewayProxyRequestEvent requestEvent, Context context) {
        final UpdateHelper updateHelper = new UpdateHelper(context.getLogger(), requestEvent);
        try {
//      Get Caller
            final ApiHelper.Caller caller = updateHelper.getCaller();

//      Get Event
            final UpdateHelper.EventWrapper updateEvent = updateHelper.getUpdateEvent(caller);

//      Update Member
            updateHelper.getDynamoDbClient()
                        .putItem(updateHelper.getPutRequest(caller, updateEvent));

        } catch (final ApiHelper.ResponseException e) {
            return e.getResponseEvent();
        } catch (final Exception e) {
            updateHelper.logTrace(e, FATAL);
            return new APIGatewayProxyResponseEvent().withStatusCode(SC_INTERNAL_SERVER_ERROR);
        }

        return new APIGatewayProxyResponseEvent().withStatusCode(SC_ACCEPTED);
    }
}
