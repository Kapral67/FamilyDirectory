package org.familydirectory.assets.lambda.function.api.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.assets.lambda.function.api.models.UpdateEvent;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.DEBUG;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.WARN;
import static java.util.Collections.singletonMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;

public final
class UpdateHelper extends ApiHelper {
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();

    public
    UpdateHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) {
        super(logger, requestEvent);
    }

    public @NotNull
    EventWrapper getUpdateEvent (final @NotNull Caller caller) {
        final UpdateEvent updateEvent;
        try {
            updateEvent = this.objectMapper.readValue(this.requestEvent.getBody(), UpdateEvent.class);
            this.logger.log(updateEvent.member()
                                       .toString(), DEBUG);
        } catch (final JsonProcessingException | IllegalArgumentException e) {
            this.logger.log("<MEMBER,`%s`> submitted invalid Update request".formatted(caller.memberId()), WARN);
            LambdaUtils.logTrace(this.logger, e, WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_BAD_REQUEST));
        }

        final Map<String, AttributeValue> ddbMemberMap = this.getDdbItem(updateEvent.id(), DdbTable.MEMBER);

        if (isNull(ddbMemberMap)) {
            this.logger.log("<MEMBER,`%s`> Requested Update to Non-Existent Member <ID,`%s`>".formatted(caller.memberId(), updateEvent.id()), WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_NOT_FOUND));
        }

        final String ddbFamilyId = ddbMemberMap.get(MemberTableParameter.FAMILY_ID.jsonFieldName())
                                               .s();
        final String ddbMemberEmail = ofNullable(ddbMemberMap.get(MemberTableParameter.EMAIL.jsonFieldName())).map(AttributeValue::s)
                                                                                                              .orElse(null);
        final String updateMemberEmail = updateEvent.member()
                                                    .getEmail();

        if (nonNull(updateMemberEmail) && !updateMemberEmail.equals(ddbMemberEmail)) {
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
                                                                  .getFirst()
                                                                  .get(MemberTableParameter.ID.jsonFieldName())
                                                                  .s();
                this.logger.log("<MEMBER,`%s`> Requested Update For <MEMBER,`%s`>, but <MEMBER,`%s`> Already Claims <EMAIL,`%s`>".formatted(caller.memberId(), updateEvent.id(), emailResponseMemberId,
                                                                                                                                            updateMemberEmail), WARN);
                throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_CONFLICT)
                                                                              .withBody("EMAIL Already Registered With Another Member"));
            }
        }

        final LocalDate ddbMemberBirthday = LocalDate.parse(ddbMemberMap.get(MemberTableParameter.BIRTHDAY.jsonFieldName())
                                                                        .s(), DdbUtils.DATE_FORMATTER);
        final LocalDate ddbMemberDeathday = ofNullable(ddbMemberMap.get(MemberTableParameter.DEATHDAY.jsonFieldName())).map(AttributeValue::s)
                                                                                                                       .filter(Predicate.not(String::isBlank))
                                                                                                                       .map(s -> LocalDate.parse(s, DdbUtils.DATE_FORMATTER))
                                                                                                                       .orElse(null);
        final boolean ddbMemberIsSuperAdult = DdbUtils.getPersonAge(ddbMemberBirthday, ddbMemberDeathday) >= DdbUtils.AGE_OF_SUPER_MAJORITY;
        return new EventWrapper(updateEvent, ddbFamilyId, ddbMemberIsSuperAdult);
    }

    public @NotNull
    PutItemRequest getPutRequest (final @NotNull Caller caller, final @NotNull EventWrapper eventWrapper) {
        if (caller.isAdmin()) {
            this.logger.log("ADMIN <MEMBER,`%s`> update <MEMBER,`%s`>".formatted(caller.memberId(), eventWrapper.updateEvent()
                                                                                                                .id()), INFO);
        } else if (caller.memberId()
                         .equals(eventWrapper.updateEvent()
                                             .id()))
        {
            this.logger.log("<MEMBER,`%s`> update SELF".formatted(caller.memberId()), INFO);
        } else if (caller.memberId()
                         .equals(eventWrapper.ddbFamilyId()) || caller.familyId()
                                                                      .equals(eventWrapper.updateEvent()
                                                                                          .id()))
        {
            this.logger.log("<MEMBER,`%s`> update <SPOUSE,`%s`>".formatted(caller.memberId(), eventWrapper.updateEvent()
                                                                                                          .id()), INFO);
        } else if (!eventWrapper.ddbMemberIsSuperAdult() &&
                   ofNullable(requireNonNull(this.getDdbItem(caller.familyId(), DdbTable.FAMILY)).get(FamilyTableParameter.DESCENDANTS.jsonFieldName())).map(AttributeValue::ss)
                                                                                                                                                        .filter(Predicate.not(List::isEmpty))
                                                                                                                                                        .filter(ss -> ss.contains(
                                                                                                                                                                eventWrapper.updateEvent()
                                                                                                                                                                            .id()))
                                                                                                                                                        .isPresent())
        {
            this.logger.log("<MEMBER,`%s`> update <DESCENDANT,`%s`>".formatted(caller.memberId(), eventWrapper.updateEvent()
                                                                                                              .id()), INFO);
        } else {
            this.logger.log("<MEMBER,`%s`> attempted to update <MEMBER,`%s`>".formatted(caller.memberId(), eventWrapper.updateEvent()
                                                                                                                       .id()), WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_FORBIDDEN));
        }

        final Map<String, AttributeValue> item = Member.retrieveDdbMap(new MemberRecord(UUID.fromString(eventWrapper.updateEvent()
                                                                                                                    .id()), eventWrapper.updateEvent()
                                                                                                                                        .member(), UUID.fromString(eventWrapper.ddbFamilyId())));

        this.logger.log(Member.convertDdbMap(item)
                              .toString(), DEBUG);

        return PutItemRequest.builder()
                             .tableName(DdbTable.MEMBER.name())
                             .item(item)
                             .build();
    }

    public
    record EventWrapper(@NotNull UpdateEvent updateEvent, @NotNull String ddbFamilyId, boolean ddbMemberIsSuperAdult) {
    }
}
