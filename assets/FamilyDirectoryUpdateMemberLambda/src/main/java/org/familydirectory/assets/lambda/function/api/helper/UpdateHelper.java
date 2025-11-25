package org.familydirectory.assets.lambda.function.api.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.assets.lambda.function.api.models.UpdateEvent;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.DEBUG;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.WARN;
import static java.lang.System.getenv;
import static java.util.Collections.singletonMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

public final
class UpdateHelper extends ApiHelper {
    private static final @NotNull String USER_POOL_ID = requireNonNull(getenv(LambdaUtils.EnvVar.COGNITO_USER_POOL_ID.name()));
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.create();

    public
    UpdateHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) {
        super(logger, requestEvent);
    }

    public @NotNull
    EventWrapper getUpdateEvent (final @NotNull Caller caller) throws ResponseException {
        final UpdateEvent updateEvent;
        try {
            updateEvent = this.objectMapper.readValue(this.requestEvent.getBody(), UpdateEvent.class);
            this.logger.log(updateEvent.member()
                                       .toString(), DEBUG);
        } catch (final JsonProcessingException | IllegalArgumentException e) {
            this.logger.log("<MEMBER,`%s`> submitted invalid Update request".formatted(caller.caller().id().toString()), WARN);
            LambdaUtils.logTrace(this.logger, e, WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_BAD_REQUEST));
        }

        final Map<String, AttributeValue> ddbMemberMap = this.getDdbItem(updateEvent.id(), DdbTable.MEMBER);

        if (isNull(ddbMemberMap)) {
            this.logger.log("<MEMBER,`%s`> Requested Update to Non-Existent Member <ID,`%s`>".formatted(caller.caller().id().toString(), updateEvent.id()), WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_NOT_FOUND));
        }

        final MemberRecord ddbMemberRecord = MemberRecord.convertDdbMap(ddbMemberMap);

        boolean shouldDeleteCognito = false;
        final String updateMemberEmail = updateEvent.member()
                                                    .getEmail();
        if (nonNull(updateMemberEmail) && !updateMemberEmail.equals(ddbMemberRecord.member().getEmail())) {
            final GlobalSecondaryIndexProps emailGsiProps = requireNonNull(MemberTableParameter.EMAIL.gsiProps());
            final QueryRequest emailRequest = QueryRequest.builder()
                                                          .tableName(DdbTable.MEMBER.name())
                                                          .indexName(emailGsiProps.getIndexName())
                                                          .keyConditionExpression("%s = :email".formatted(emailGsiProps.getPartitionKey()
                                                                                                                       .getName()))
                                                          .expressionAttributeValues(singletonMap(":email", AttributeValue.fromS(updateMemberEmail)))
                                                          .limit(1)
                                                          .build();
            final QueryResponse emailResponse = this.dynamoDbClient.query(emailRequest);
            if (!emailResponse.items()
                              .isEmpty())
            {
                final String emailResponseMemberId = emailResponse.items()
                                                                  .getFirst()
                                                                  .get(MemberTableParameter.ID.jsonFieldName())
                                                                  .s();
                this.logger.log("<MEMBER,`%s`> Requested Update For <MEMBER,`%s`>, but <MEMBER,`%s`> Already Claims <EMAIL,`%s`>".formatted(caller.caller().id().toString(), updateEvent.id(), emailResponseMemberId,
                                                                                                                                            updateMemberEmail), WARN);
                throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_CONFLICT)
                                                                              .withBody("EMAIL Already Registered With Another Member"));
            }
            shouldDeleteCognito = true;
        }

        final boolean ddbMemberIsSuperAdult = ddbMemberRecord.member().getAge() >= DdbUtils.AGE_OF_SUPER_MAJORITY;
        return new EventWrapper(updateEvent, ddbMemberRecord, ddbMemberIsSuperAdult, shouldDeleteCognito);
    }

    public @NotNull
    PutItemRequest getPutRequest (final @NotNull Caller caller, final @NotNull EventWrapper eventWrapper) throws ResponseException {
        if (caller.isAdmin()) {
            this.logger.log("ADMIN <MEMBER,`%s`> update <MEMBER,`%s`>".formatted(caller.caller().id().toString(), eventWrapper.updateEvent().id()), INFO);
        } else if (caller.caller().id().toString().equals(eventWrapper.updateEvent().id())) {
            this.logger.log("<MEMBER,`%s`> update SELF".formatted(caller.caller().id().toString()), INFO);
        } else if (caller.caller().id().toString().equals(eventWrapper.ddbMemberRecord().familyId().toString()) || caller.caller().familyId().toString().equals(eventWrapper.updateEvent().id())) {
            this.logger.log("<MEMBER,`%s`> update <SPOUSE,`%s`>".formatted(caller.caller().id().toString(), eventWrapper.updateEvent().id()), INFO);
        } else if (!eventWrapper.ddbMemberIsSuperAdult() && Optional.ofNullable(this.getDdbItem(caller.caller().familyId().toString(), DdbTable.FAMILY))
                                                                    .map(map -> map.get(FamilyTableParameter.DESCENDANTS.jsonFieldName()))
                                                                    .map(AttributeValue::ss)
                                                                    .filter(Predicate.not(List::isEmpty))
                                                                    .filter(ss -> ss.contains(eventWrapper.updateEvent().id()))
                                                                    .isPresent())
        {
            this.logger.log("<MEMBER,`%s`> update <DESCENDANT,`%s`>".formatted(caller.caller().id().toString(), eventWrapper.updateEvent().id()), INFO);
        } else {
            this.logger.log("<MEMBER,`%s`> attempted to update <MEMBER,`%s`>".formatted(caller.caller().id().toString(), eventWrapper.updateEvent().id()), WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_FORBIDDEN));
        }

        if (eventWrapper.ddbMemberRecord().member().getEtag().equals(eventWrapper.updateEvent().member().getEtag())) {
            this.logger.log("<MEMBER,`%s`> update <MEMBER,`%s`> without modifications".formatted(caller.caller().id().toString(), eventWrapper.updateEvent().id()), WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_OK));
        }

        eventWrapper.updateEvent().member().setLastModifiedNow();

        final Map<String, AttributeValue> item = Member.retrieveDdbMap(new MemberRecord(
            UUID.fromString(eventWrapper.updateEvent().id()),
            eventWrapper.updateEvent().member(),
            eventWrapper.ddbMemberRecord().familyId()
        ));

        this.logger.log(Member.convertDdbMap(item)
                              .toString(), DEBUG);

        return PutItemRequest.builder()
                             .tableName(DdbTable.MEMBER.name())
                             .item(item)
                             .build();
    }

    public
    void deleteCognitoAccountAndNotify(final String ddbMemberId, final String newEmail) {
        final var notifyEmailAddresses = new HashSet<String>();
        notifyEmailAddresses.add(newEmail);
        final GlobalSecondaryIndexProps cognitoGsiProps = requireNonNull(CognitoTableParameter.MEMBER.gsiProps());
        final QueryRequest cognitoMemberQueryRequest = QueryRequest.builder()
                                                                   .tableName(DdbTable.COGNITO.name())
                                                                   .indexName(cognitoGsiProps.getIndexName())
                                                                   .keyConditionExpression("#memberId = :memberId")
                                                                   .expressionAttributeNames(singletonMap("#memberId", cognitoGsiProps.getPartitionKey()
                                                                                                                                      .getName()))
                                                                   .expressionAttributeValues(singletonMap(":memberId", AttributeValue.fromS(ddbMemberId)))
                                                                   .limit(1)
                                                                   .build();
        final QueryResponse cognitoMemberQueryResponse = this.dynamoDbClient.query(cognitoMemberQueryRequest);
        if (!cognitoMemberQueryResponse.items()
                                       .isEmpty())
        {
            final String ddbMemberCognitoSub = Optional.ofNullable(cognitoMemberQueryResponse.items().getFirst().get(CognitoTableParameter.ID.jsonFieldName()))
                                                       .map(AttributeValue::s)
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
                                                                      .userPoolId(USER_POOL_ID)
                                                                      .build();
            final UserType ddbMemberCognitoUser = Optional.ofNullable(this.cognitoClient.listUsers(listUsersRequest))
                                                          .filter(ListUsersResponse::hasUsers)
                                                          .map(ListUsersResponse::users)
                                                          .map(List::getFirst)
                                                          .orElseThrow();
            final String ddbMemberCognitoUsername = Optional.of(ddbMemberCognitoUser)
                                                            .map(UserType::username)
                                                            .filter(Predicate.not(String::isBlank))
                                                            .orElseThrow();
            this.cognitoClient.adminDeleteUser(AdminDeleteUserRequest.builder()
                                                                     .userPoolId(USER_POOL_ID)
                                                                     .username(ddbMemberCognitoUsername)
                                                                     .build());
            this.logger.log("Cognito Account Deleted for <MEMBER,`%s`>: <USERNAME,`%s`>".formatted(ddbMemberId, ddbMemberCognitoUsername), INFO);

            Optional.of(ddbMemberCognitoUser)
                    .filter(UserType::hasAttributes)
                    .map(UserType::attributes)
                    .stream()
                    .flatMap(List::stream)
                    .filter(attr -> attr.name().equalsIgnoreCase("email"))
                    .findFirst()
                    .map(AttributeType::value)
                    .filter(s -> s.contains("@"))
                    .ifPresent(notifyEmailAddresses::add);
            final String emailId = LambdaUtils.sendEmail(
                notifyEmailAddresses.stream().toList(),
                "Notice of Account Deletion",
                "Your old account was deleted due to an email address change. Please sign up again with your new email."
            );
            this.logger.log("Sent Account Deletion Notice To <EMAIL's,`%s`>: <MESSAGE_ID,`%s`>".formatted(notifyEmailAddresses, emailId), INFO);
        }
    }

    @Override
    public
    void close () {
        super.close();
        this.cognitoClient.close();
    }

    public
    record EventWrapper(@NotNull UpdateEvent updateEvent, @NotNull MemberRecord ddbMemberRecord, boolean ddbMemberIsSuperAdult, boolean shouldDeleteCognito) {
    }
}
