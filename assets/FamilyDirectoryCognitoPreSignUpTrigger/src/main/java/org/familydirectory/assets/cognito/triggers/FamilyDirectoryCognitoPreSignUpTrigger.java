package org.familydirectory.assets.cognito.triggers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreSignUpEvent;

public
class FamilyDirectoryCognitoPreSignUpTrigger implements RequestHandler<CognitoUserPoolPreSignUpEvent, CognitoUserPoolPreSignUpEvent> {

    @Override
    public
    CognitoUserPoolPreSignUpEvent handleRequest (CognitoUserPoolPreSignUpEvent event, Context context)
    {
        final String email = event.getRequest()
                                  .getUserAttributes()
                                  .get("email");

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
