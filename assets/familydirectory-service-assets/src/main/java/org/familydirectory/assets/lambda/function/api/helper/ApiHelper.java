package org.familydirectory.assets.lambda.function.api.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.lambda.function.helper.LambdaFunctionHelper;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.DEBUG;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.ERROR;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

public abstract
class ApiHelper implements LambdaFunctionHelper {
    protected final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    protected final @NotNull LambdaLogger logger;
    protected final @NotNull APIGatewayProxyRequestEvent requestEvent;

    public
    ApiHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) {
        super();
        this.logger = requireNonNull(logger);
        this.requestEvent = requireNonNull(requestEvent);
        this.logger.log(this.requestEvent.toString(), DEBUG);
    }

    @NotNull
    public final
    Caller getCaller () {
        final Map<String, AttributeValue> caller;
        final String callerMemberId, callerFamilyId;
        final boolean isCallerAdmin;
        try {
            // https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-integrations-lambda.html#http-api-develop-integrations-lambda.proxy-format
            @SuppressWarnings("unchecked")
            final Map<String, Object> callerClaims = ((Map<String, Object>) ((Map<String, Object>) requireNonNull(this.getRequestEvent()).getRequestContext()
                                                                                                                                         .getAuthorizer()
                                                                                                                                         .get("jwt")).get("claims"));
            final String callerSub = Optional.of(callerClaims)
                                             .map(map -> map.get("sub"))
                                             .map(Object::toString)
                                             .filter(Predicate.not(String::isBlank))
                                             .orElseThrow(NullPointerException::new);

            this.getLogger()
                .log("<COGNITO_SUB,`%s`> Invoked".formatted(callerSub), INFO);

            final Map<String, AttributeValue> callerCognito = requireNonNull(this.getDdbItem(callerSub, DdbTable.COGNITO));
            isCallerAdmin = ofNullable(callerCognito.get(CognitoTableParameter.IS_ADMIN.jsonFieldName())).map(AttributeValue::bool)
                                                                                                         .orElse(false);

            callerMemberId = ofNullable(callerCognito.get(CognitoTableParameter.MEMBER.jsonFieldName())).map(AttributeValue::s)
                                                                                                        .filter(Predicate.not(String::isBlank))
                                                                                                        .orElseThrow(NullPointerException::new);
            caller = requireNonNull(this.getDdbItem(callerMemberId, DdbTable.MEMBER));
            callerFamilyId = ofNullable(caller.get(MemberTableParameter.FAMILY_ID.jsonFieldName())).map(AttributeValue::s)
                                                                                                   .filter(Predicate.not(String::isBlank))
                                                                                                   .orElseThrow(NullPointerException::new);

        } catch (final NullPointerException | ClassCastException e) {
            LambdaUtils.logTrace(this.getLogger(), e, ERROR);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_UNAUTHORIZED));
        }

        this.getLogger()
            .log("<MEMBER,`%s`> Authenticated".formatted(callerMemberId), INFO);
        return new Caller(callerMemberId, caller, callerFamilyId, isCallerAdmin);
    }

    public final @NotNull
    APIGatewayProxyRequestEvent getRequestEvent () {
        return this.requestEvent;
    }

    @Override
    public final @NotNull
    LambdaLogger getLogger () {
        return this.logger;
    }

    @Override
    public final @NotNull
    DynamoDbClient getDynamoDbClient () {
        return this.dynamoDbClient;
    }

    /**
     * This method does not distinguish between Members that do not have a Cognito Account and Members that have a Cognito Account, but their Cognito Account is not granted admin privileges
     *
     * @param memberId ID of Member in Member Table
     *
     * @return true if Member has a Cognito Account AND their Cognito Account has been granted Admin Privileges; false otherwise
     */
    public final
    boolean isMemberAdmin (final @NotNull String memberId) {
        final QueryRequest queryRequest = QueryRequest.builder()
                                                      .tableName(DdbTable.COGNITO.name())
                                                      .indexName(requireNonNull(CognitoTableParameter.MEMBER.gsiProps()).getIndexName())
                                                      .keyConditionExpression("#memberId = :memberId")
                                                      .expressionAttributeNames(singletonMap("#memberId", CognitoTableParameter.MEMBER.gsiProps()
                                                                                                                                      .getPartitionKey()
                                                                                                                                      .getName()))
                                                      .expressionAttributeValues(singletonMap(":memberId", AttributeValue.fromS(requireNonNull(memberId))))
                                                      .limit(1)
                                                      .build();
        final List<Map<String, AttributeValue>> items = this.getDynamoDbClient()
                                                            .query(queryRequest)
                                                            .items();
        return !items.isEmpty() && ofNullable(requireNonNull(this.getDdbItem(items.getFirst()
                                                                                  .get(CognitoTableParameter.ID.jsonFieldName())
                                                                                  .s(), DdbTable.COGNITO)).get(CognitoTableParameter.IS_ADMIN.jsonFieldName())).map(AttributeValue::bool)
                                                                                                                                                               .orElse(false);
    }

    public
    record Caller(@NotNull String memberId, @NotNull Map<String, AttributeValue> attributeMap, @NotNull String familyId, boolean isAdmin) {
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
