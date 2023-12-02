package org.familydirectory.assets.lambda.function.api.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.lambda.function.api.models.CreateEvent;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.Update;
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

    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();

    private final @NotNull LambdaLogger logger;

    private final @NotNull APIGatewayProxyRequestEvent requestEvent;
    private final @NotNull UUID inputMemberId = UUID.randomUUID();

    public
    CreateHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) {
        super();
        this.logger = requireNonNull(logger);
        this.requestEvent = requireNonNull(requestEvent);
    }

    public @NotNull
    CreateEvent getCreateEvent (final @NotNull ApiHelper.Caller caller) {
        final CreateEvent createEvent;
        try {
            createEvent = this.objectMapper.readValue(this.requestEvent.getBody(), CreateEvent.class);
        } catch (final JsonProcessingException e) {
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
                                                                  .iterator()
                                                                  .next()
                                                                  .get(MemberTableParameter.ID.jsonFieldName())
                                                                  .s();
                this.logger.log("<MEMBER,`%s`> Requested Create, but Existing <MEMBER,`%s`> Already Claims <EMAIL,`%s`>".formatted(callerMemberId, emailResponseMemberId, memberEmail), WARN);
                throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_CONFLICT)
                                                                              .withBody("Email Already Registered With Another Member"));
            }
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
    public @NotNull
    APIGatewayProxyRequestEvent getRequestEvent () {
        return this.requestEvent;
    }

    public @NotNull
    TransactWriteItemsRequest buildCreateTransaction (final @NotNull Caller caller, final @NotNull CreateEvent createEvent) {
        final List<TransactWriteItem> transactionItems = new ArrayList<>();
        final Map<String, AttributeValue> callerFamily = ofNullable(this.getDdbItem(caller.familyId(), DdbTable.FAMILY)).orElseThrow();
        final String inputFamilyId;
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
        transactionItems.add(TransactWriteItem.builder()
                                              .put(Put.builder()
                                                      .tableName(DdbTable.MEMBER.name())
                                                      .item(this.buildMember(createEvent, inputFamilyId))
                                                      .build())
                                              .build());
        return TransactWriteItemsRequest.builder()
                                        .transactItems(transactionItems)
                                        .build();
    }

    private @NotNull
    Map<String, AttributeValue> buildMember (final @NotNull CreateEvent createEvent, final @NotNull String inputFamilyId) {
        final Map<String, AttributeValue> member = new HashMap<>();

        for (final MemberTableParameter field : MemberTableParameter.values()) {
            switch (field) {
                case ID -> member.put(field.jsonFieldName(), AttributeValue.fromS(this.inputMemberId.toString()));
                case FIRST_NAME -> member.put(field.jsonFieldName(), AttributeValue.fromS(createEvent.member()
                                                                                                     .getFirstName()));
                case MIDDLE_NAME -> ofNullable(createEvent.member()
                                                          .getMiddleName()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s)));
                case LAST_NAME -> member.put(field.jsonFieldName(), AttributeValue.fromS(createEvent.member()
                                                                                                    .getLastName()));
                case SUFFIX -> ofNullable(createEvent.member()
                                                     .getSuffix()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s.value())));
                case BIRTHDAY -> member.put(field.jsonFieldName(), AttributeValue.fromS(createEvent.member()
                                                                                                   .getBirthdayString()));
                case DEATHDAY -> ofNullable(createEvent.member()
                                                       .getDeathdayString()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s)));
                case EMAIL -> ofNullable(createEvent.member()
                                                    .getEmail()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s)));
                case PHONES -> ofNullable(createEvent.member()
                                                     .getPhonesDdbMap()).ifPresent(m -> member.put(field.jsonFieldName(), AttributeValue.fromM(m)));
                case ADDRESS -> ofNullable(createEvent.member()
                                                      .getAddress()).ifPresent(ss -> member.put(field.jsonFieldName(), AttributeValue.fromSs(ss)));
                case FAMILY_ID -> member.put(field.jsonFieldName(), AttributeValue.fromS(inputFamilyId));
                default -> throw new IllegalStateException("Unhandled Member Parameter: `%s`".formatted(field.jsonFieldName()));
            }
        }

        return member;
    }
}
