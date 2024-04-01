package org.familydirectory.assets.lambda.function.api.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.lambda.function.api.models.CreateEvent;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.Update;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.DEBUG;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.WARN;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;

public final
class CreateHelper extends ApiHelper {
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull UUID inputMemberId = UUID.randomUUID();

    public
    CreateHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) {
        super(logger, requestEvent);
    }

    public @NotNull
    CreateEvent getCreateEvent (final @NotNull ApiHelper.Caller caller) {
        final CreateEvent createEvent;
        try {
            createEvent = this.objectMapper.readValue(this.requestEvent.getBody(), CreateEvent.class);
            this.logger.log(createEvent.member()
                                       .toString(), DEBUG);
        } catch (final JsonProcessingException | IllegalArgumentException e) {
            this.logger.log("<MEMBER,`%s`> submitted invalid Create request".formatted(caller.memberId()), WARN);
            LambdaUtils.logTrace(this.logger, e, WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_BAD_REQUEST));
        }

        this.validateMemberEmailIsUnique(caller.memberId(), createEvent.member()
                                                                       .getEmail());

        return createEvent;
    }

    private
    void validateMemberEmailIsUnique (final @NotNull String callerMemberId, final @Nullable String memberEmail) {
        if (nonNull(memberEmail) && !memberEmail.isBlank()) {
            final QueryRequest emailRequest = QueryRequest.builder()
                                                          .tableName(DdbTable.MEMBER.name())
                                                          .indexName(requireNonNull(MemberTableParameter.EMAIL.gsiProps()).getIndexName())
                                                          .keyConditionExpression("%s = :email".formatted(MemberTableParameter.EMAIL.gsiProps()
                                                                                                                                    .getPartitionKey()
                                                                                                                                    .getName()))
                                                          .expressionAttributeValues(singletonMap(":email", AttributeValue.fromS(memberEmail)))
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
                this.logger.log("<MEMBER,`%s`> Requested Create, but Existing <MEMBER,`%s`> Already Claims <EMAIL,`%s`>".formatted(callerMemberId, emailResponseMemberId, memberEmail), WARN);
                throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_CONFLICT)
                                                                              .withBody("Email Already Registered With Another Member"));
            }
        }
    }

    public @NotNull
    TransactWriteItemsRequest buildCreateTransaction (final @NotNull Caller caller, final @NotNull CreateEvent createEvent) {
        final List<TransactWriteItem> transactionItems = new ArrayList<>();
        final String inputFamilyId;
        if (caller.isAdmin() && nonNull(createEvent.ancestor())) {
            final Map<String, AttributeValue> ancestorFamily = requireNonNull(this.getDdbItem(createEvent.ancestor(), DdbTable.FAMILY));
            if (createEvent.isSpouse()) {
                this.logger.log("ADMIN <MEMBER,`%s`> Creating Spouse for <MEMBER,`%s`>".formatted(caller.memberId(), createEvent.ancestor()), INFO);
                if (ofNullable(ancestorFamily.get(FamilyTableParameter.SPOUSE.jsonFieldName())).map(AttributeValue::s)
                                                                                               .filter(Predicate.not(String::isBlank))
                                                                                               .isEmpty())
                {
                    inputFamilyId = ofNullable(ancestorFamily.get(FamilyTableParameter.ID.jsonFieldName())).map(AttributeValue::s)
                                                                                                           .filter(Predicate.not(String::isBlank))
                                                                                                           .orElseThrow();
                    transactionItems.add(TransactWriteItem.builder()
                                                          .update(Update.builder()
                                                                        .tableName(DdbTable.FAMILY.name())
                                                                        .key(singletonMap(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(inputFamilyId)))
                                                                        .updateExpression("SET %s = :spouseKey".formatted(FamilyTableParameter.SPOUSE.jsonFieldName()))
                                                                        .expressionAttributeValues(singletonMap(":spouseKey", AttributeValue.fromS(this.inputMemberId.toString())))
                                                                        .build())
                                                          .build());
                } else {
                    this.logger.log("<MEMBER,`%s` Spouse already exists".formatted(createEvent.ancestor()), WARN);
                    throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_CONFLICT));
                }
            } else {
                this.logger.log("ADMIN <MEMBER,`%s`> Creating Descendant for <MEMBER,`%s`>".formatted(caller.memberId(), createEvent.ancestor()), INFO);
                inputFamilyId = this.inputMemberId.toString();
                final String descendantsUpdateExpression = (ofNullable(ancestorFamily.get(FamilyTableParameter.DESCENDANTS.jsonFieldName())).map(AttributeValue::ss)
                                                                                                                                            .filter(Predicate.not(List::isEmpty))
                                                                                                                                            .isEmpty())
                        ? "SET %s = :descendants".formatted(FamilyTableParameter.DESCENDANTS.jsonFieldName())
                        : "ADD %s :descendants".formatted(FamilyTableParameter.DESCENDANTS.jsonFieldName());
                final String ancestorFamilyId = ofNullable(ancestorFamily.get(FamilyTableParameter.ID.jsonFieldName())).map(AttributeValue::s)
                                                                                                                       .filter(Predicate.not(String::isBlank))
                                                                                                                       .orElseThrow();
                transactionItems.add(TransactWriteItem.builder()
                                                      .update(Update.builder()
                                                                    .tableName(DdbTable.FAMILY.name())
                                                                    .key(singletonMap(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(ancestorFamilyId)))
                                                                    .updateExpression(descendantsUpdateExpression)
                                                                    .expressionAttributeValues(singletonMap(":descendants", AttributeValue.fromSs(singletonList(inputFamilyId))))
                                                                    .build())
                                                      .build());
                transactionItems.add(TransactWriteItem.builder()
                                                      .put(Put.builder()
                                                              .tableName(DdbTable.FAMILY.name())
                                                              .item(Map.of(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(inputFamilyId), FamilyTableParameter.ANCESTOR.jsonFieldName(),
                                                                           AttributeValue.fromS(ancestorFamilyId)))
                                                              .build())
                                                      .build());
            }
        } else {
            final Map<String, AttributeValue> callerFamily = requireNonNull(this.getDdbItem(caller.familyId(), DdbTable.FAMILY));
            if (caller.memberId()
                      .equals(caller.familyId()) && createEvent.isSpouse())
            {
                if (ofNullable(callerFamily.get(FamilyTableParameter.SPOUSE.jsonFieldName())).map(AttributeValue::s)
                                                                                             .filter(Predicate.not(String::isBlank))
                                                                                             .isEmpty())
                {
                    this.logger.log("<MEMBER,`%s`> Creating Spouse".formatted(caller.memberId()), INFO);
                    inputFamilyId = caller.familyId();
                    transactionItems.add(TransactWriteItem.builder()
                                                          .update(Update.builder()
                                                                        .tableName(DdbTable.FAMILY.name())
                                                                        .key(singletonMap(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(caller.familyId())))
                                                                        .updateExpression("SET %s = :spouseKey".formatted(FamilyTableParameter.SPOUSE.jsonFieldName()))
                                                                        .expressionAttributeValues(singletonMap(":spouseKey", AttributeValue.fromS(this.inputMemberId.toString())))
                                                                        .build())
                                                          .build());
                } else {
                    this.logger.log("<MEMBER,`%s`> Spouse already exists".formatted(caller.memberId()), WARN);
                    throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_CONFLICT));
                }
            } else if (!createEvent.isSpouse()) {
                this.logger.log("<MEMBER,`%s`> Creating Descendant".formatted(caller.memberId()), INFO);
                inputFamilyId = this.inputMemberId.toString();
                final String descendantsUpdateExpression = (ofNullable(callerFamily.get(FamilyTableParameter.DESCENDANTS.jsonFieldName())).map(AttributeValue::ss)
                                                                                                                                          .filter(Predicate.not(List::isEmpty))
                                                                                                                                          .isEmpty())
                        ? "SET %s = :descendants".formatted(FamilyTableParameter.DESCENDANTS.jsonFieldName())
                        : "ADD %s :descendants".formatted(FamilyTableParameter.DESCENDANTS.jsonFieldName());
                transactionItems.add(TransactWriteItem.builder()
                                                      .update(Update.builder()
                                                                    .tableName(DdbTable.FAMILY.name())
                                                                    .key(singletonMap(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(caller.familyId())))
                                                                    .updateExpression(descendantsUpdateExpression)
                                                                    .expressionAttributeValues(singletonMap(":descendants", AttributeValue.fromSs(singletonList(inputFamilyId))))
                                                                    .build())
                                                      .build());
                transactionItems.add(TransactWriteItem.builder()
                                                      .put(Put.builder()
                                                              .tableName(DdbTable.FAMILY.name())
                                                              .item(Map.of(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(inputFamilyId), FamilyTableParameter.ANCESTOR.jsonFieldName(),
                                                                           AttributeValue.fromS(caller.familyId())))
                                                              .build())
                                                      .build());
            } else {
                this.logger.log("<MEMBER,`%s`> Denied Request to Create New Member".formatted(caller.memberId()), WARN);
                throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_FORBIDDEN));
            }
        }

        final Map<String, AttributeValue> item = Member.retrieveDdbMap(new MemberRecord(this.inputMemberId, createEvent.member(), UUID.fromString(inputFamilyId)));

        this.logger.log(Member.convertDdbMap(item)
                              .toString(), DEBUG);

        transactionItems.add(TransactWriteItem.builder()
                                              .put(Put.builder()
                                                      .tableName(DdbTable.MEMBER.name())
                                                      .item(item)
                                                      .build())
                                              .build());
        return TransactWriteItemsRequest.builder()
                                        .transactItems(transactionItems)
                                        .build();
    }
}
