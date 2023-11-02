package org.familydirectory.assets.lambda.function.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.familydirectory.assets.lambda.function.api.helper.ApiHelper;
import org.familydirectory.assets.lambda.function.api.helper.CreateHelper;
import org.familydirectory.assets.lambda.function.api.models.CreateEvent;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;

public
class FamilyDirectoryCreateMemberLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public final @NotNull
    APIGatewayProxyResponseEvent handleRequest (final @NotNull APIGatewayProxyRequestEvent event, final @NotNull Context context)
    {
        try (final CreateHelper createHelper = new CreateHelper(context.getLogger(), event)) {

//      Get Caller
            final ApiHelper.Caller caller = createHelper.getCaller();

//      Get Event
            final CreateEvent createEvent = createHelper.getCreateEvent(caller);

//      Build Transaction
            final TransactWriteItemsRequest transaction = createHelper.buildCreateTransaction(caller, createEvent);

//      Execute Transaction
            createHelper.getDynamoDbClient()
                        .transactWriteItems(transaction);

            return new APIGatewayProxyResponseEvent().withStatusCode(SC_CREATED);

        } catch (final ApiHelper.ResponseException e) {
            return e.getResponseEvent();
        } catch (final Throwable e) {
            LambdaUtils.logTrace(context.getLogger(), e, FATAL);
            return new APIGatewayProxyResponseEvent().withStatusCode(SC_INTERNAL_SERVER_ERROR);
        }
    }
}
