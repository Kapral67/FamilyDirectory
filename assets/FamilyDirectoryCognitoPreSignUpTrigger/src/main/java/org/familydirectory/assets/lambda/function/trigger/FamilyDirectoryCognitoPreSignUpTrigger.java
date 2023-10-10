package org.familydirectory.assets.lambda.function.trigger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreSignUpEvent;
import java.util.function.Predicate;
import static java.util.Optional.ofNullable;

public
class FamilyDirectoryCognitoPreSignUpTrigger implements RequestHandler<CognitoUserPoolPreSignUpEvent, CognitoUserPoolPreSignUpEvent> {

    @Override
    public
    CognitoUserPoolPreSignUpEvent handleRequest (CognitoUserPoolPreSignUpEvent event, Context context)
    {
        final String email = ofNullable(event.getRequest()
                                             .getUserAttributes()
                                             .get("email")).filter(Predicate.not(String::isBlank))
                                                           .orElseThrow();

        return CognitoUserPoolPreSignUpEvent.builder()
                                            .withVersion(event.getVersion())
                                            .withTriggerSource(event.getTriggerSource())
                                            .withRegion(event.getRegion())
                                            .withUserPoolId(event.getUserPoolId())
                                            .withUserName(event.getUserName())
                                            .withCallerContext(event.getCallerContext())
                                            .withRequest(event.getRequest())
                                            .withResponse(CognitoUserPoolPreSignUpEvent.Response.builder()
                                                                                                .withAutoConfirmUser(false)
                                                                                                .withAutoVerifyEmail(false)
                                                                                                .withAutoVerifyPhone(false)
                                                                                                .build())
                                            .build();
    }
}
