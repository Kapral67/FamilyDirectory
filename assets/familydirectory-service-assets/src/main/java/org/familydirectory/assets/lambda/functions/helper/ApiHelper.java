package org.familydirectory.assets.lambda.functions.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.DEBUG;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.ERROR;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.TRACE;
import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.familydirectory.assets.ddb.enums.DdbTable.COGNITO;
import static org.familydirectory.assets.ddb.enums.DdbTable.MEMBERS;
import static org.familydirectory.assets.ddb.enums.DdbTable.PK;
import static org.familydirectory.assets.ddb.enums.cognito.CognitoParams.MEMBER;
import static org.familydirectory.assets.ddb.enums.member.MemberParams.FAMILY_ID;

public abstract
class ApiHelper {
    public final @NotNull
    Caller getCaller () {
        final Map<String, AttributeValue> caller;
        final String callerMemberId, callerFamilyId;
        try {
            @SuppressWarnings("unchecked")
            final Map<String, Object> callerClaims = (Map<String, Object>) requireNonNull(this.getRequestEvent()
                                                                                              .getRequestContext()
                                                                                              .getAuthorizer()
                                                                                              .get("claims"));
            final String callerSub = Optional.of(callerClaims)
                                             .map(map -> map.get("sub"))
                                             .map(Object::toString)
                                             .filter(Predicate.not(String::isBlank))
                                             .orElseThrow(NullPointerException::new);

            this.getLogger()
                .log(format("<COGNITO_SUB,`%s`> Invoked CreateMember Lambda", callerSub), INFO);

            final Map<String, AttributeValue> callerCognito = ofNullable(this.getDdbItem(callerSub, COGNITO)).orElseThrow(NullPointerException::new);

            callerMemberId = ofNullable(callerCognito.get(MEMBER.jsonFieldName())).map(AttributeValue::s)
                                                                                  .filter(Predicate.not(String::isBlank))
                                                                                  .orElseThrow(NullPointerException::new);
            caller = ofNullable(this.getDdbItem(callerMemberId, MEMBERS)).orElseThrow(NullPointerException::new);
            callerFamilyId = ofNullable(caller.get(FAMILY_ID.jsonFieldName())).map(AttributeValue::s)
                                                                              .filter(Predicate.not(String::isBlank))
                                                                              .orElseThrow(NullPointerException::new);

        } catch (final NullPointerException | ClassCastException e) {
            this.logTrace(e, ERROR);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_UNAUTHORIZED));
        }

        this.getLogger()
            .log(format("<MEMBER,`%s`> Authenticated", callerMemberId), INFO);
        return new Caller(callerMemberId, caller, callerFamilyId);
    }

    public abstract @NotNull
    LambdaLogger getLogger ();

    public abstract @NotNull
    APIGatewayProxyRequestEvent getRequestEvent ();

    public @Nullable
    Map<String, AttributeValue> getDdbItem (final @NotNull String primaryKey, final @NotNull DdbTable ddbTable) {
        final GetItemRequest request = GetItemRequest.builder()
                                                     .tableName(ddbTable.name())
                                                     .key(singletonMap(PK.getName(), AttributeValue.fromS(primaryKey)))
                                                     .build();
        final GetItemResponse response = this.getDynamoDbClient()
                                             .getItem(request);
        return (response.hasItem())
                ? response.item()
                : null;
    }

    public abstract @NotNull
    DynamoDbClient getDynamoDbClient ();

    public
    void logTrace (final @NotNull Throwable e, final @NotNull LogLevel logLevel) {
        this.getLogger()
            .log(e.getMessage(), logLevel);
        ofNullable(e.getCause()).ifPresent(throwable -> this.getLogger()
                                                            .log(throwable.toString(), DEBUG));
        this.getLogger()
            .log(Arrays.toString(e.getStackTrace()), TRACE);
    }

    public
    record Caller(@NotNull String memberId, @NotNull Map<String, AttributeValue> attributeMap, @NotNull String familyId) {
    }

    public static final
    class ResponseException extends RuntimeException {
        private final @NotNull APIGatewayProxyResponseEvent responseEvent;

        public
        ResponseException (final @NotNull APIGatewayProxyResponseEvent responseEvent) {
            this.responseEvent = responseEvent;
        }

        public @NotNull
        APIGatewayProxyResponseEvent getResponseEvent () {
            return this.responseEvent;
        }
    }
}
