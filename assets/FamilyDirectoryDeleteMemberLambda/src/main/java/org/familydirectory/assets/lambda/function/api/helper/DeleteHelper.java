package org.familydirectory.assets.lambda.function.api.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.lambda.function.LambdaUtils;
import org.familydirectory.assets.lambda.function.api.models.DeleteEvent;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.ERROR;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.WARN;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;

public final
class DeleteHelper extends ApiHelper {
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final @NotNull CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.create();
    private final @NotNull String userPoolId;
    private final @NotNull LambdaLogger logger;
    private final @NotNull APIGatewayProxyRequestEvent requestEvent;

    public
    DeleteHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) {
        this.logger = requireNonNull(logger);
        this.requestEvent = requireNonNull(requestEvent);
        this.userPoolId = this.retrieveUserPoolId();
    }

    private @NotNull
    String retrieveUserPoolId () {
        final ListUserPoolsResponse response = this.cognitoClient.listUserPools(ListUserPoolsRequest.builder()
                                                                                                    .maxResults(1)
                                                                                                    .build());
        if (!response.hasUserPools()) {
            throw new IllegalStateException("No User Pools Found");
        }
        return response.userPools()
                       .get(0)
                       .id();
    }

    public @NotNull
    EventWrapper getDeleteEvent (final @NotNull Caller caller) {
        final DeleteEvent deleteEvent;
        try {
            deleteEvent = this.objectMapper.convertValue(this.requestEvent.getBody(), DeleteEvent.class);
        } catch (final IllegalArgumentException e) {
            this.logger.log("<MEMBER,`%s`> submitted invalid Delete request".formatted(caller.memberId()), WARN);
            LambdaUtils.logTrace(this.logger, e, WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_BAD_REQUEST));
        }

        final QueryResponse response = this.dynamoDbClient.query(QueryRequest.builder()
                                                                             .tableName(DdbTable.MEMBER.name())
                                                                             .indexName(requireNonNull(MemberTableParameter.KEY.gsiProps()).getIndexName())
                                                                             .keyConditionExpression("%s = :key".formatted(MemberTableParameter.KEY.gsiProps()
                                                                                                                                                   .getPartitionKey()
                                                                                                                                                   .getName()))
                                                                             .expressionAttributeValues(singletonMap(":key", AttributeValue.fromS(deleteEvent.member()
                                                                                                                                                             .getKey())))
                                                                             .limit(2)
                                                                             .build());

        if (!response.hasItems()) {
            this.logger.log("<MEMBER,`%s`> Requested Delete to Non-Existent Member <KEY,`%s`>".formatted(caller.memberId(), deleteEvent.member()
                                                                                                                                       .getKey()), WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_NOT_FOUND));
        } else if (response.items()
                           .size() > 1)
        {
            this.logger.log("<MEMBER,`%s`> Requested Delete to Ambiguous <KEY,`%s`> Referencing Multiple Members".formatted(caller.memberId(), deleteEvent.member()
                                                                                                                                                          .getKey()), ERROR);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_INTERNAL_SERVER_ERROR));
        }

        final Map<String, AttributeValue> ddbMemberMap = response.items()
                                                                 .get(0);
        return new EventWrapper(deleteEvent, ddbMemberMap.get(MemberTableParameter.ID.jsonFieldName())
                                                         .s(), ddbMemberMap.get(MemberTableParameter.FAMILY_ID.jsonFieldName())
                                                                           .s());
    }

    @Override
    public @NotNull
    LambdaLogger getLogger () {
        return this.logger;
    }

    @Override
    public @NotNull
    APIGatewayProxyRequestEvent getRequestEvent () {
        return this.requestEvent;
    }

    @Override
    public @NotNull
    DynamoDbClient getDynamoDbClient () {
        return this.dynamoDbClient;
    }

    public @NotNull
    CognitoIdentityProviderClient getCognitoClient () {
        return this.cognitoClient;
    }

    public @NotNull
    String getUserPoolId () {
        return this.userPoolId;
    }

    public
    record EventWrapper(@NotNull DeleteEvent deleteEvent, @NotNull String ddbMemberId, @NotNull String ddbFamilyId) {
    }
}
