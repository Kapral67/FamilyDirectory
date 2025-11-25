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
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.ERROR;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;
import static java.lang.System.getenv;
import static java.util.Objects.requireNonNull;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;

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

            Exception amplifyDeploymentException = null;
            if (updateEvent.updateEvent()
                           .id()
                           .equals(getenv(LambdaUtils.EnvVar.ROOT_ID.name())))

            {
                try (final AmplifyClient amplifyClient = AmplifyClient.create()) {
                    AmplifyUtils.appDeployment(amplifyClient, "<MEMBER,`%s`> update ROOT".formatted(caller.caller().id()), updateEvent.updateEvent().member().getLastName(),
                                               requireNonNull(getenv(LambdaUtils.EnvVar.AMPLIFY_APP_ID.name())), requireNonNull(getenv(LambdaUtils.EnvVar.AMPLIFY_BRANCH_NAME.name())));
                } catch (Exception e) {
                    amplifyDeploymentException = e;
                }
            }

            if (updateEvent.shouldDeleteCognito()) {
                try {
                    updateHelper.deleteCognitoAccountAndNotify(updateEvent.updateEvent()
                                                                          .id(), updateEvent.updateEvent()
                                                                                            .member()
                                                                                            .getEmail());
                } catch (Exception e) {
                    if (amplifyDeploymentException != null) {
                        e.addSuppressed(amplifyDeploymentException);
                    }
                    throw e;
                }
            }

            if (amplifyDeploymentException != null) {
                LambdaUtils.logTrace(updateHelper.getLogger(), amplifyDeploymentException, ERROR);
            }

            return new APIGatewayProxyResponseEvent().withStatusCode(SC_OK);

        } catch (final ApiHelper.ResponseException e) {
            return e.getResponseEvent();
        } catch (final Throwable e) {
            LambdaUtils.logTrace(context.getLogger(), e, FATAL);
            return new APIGatewayProxyResponseEvent().withStatusCode(SC_INTERNAL_SERVER_ERROR);
        }
    }
}
