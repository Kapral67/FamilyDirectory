package org.familydirectory.assets.lambda.function.api.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.lambda.function.api.models.DeleteEvent;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.Update;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.WARN;
import static java.lang.System.getenv;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;

public final
class DeleteHelper extends ApiHelper {
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final @NotNull CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.create();
    private final @NotNull String userPoolId = getenv(LambdaUtils.EnvVar.COGNITO_USER_POOL_ID.name());
    private final @NotNull LambdaLogger logger;
    private final @NotNull APIGatewayProxyRequestEvent requestEvent;

    public
    DeleteHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) {
        super();
        this.logger = requireNonNull(logger);
        this.requestEvent = requireNonNull(requestEvent);
    }

    public @NotNull
    EventWrapper getDeleteEvent (final @NotNull Caller caller) {
        final DeleteEvent deleteEvent;
        try {
            deleteEvent = this.objectMapper.readValue(this.requestEvent.getBody(), DeleteEvent.class);
        } catch (final JsonProcessingException | IllegalArgumentException e) {
            this.logger.log("<MEMBER,`%s`> submitted invalid Delete request".formatted(caller.memberId()), WARN);
            LambdaUtils.logTrace(this.logger, e, WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_BAD_REQUEST));
        }

        final Map<String, AttributeValue> ddbMemberMap = this.getDdbItem(deleteEvent.id(), DdbTable.MEMBER);

        if (isNull(ddbMemberMap)) {
            this.logger.log("<MEMBER,`%s`> Requested Delete to Non-Existent Member <ID,`%s`>".formatted(caller.memberId(), deleteEvent.id()), WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_NOT_FOUND));
        }

        return new EventWrapper(deleteEvent, ddbMemberMap.get(MemberTableParameter.ID.jsonFieldName())
                                                         .s(), ddbMemberMap.get(MemberTableParameter.FAMILY_ID.jsonFieldName())
                                                                           .s());
    }

    public @NotNull
    TransactWriteItemsRequest buildDeleteTransaction (final @NotNull Caller caller, final @NotNull EventWrapper eventWrapper) {
        final Map<String, AttributeValue> callerFamily = ofNullable(this.getDdbItem(caller.familyId(), DdbTable.FAMILY)).orElseThrow();
        final List<TransactWriteItem> transactionItems;

        if (caller.memberId()
                  .equals(caller.familyId()) && ofNullable(callerFamily.get(FamilyTableParameter.SPOUSE.jsonFieldName())).map(AttributeValue::s)
                                                                                                                         .filter(s -> s.equals(eventWrapper.ddbMemberId()))
                                                                                                                         .isPresent())
        {
            final Update callerFamilyUpdateSpouse = Update.builder()
                                                          .tableName(DdbTable.FAMILY.name())
                                                          .key(singletonMap(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(caller.familyId())))
                                                          .updateExpression("REMOVE %s".formatted(FamilyTableParameter.SPOUSE.jsonFieldName()))
                                                          .build();
            this.logger.log("<MEMBER,`%s`> remove <SPOUSE,`%s`> from <FAMILY,`%s`>".formatted(caller.memberId(), eventWrapper.ddbMemberId(), caller.familyId()), INFO);
            final Delete ddbMemberDelete = Delete.builder()
                                                 .tableName(DdbTable.MEMBER.name())
                                                 .key(singletonMap(MemberTableParameter.ID.jsonFieldName(), AttributeValue.fromS(eventWrapper.ddbMemberId())))
                                                 .build();
            this.logger.log("<MEMBER,`%s`> delete <MEMBER,`%s`>".formatted(caller.memberId(), eventWrapper.ddbMemberId()), INFO);
            transactionItems = List.of(TransactWriteItem.builder()
                                                        .update(callerFamilyUpdateSpouse)
                                                        .build(), TransactWriteItem.builder()
                                                                                   .delete(ddbMemberDelete)
                                                                                   .build());
        } else {
            this.logger.log("<MEMBER,`%s`> attempted to delete <MEMBER,`%s`>".formatted(caller.memberId(), eventWrapper.ddbMemberId()), WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_FORBIDDEN));
        }

        return TransactWriteItemsRequest.builder()
                                        .transactItems(transactionItems)
                                        .build();
    }

    public
    void deleteCognitoAccountAndNotify (final @NotNull String ddbMemberId) {
        final QueryRequest cognitoMemberQueryRequest = QueryRequest.builder()
                                                                   .tableName(DdbTable.COGNITO.name())
                                                                   .indexName(requireNonNull(CognitoTableParameter.MEMBER.gsiProps()).getIndexName())
                                                                   .keyConditionExpression("#memberId = :memberId")
                                                                   .expressionAttributeNames(singletonMap("#memberId", CognitoTableParameter.MEMBER.gsiProps()
                                                                                                                                                   .getPartitionKey()
                                                                                                                                                   .getName()))
                                                                   .expressionAttributeValues(singletonMap(":memberId", AttributeValue.fromS(ddbMemberId)))
                                                                   .limit(1)
                                                                   .build();
        final QueryResponse cognitoMemberQueryResponse = this.dynamoDbClient.query(cognitoMemberQueryRequest);
        if (!cognitoMemberQueryResponse.items()
                                       .isEmpty())
        {
            final String ddbMemberCognitoSub = ofNullable(cognitoMemberQueryResponse.items()
                                                                                    .getFirst()
                                                                                    .get(CognitoTableParameter.ID.jsonFieldName())).map(AttributeValue::s)
                                                                                                                                   .filter(Predicate.not(String::isBlank))
                                                                                                                                   .orElseThrow();

            this.dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                                                            .tableName(DdbTable.COGNITO.name())
                                                            .key(singletonMap(CognitoTableParameter.ID.jsonFieldName(), AttributeValue.fromS(ddbMemberCognitoSub)))
                                                            .build());
            this.logger.log("COGNITO Table Entry Deleted for <MEMBER,`%s`>: <COGNITO_SUB,`%s`>".formatted(ddbMemberId, ddbMemberCognitoSub), INFO);

            final ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                                                                      .filter("sub = \"%s\"".formatted(ddbMemberCognitoSub))
                                                                      .limit(1)
                                                                      .userPoolId(this.userPoolId)
                                                                      .build();
            final UserType ddbMemberCognitoUser = ofNullable(this.cognitoClient.listUsers(listUsersRequest)).filter(ListUsersResponse::hasUsers)
                                                                                                            .map(ListUsersResponse::users)
                                                                                                            .map(List::getFirst)
                                                                                                            .orElseThrow();
            final String ddbMemberCognitoUsername = Optional.of(ddbMemberCognitoUser)
                                                            .map(UserType::username)
                                                            .filter(Predicate.not(String::isBlank))
                                                            .orElseThrow();
            this.cognitoClient.adminDeleteUser(AdminDeleteUserRequest.builder()
                                                                     .userPoolId(this.userPoolId)
                                                                     .username(ddbMemberCognitoUsername)
                                                                     .build());
            this.logger.log("Cognito Account Deleted for <MEMBER,`%s`>: <USERNAME,`%s`>".formatted(ddbMemberId, ddbMemberCognitoUsername), INFO);

            final String ddbMemberCognitoEmail = Optional.of(ddbMemberCognitoUser)
                                                         .filter(UserType::hasAttributes)
                                                         .map(UserType::attributes)
                                                         .orElseThrow()
                                                         .stream()
                                                         .filter(attr -> attr.name()
                                                                             .equalsIgnoreCase("email"))
                                                         .findFirst()
                                                         .map(AttributeType::value)
                                                         .filter(s -> s.contains("@"))
                                                         .orElseThrow();
            final String emailId = LambdaUtils.sendEmail(singletonList(ddbMemberCognitoEmail), "Notice of Account Deletion", "Your account has been irreversibly deleted.");
            this.logger.log("Sent Account Deletion Notice To <EMAIL,`%s`>: <MESSAGE_ID,`%s`>".formatted(ddbMemberCognitoEmail, emailId), INFO);
        }
    }

    @Override
    public @NotNull
    LambdaLogger getLogger () {
        return this.logger;
    }

    @Override
    public @NotNull
    DynamoDbClient getDynamoDbClient () {
        return this.dynamoDbClient;
    }

    @Override
    public
    void close () {
        super.close();
        this.cognitoClient.close();
    }

    @Override
    public @NotNull
    APIGatewayProxyRequestEvent getRequestEvent () {
        return this.requestEvent;
    }

    public
    record EventWrapper(@NotNull DeleteEvent deleteEvent, @NotNull String ddbMemberId, @NotNull String ddbFamilyId) {
    }
}
