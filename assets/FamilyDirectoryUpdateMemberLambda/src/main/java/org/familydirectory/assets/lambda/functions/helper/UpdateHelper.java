package org.familydirectory.assets.lambda.functions.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.PhoneType;
import org.familydirectory.assets.ddb.enums.SuffixType;
import org.familydirectory.assets.ddb.enums.member.MemberParams;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.lambda.models.UpdateEvent;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.ERROR;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.WARN;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.familydirectory.assets.ddb.enums.family.FamilyParams.DESCENDANTS;
import static org.familydirectory.assets.ddb.utils.DdbUtils.DATE_FORMATTER;

public final
class UpdateHelper extends ApiHelper {
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final @NotNull LambdaLogger logger;
    private final @NotNull APIGatewayProxyRequestEvent requestEvent;

    public
    UpdateHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) {
        this.logger = requireNonNull(logger);
        this.requestEvent = requireNonNull(requestEvent);
    }

    public @NotNull
    EventWrapper getUpdateEvent (final @NotNull Caller caller) {
        final UpdateEvent updateEvent;
        try {
            updateEvent = this.objectMapper.convertValue(this.requestEvent.getBody(), UpdateEvent.class);
        } catch (final IllegalArgumentException e) {
            this.logger.log("<MEMBER,`%s`> submitted invalid UpdateMember request".formatted(caller.memberId()), WARN);
            this.logTrace(e, WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_BAD_REQUEST));
        }

        final QueryResponse response = this.dynamoDbClient.query(QueryRequest.builder()
                                                                             .tableName(DdbTable.MEMBERS.name())
                                                                             .indexName(requireNonNull(MemberParams.KEY.gsiProps()).getIndexName())
                                                                             .keyConditionExpression("%s = :key".formatted(MemberParams.KEY.gsiProps()
                                                                                                                                           .getPartitionKey()
                                                                                                                                           .getName()))
                                                                             .expressionAttributeValues(singletonMap(":key", AttributeValue.fromS(updateEvent.getMember()
                                                                                                                                                             .getKey())))
                                                                             .limit(2)
                                                                             .build());

        if (!response.hasItems()) {
            this.logger.log("<MEMBER,`%s`> Requested Update to Non-Existent Member `%s`".formatted(caller.memberId(), updateEvent.getMember()
                                                                                                                                 .getKey()), WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_NOT_FOUND));
        } else if (response.items()
                           .size() > 1)
        {
            this.logger.log("<MEMBER,`%s`> Multiple Members with Key `%s` found in DDB".formatted(caller.memberId(), updateEvent.getMember()
                                                                                                                                .getKey()), ERROR);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_INTERNAL_SERVER_ERROR));
        }

        final Map<String, AttributeValue> ddbMemberMap = response.items()
                                                                 .get(0);
        final Member ddbMember;
        try {
            ddbMember = Member.builder()
                              .firstName(ddbMemberMap.get(MemberParams.FIRST_NAME.jsonFieldName())
                                                     .s())
                              .middleName(ofNullable(ddbMemberMap.get(MemberParams.MIDDLE_NAME.jsonFieldName())).map(AttributeValue::s)
                                                                                                                .filter(Predicate.not(String::isBlank))
                                                                                                                .orElse(null))
                              .lastName(ddbMemberMap.get(MemberParams.LAST_NAME.jsonFieldName())
                                                    .s())
                              .birthday(LocalDate.parse(ddbMemberMap.get(MemberParams.BIRTHDAY.jsonFieldName())
                                                                    .s(), DATE_FORMATTER))
                              .email(ofNullable(ddbMemberMap.get(MemberParams.EMAIL.jsonFieldName())).map(AttributeValue::s)
                                                                                                     .filter(Predicate.not(String::isBlank))
                                                                                                     .orElse(null))
                              .deathday(ofNullable(ddbMemberMap.get(MemberParams.DEATHDAY.jsonFieldName())).map(AttributeValue::s)
                                                                                                           .filter(Predicate.not(String::isBlank))
                                                                                                           .map(s -> LocalDate.parse(s, DATE_FORMATTER))
                                                                                                           .orElse(null))
                              .phones(ofNullable(ddbMemberMap.get(MemberParams.PHONES.jsonFieldName())).filter(AttributeValue::hasM)
                                                                                                       .map(AttributeValue::m)
                                                                                                       .filter(Predicate.not(Map::isEmpty))
                                                                                                       .map(m -> m.entrySet()
                                                                                                                  .stream()
                                                                                                                  .filter(v -> ofNullable(v.getValue()).map(AttributeValue::s)
                                                                                                                                                       .filter(Predicate.not(String::isBlank))
                                                                                                                                                       .isPresent())
                                                                                                                  .collect(toMap(k -> PhoneType.forValue(k.getKey()), v -> v.getValue()
                                                                                                                                                                            .s())))
                                                                                                       .orElse(null))
                              .address(ofNullable(ddbMemberMap.get(MemberParams.ADDRESS.jsonFieldName())).filter(AttributeValue::hasSs)
                                                                                                         .map(AttributeValue::ss)
                                                                                                         .map(ss -> ss.stream()
                                                                                                                      .filter(Predicate.not(String::isBlank))
                                                                                                                      .toList())
                                                                                                         .filter(Predicate.not(List::isEmpty))
                                                                                                         .orElse(null))
                              .suffix(ofNullable(ddbMemberMap.get(MemberParams.SUFFIX.jsonFieldName())).map(AttributeValue::s)
                                                                                                       .filter(Predicate.not(String::isBlank))
                                                                                                       .map(SuffixType::forValue)
                                                                                                       .orElse(null))
                              .build();
            if (!ddbMemberMap.get(MemberParams.KEY.jsonFieldName())
                             .s()
                             .equals(ddbMember.getKey()))
            {
                throw new IllegalStateException();
            }
        } catch (final Exception e) {
            this.logger.log("<MEMBER,`%s`> discovered invalid Member in DDB: `%s`".formatted(caller.memberId(), updateEvent.getMember()
                                                                                                                           .getKey()), ERROR);
            this.logTrace(e, ERROR);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_INTERNAL_SERVER_ERROR));
        }

        return new EventWrapper(updateEvent, ddbMember, ddbMemberMap.get(MemberParams.ID.jsonFieldName())
                                                                    .s(), ddbMemberMap.get(MemberParams.FAMILY_ID.jsonFieldName())
                                                                                      .s());
    }

    public @NotNull
    TransactWriteItemsRequest buildUpdateTransaction (final @NotNull Caller caller, final @NotNull EventWrapper eventWrapper) {
        final List<TransactWriteItem> transactionItems = new ArrayList<>();
        final Map<String, AttributeValue> callerFamily = ofNullable(this.getDdbItem(caller.familyId(), DdbTable.FAMILIES)).orElseThrow();

        if (caller.memberId()
                  .equals(eventWrapper.ddbMemberId()))
        {
            // TODO: UPDATE SELF (CALLER)
            this.logger.log("<MEMBER,`%s`> update SELF".formatted(caller.memberId()), INFO);
        } else if (caller.memberId()
                         .equals(eventWrapper.ddbFamilyId()) || caller.familyId()
                                                                      .equals(eventWrapper.ddbMemberId()))
        {
            // TODO: UPDATE SPOUSE
            this.logger.log("<MEMBER,`%s`> update <SPOUSE,`%s`>".formatted(caller.memberId(), eventWrapper.ddbMemberId()), INFO);
        } else if (ofNullable(callerFamily.get(DESCENDANTS.jsonFieldName())).filter(AttributeValue::hasSs)
                                                                            .map(AttributeValue::ss)
                                                                            .filter(ss -> ss.contains(eventWrapper.ddbMemberId()))
                                                                            .isPresent())
        {
            // TODO: UPDATE DESCENDANT
            this.logger.log("<MEMBER,`%s`> update <DESCENDANT,`%s`>".formatted(caller.memberId(), eventWrapper.ddbMemberId()), INFO);
        } else {
            this.logger.log("<MEMBER,`%s`> attempted to update <MEMBER,`%s`>".formatted(caller.memberId(), eventWrapper.ddbMemberId()), WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_UNAUTHORIZED));
        }
        return TransactWriteItemsRequest.builder()
                                        .build();
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

    public
    record EventWrapper(@NotNull UpdateEvent updateEvent, @NotNull Member ddbMember, @NotNull String ddbMemberId, @NotNull String ddbFamilyId) {
    }
}
