package org.familydirectory.assets.lambda.function.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.familydirectory.assets.lambda.function.api.helper.ApiHelper;
import org.familydirectory.assets.lambda.function.api.helper.DeleteHelper;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;

public
class FamilyDirectoryDeleteMemberLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public final @NotNull
    APIGatewayProxyResponseEvent handleRequest (final @NotNull APIGatewayProxyRequestEvent requestEvent, final @NotNull Context context)
    {

        try (final DeleteHelper deleteHelper = new DeleteHelper(context.getLogger(), requestEvent)) {

//      Get Caller
            final ApiHelper.Caller caller = deleteHelper.getCaller();

//      Get Event
            final DeleteHelper.EventWrapper deleteEvent = deleteHelper.getDeleteEvent(caller);

//      Build Transaction
            final TransactWriteItemsRequest transaction = deleteHelper.buildDeleteTransaction(caller, deleteEvent);

//      Execute Transaction
            deleteHelper.getDynamoDbClient()
                        .transactWriteItems(transaction);

//      Delete Cognito Account & Notify User of Account Deletion
            deleteHelper.deleteCognitoAccountAndNotify(deleteEvent.ddbMemberId());

            return new APIGatewayProxyResponseEvent().withStatusCode(SC_ACCEPTED);

        } catch (final ApiHelper.ResponseException e) {
            return e.getResponseEvent();
        } catch (final Throwable e) {
            LambdaUtils.logTrace(context.getLogger(), e, FATAL);
            return new APIGatewayProxyResponseEvent().withStatusCode(SC_INTERNAL_SERVER_ERROR);
        }
    }
}
