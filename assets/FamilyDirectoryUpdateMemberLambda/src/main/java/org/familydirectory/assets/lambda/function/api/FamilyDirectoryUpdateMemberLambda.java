package org.familydirectory.assets.lambda.function.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.familydirectory.assets.amplify.utility.AmplifyUtils;
import org.familydirectory.assets.lambda.function.api.helper.ApiHelper;
import org.familydirectory.assets.lambda.function.api.helper.UpdateHelper;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.amplify.AmplifyClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.DEBUG;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;
import static java.lang.System.getenv;
import static java.util.Objects.requireNonNull;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;

public
class FamilyDirectoryUpdateMemberLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public final @NotNull
    APIGatewayProxyResponseEvent handleRequest (final @NotNull APIGatewayProxyRequestEvent requestEvent, final @NotNull Context context)
    {
        try (final UpdateHelper updateHelper = new UpdateHelper(context.getLogger(), requestEvent)) {

//      Get Caller
            final ApiHelper.Caller caller = updateHelper.getCaller();

//      Get Event
            final UpdateHelper.EventWrapper updateEvent = updateHelper.getUpdateEvent(caller);

//      Update Member
            final PutItemRequest putItemRequest = updateHelper.getPutRequest(caller, updateEvent);
            updateHelper.getLogger()
                        .log(putItemRequest.toString(), DEBUG);
            updateHelper.getDynamoDbClient()
                        .putItem(putItemRequest);

            if (updateEvent.updateEvent()
                           .id()
                           .equals(getenv(LambdaUtils.EnvVar.ROOT_ID.name())))

            {
                try (final AmplifyClient amplifyClient = AmplifyClient.create()) {
                    AmplifyUtils.appDeployment(amplifyClient, "<MEMBER,`%s`> update ROOT".formatted(caller.memberId()), updateEvent.updateEvent()
                                                                                                                                   .member()
                                                                                                                                   .getLastName(),
                                               requireNonNull(getenv(LambdaUtils.EnvVar.AMPLIFY_APP_ID.name())), requireNonNull(getenv(LambdaUtils.EnvVar.AMPLIFY_BRANCH_NAME.name())));
                }
            }

            return new APIGatewayProxyResponseEvent().withStatusCode(SC_ACCEPTED);

        } catch (final ApiHelper.ResponseException e) {
            return e.getResponseEvent();
        } catch (final Throwable e) {
            LambdaUtils.logTrace(context.getLogger(), e, FATAL);
            return new APIGatewayProxyResponseEvent().withStatusCode(SC_INTERNAL_SERVER_ERROR);
        }
    }
}
