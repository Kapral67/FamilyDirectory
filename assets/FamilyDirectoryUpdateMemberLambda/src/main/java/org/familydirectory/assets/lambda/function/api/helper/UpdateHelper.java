package org.familydirectory.assets.lambda.function.api.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.assets.lambda.function.api.models.UpdateEvent;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.ERROR;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.WARN;
import static java.util.Collections.singletonMap;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;

public final
class UpdateHelper extends ApiHelper {
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final @NotNull LambdaLogger logger;
    private final @NotNull APIGatewayProxyRequestEvent requestEvent;

    public
    UpdateHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) {
        super();
        this.logger = requireNonNull(logger);
        this.requestEvent = requireNonNull(requestEvent);
    }

    public @NotNull
    EventWrapper getUpdateEvent (final @NotNull Caller caller) {
        final UpdateEvent updateEvent;
        try {
            updateEvent = this.objectMapper.convertValue(this.requestEvent.getBody(), UpdateEvent.class);
        } catch (final IllegalArgumentException e) {
            this.logger.log("<MEMBER,`%s`> submitted invalid Update request".formatted(caller.memberId()), WARN);
            LambdaUtils.logTrace(this.logger, e, WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_BAD_REQUEST));
        }

        final QueryResponse response = this.dynamoDbClient.query(QueryRequest.builder()
                                                                             .tableName(DdbTable.MEMBER.name())
                                                                             .indexName(requireNonNull(MemberTableParameter.KEY.gsiProps()).getIndexName())
                                                                             .keyConditionExpression("%s = :key".formatted(MemberTableParameter.KEY.gsiProps()
                                                                                                                                                   .getPartitionKey()
                                                                                                                                                   .getName()))
                                                                             .expressionAttributeValues(singletonMap(":key", AttributeValue.fromS(updateEvent.member()
                                                                                                                                                             .getKey())))
                                                                             .limit(2)
                                                                             .build());

        if (response.items()
                    .isEmpty())
        {
            this.logger.log("<MEMBER,`%s`> Requested Update to Non-Existent Member <KEY,`%s`>".formatted(caller.memberId(), updateEvent.member()
                                                                                                                                       .getKey()), WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_NOT_FOUND));
        } else if (response.items()
                           .size() > 1)
        {
            this.logger.log("<MEMBER,`%s`> Requested Update to Ambiguous <KEY,`%s`> Referencing Multiple Members".formatted(caller.memberId(), updateEvent.member()
                                                                                                                                                          .getKey()), ERROR);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_INTERNAL_SERVER_ERROR));
        }

        final Map<String, AttributeValue> ddbMemberMap = response.items()
                                                                 .get(0);
        final String ddbMemberId = ddbMemberMap.get(MemberTableParameter.ID.jsonFieldName())
                                               .s();
        final String ddbFamilyId = ddbMemberMap.get(MemberTableParameter.FAMILY_ID.jsonFieldName())
                                               .s();
        final String ddbMemberEmail = ofNullable(ddbMemberMap.get(MemberTableParameter.EMAIL.jsonFieldName())).map(AttributeValue::s)
                                                                                                              .filter(s -> s.contains("@"))
                                                                                                              .orElse(null);
        final String updateMemberEmail = ofNullable(updateEvent.member()
                                                               .getEmail()).filter(s -> s.contains("@"))
                                                                           .orElse(null);

        if (nonNull(updateMemberEmail) && !Objects.equals(ddbMemberEmail, updateMemberEmail)) {
            final QueryRequest emailRequest = QueryRequest.builder()
                                                          .tableName(DdbTable.MEMBER.name())
                                                          .indexName(requireNonNull(MemberTableParameter.EMAIL.gsiProps()).getIndexName())
                                                          .keyConditionExpression("%s = :email".formatted(MemberTableParameter.EMAIL.gsiProps()
                                                                                                                                    .getPartitionKey()
                                                                                                                                    .getName()))
                                                          .expressionAttributeValues(singletonMap(":email", AttributeValue.fromS(updateMemberEmail)))
                                                          .limit(1)
                                                          .build();
            final QueryResponse emailResponse = this.dynamoDbClient.query(emailRequest);
            if (!emailResponse.items()
                              .isEmpty())
            {
                final String emailResponseMemberId = emailResponse.items()
                                                                  .get(0)
                                                                  .get(MemberTableParameter.ID.jsonFieldName())
                                                                  .s();
                this.logger.log("<MEMBER,`%s`> Requested Update For <MEMBER,`%s`>, but <MEMBER,`%s`> Already Claims <EMAIL,`%s`>".formatted(caller.memberId(), ddbMemberId, emailResponseMemberId,
                                                                                                                                            updateMemberEmail), WARN);
                throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_CONFLICT)
                                                                              .withBody("EMAIL Already Registered With Another Member"));
            }
        }

        final boolean ddbMemberIsAdult = DdbUtils.isPersonAdult(LocalDate.parse(ddbMemberMap.get(MemberTableParameter.BIRTHDAY.jsonFieldName())
                                                                                            .s(), DdbUtils.DATE_FORMATTER),
                                                                ofNullable(ddbMemberMap.get(MemberTableParameter.DEATHDAY.jsonFieldName())).map(AttributeValue::s)
                                                                                                                                           .filter(Predicate.not(String::isBlank))
                                                                                                                                           .map(s -> LocalDate.parse(s, DdbUtils.DATE_FORMATTER))
                                                                                                                                           .orElse(null));
        return new EventWrapper(updateEvent, ddbMemberId, ddbFamilyId, ddbMemberIsAdult);
    }

    public @NotNull
    PutItemRequest getPutRequest (final @NotNull Caller caller, final @NotNull EventWrapper eventWrapper) {
        final Map<String, AttributeValue> callerFamily = ofNullable(this.getDdbItem(caller.familyId(), DdbTable.FAMILY)).orElseThrow();

        if (caller.memberId()
                  .equals(eventWrapper.ddbMemberId()))
        {
            this.logger.log("<MEMBER,`%s`> update SELF".formatted(caller.memberId()), INFO);
        } else if (caller.memberId()
                         .equals(eventWrapper.ddbFamilyId()) || caller.familyId()
                                                                      .equals(eventWrapper.ddbMemberId()))
        {
            this.logger.log("<MEMBER,`%s`> update <SPOUSE,`%s`>".formatted(caller.memberId(), eventWrapper.ddbMemberId()), INFO);
        } else if (!eventWrapper.ddbMemberIsAdult() && ofNullable(callerFamily.get(FamilyTableParameter.DESCENDANTS.jsonFieldName())).filter(AttributeValue::hasSs)
                                                                                                                                     .map(AttributeValue::ss)
                                                                                                                                     .filter(ss -> ss.contains(eventWrapper.ddbMemberId()))
                                                                                                                                     .isPresent())
        {
            this.logger.log("<MEMBER,`%s`> update <DESCENDANT,`%s`>".formatted(caller.memberId(), eventWrapper.ddbMemberId()), INFO);
        } else {
            this.logger.log("<MEMBER,`%s`> attempted to update <MEMBER,`%s`>".formatted(caller.memberId(), eventWrapper.ddbMemberId()), WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_FORBIDDEN));
        }

        return PutItemRequest.builder()
                             .tableName(DdbTable.MEMBER.name())
                             .item(this.buildMember(eventWrapper))
                             .build();
    }

    private @NotNull
    Map<String, AttributeValue> buildMember (final @NotNull EventWrapper eventWrapper) {
        final Map<String, AttributeValue> member = new HashMap<>();

        for (final MemberTableParameter field : MemberTableParameter.values()) {
            switch (field) {
                case ID -> member.put(field.jsonFieldName(), AttributeValue.fromS(eventWrapper.ddbMemberId()));
                case KEY -> member.put(field.jsonFieldName(), AttributeValue.fromS(eventWrapper.updateEvent()
                                                                                               .member()
                                                                                               .getKey()));
                case FIRST_NAME -> member.put(field.jsonFieldName(), AttributeValue.fromS(eventWrapper.updateEvent()
                                                                                                      .member()
                                                                                                      .getFirstName()));
                case MIDDLE_NAME -> ofNullable(eventWrapper.updateEvent()
                                                           .member()
                                                           .getMiddleName()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s)));
                case LAST_NAME -> member.put(field.jsonFieldName(), AttributeValue.fromS(eventWrapper.updateEvent()
                                                                                                     .member()
                                                                                                     .getLastName()));
                case SUFFIX -> ofNullable(eventWrapper.updateEvent()
                                                      .member()
                                                      .getSuffix()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s.value())));
                case BIRTHDAY -> member.put(field.jsonFieldName(), AttributeValue.fromS(eventWrapper.updateEvent()
                                                                                                    .member()
                                                                                                    .getBirthdayString()));
                case DEATHDAY -> ofNullable(eventWrapper.updateEvent()
                                                        .member()
                                                        .getDeathdayString()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s)));
                case EMAIL -> ofNullable(eventWrapper.updateEvent()
                                                     .member()
                                                     .getEmail()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s)));
                case PHONES -> ofNullable(eventWrapper.updateEvent()
                                                      .member()
                                                      .getPhonesDdbMap()).ifPresent(m -> member.put(field.jsonFieldName(), AttributeValue.fromM(m)));
                case ADDRESS -> ofNullable(eventWrapper.updateEvent()
                                                       .member()
                                                       .getAddress()).ifPresent(ss -> member.put(field.jsonFieldName(), AttributeValue.fromSs(ss)));
                case FAMILY_ID -> member.put(field.jsonFieldName(), AttributeValue.fromS(eventWrapper.ddbFamilyId()));
                default -> throw new IllegalStateException("Unhandled Member Parameter: `%s`".formatted(field.jsonFieldName()));
            }
        }

        return member;
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
    public @NotNull
    APIGatewayProxyRequestEvent getRequestEvent () {
        return this.requestEvent;
    }

    public
    record EventWrapper(@NotNull UpdateEvent updateEvent, @NotNull String ddbMemberId, @NotNull String ddbFamilyId, boolean ddbMemberIsAdult) {
    }
}
