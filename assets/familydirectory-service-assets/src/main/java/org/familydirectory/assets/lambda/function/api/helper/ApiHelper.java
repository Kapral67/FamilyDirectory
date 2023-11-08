package org.familydirectory.assets.lambda.function.api.helper;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.lambda.function.helper.LambdaFunctionHelper;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.DEBUG;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.ERROR;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static java.util.Optional.ofNullable;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

public abstract
class ApiHelper implements LambdaFunctionHelper {
    public final @NotNull
    Caller getCaller () {
        final Map<String, AttributeValue> caller;
        final String callerMemberId, callerFamilyId;
        try {
            // https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-integrations-lambda.html#http-api-develop-integrations-lambda.proxy-format
            @SuppressWarnings("unchecked")
            final Map<String, Object> callerClaims = ((Map<String, Object>) ((Map<String, Object>) this.getRequestEvent()
                                                                                                       .getRequestContext()
                                                                                                       .getAuthorizer()
                                                                                                       .get("jwt")).get("claims"));
            final String callerSub = Optional.of(callerClaims)
                                             .map(map -> map.get("sub"))
                                             .map(Object::toString)
                                             .filter(Predicate.not(String::isBlank))
                                             .orElseThrow(NullPointerException::new);

            this.getLogger()
                .log("<COGNITO_SUB,`%s`> Invoked".formatted(callerSub), INFO);

            final Map<String, AttributeValue> callerCognito = ofNullable(this.getDdbItem(callerSub, DdbTable.COGNITO)).orElseThrow(NullPointerException::new);

            callerMemberId = ofNullable(callerCognito.get(CognitoTableParameter.MEMBER.jsonFieldName())).map(AttributeValue::s)
                                                                                                        .filter(Predicate.not(String::isBlank))
                                                                                                        .orElseThrow(NullPointerException::new);
            caller = ofNullable(this.getDdbItem(callerMemberId, DdbTable.MEMBER)).orElseThrow(NullPointerException::new);
            callerFamilyId = ofNullable(caller.get(MemberTableParameter.FAMILY_ID.jsonFieldName())).map(AttributeValue::s)
                                                                                                   .filter(Predicate.not(String::isBlank))
                                                                                                   .orElseThrow(NullPointerException::new);

        } catch (final NullPointerException | ClassCastException e) {
            LambdaUtils.logTrace(this.getLogger(), e, ERROR);
            this.getLogger()
                .log(this.getRequestEvent()
                         .toString(), DEBUG);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_UNAUTHORIZED));
        }

        this.getLogger()
            .log("<MEMBER,`%s`> Authenticated".formatted(callerMemberId), INFO);
        return new Caller(callerMemberId, caller, callerFamilyId);
    }

    public abstract @NotNull
    APIGatewayProxyRequestEvent getRequestEvent ();

    public
    record Caller(@NotNull String memberId, @NotNull Map<String, AttributeValue> attributeMap, @NotNull String familyId) {
    }

    public static final
    class ResponseException extends RuntimeException {
        private final @NotNull APIGatewayProxyResponseEvent responseEvent;

        public
        ResponseException (final @NotNull APIGatewayProxyResponseEvent responseEvent) {
            super();
            this.responseEvent = responseEvent;
        }

        public @NotNull
        APIGatewayProxyResponseEvent getResponseEvent () {
            return this.responseEvent;
        }
    }
}
