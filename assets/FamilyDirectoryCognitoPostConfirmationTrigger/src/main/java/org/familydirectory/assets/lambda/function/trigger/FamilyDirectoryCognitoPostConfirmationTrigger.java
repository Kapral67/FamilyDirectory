package org.familydirectory.assets.lambda.function.trigger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostConfirmationEvent;
import java.util.Map;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import static java.util.Optional.ofNullable;

public
class FamilyDirectoryCognitoPostConfirmationTrigger implements RequestHandler<CognitoUserPoolPostConfirmationEvent, CognitoUserPoolPostConfirmationEvent> {

    private static final DynamoDbClient DDB_CLIENT = DynamoDbClient.create();
    private static final CognitoIdentityProviderAsyncClient COGNITO_CLIENT = CognitoIdentityProviderAsyncClient.create();

    @Override
    public final @NotNull
    CognitoUserPoolPostConfirmationEvent handleRequest (final @NotNull CognitoUserPoolPostConfirmationEvent event, final @NotNull Context context)
    {
        final LambdaLogger logger = context.getLogger();
        final String email;
        try {
            email = ofNullable(event.getRequest()
                                    .getUserAttributes()
                                    .get("email")).filter(Predicate.not(String::isBlank))
                                                  .orElseThrow();

        } catch (final Exception e) {
            // TODO: Disabling this user is mandatory and we need to avoid potential failures:
            // TODO: - Use Async Call
            // TODO: - Use Retry Logic
            // TODO: - Setup DLQ/SNS/SQS (more Research needed) to ensure that this request eventually succeeds even if retries fail
            COGNITO_CLIENT.adminDisableUser(AdminDisableUserRequest.builder()
                                                                   .userPoolId(event.getUserPoolId())
                                                                   .username(event.getUserName())
                                                                   .build());
            throw e;
        }

        return CognitoUserPoolPostConfirmationEvent.builder()
                                                   .withVersion(event.getVersion())
                                                   .withTriggerSource(event.getTriggerSource())
                                                   .withRegion(event.getRegion())
                                                   .withUserPoolId(event.getUserPoolId())
                                                   .withUserName(event.getUserName())
                                                   .withCallerContext(event.getCallerContext())
                                                   .withRequest(CognitoUserPoolPostConfirmationEvent.Request.builder()
                                                                                                            .withClientMetadata(event.getRequest()
                                                                                                                                     .getClientMetadata())
                                                                                                            .withUserAttributes(Map.of("email", email, "email_verified", "true"))
                                                                                                            .build())
                                                   .build();
    }
}
