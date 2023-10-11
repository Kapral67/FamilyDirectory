package org.familydirectory.assets.lambda.function.trigger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostConfirmationEvent;
import java.util.Map;
import java.util.function.Predicate;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import static java.util.Optional.ofNullable;

public
class FamilyDirectoryCognitoPostConfirmationTrigger implements RequestHandler<CognitoUserPoolPostConfirmationEvent, CognitoUserPoolPostConfirmationEvent> {

    private static final DynamoDbClient DDB_CLIENT = DynamoDbClient.create();

    @Override
    public
    CognitoUserPoolPostConfirmationEvent handleRequest (CognitoUserPoolPostConfirmationEvent event, Context context)
    {
        final LambdaLogger logger = context.getLogger();

        final String email = ofNullable(event.getRequest()
                                             .getUserAttributes()
                                             .get("email")).filter(Predicate.not(String::isBlank))
                                                           .orElseThrow();

        logger.log("ACCEPT");

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
