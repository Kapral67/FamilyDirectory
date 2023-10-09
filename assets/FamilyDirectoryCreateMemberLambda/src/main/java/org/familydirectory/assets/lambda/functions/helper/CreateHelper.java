package org.familydirectory.assets.lambda.functions.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.member.MemberParams;
import org.familydirectory.assets.lambda.models.CreateEvent;
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
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.familydirectory.assets.ddb.enums.DdbTable.FAMILIES;
import static org.familydirectory.assets.ddb.enums.DdbTable.MEMBERS;
import static org.familydirectory.assets.ddb.enums.DdbTable.PK;
import static org.familydirectory.assets.ddb.enums.family.FamilyParams.ANCESTOR;
import static org.familydirectory.assets.ddb.enums.family.FamilyParams.DESCENDANTS;
import static org.familydirectory.assets.ddb.enums.family.FamilyParams.SPOUSE;

public final
class CreateHelper extends ApiHelper {
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();

    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();

    private final @NotNull LambdaLogger logger;

    private final @NotNull APIGatewayProxyRequestEvent requestEvent;
    private final @NotNull UUID inputMemberId = UUID.randomUUID();

    public
    CreateHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) {
        this.logger = requireNonNull(logger);
        this.requestEvent = requireNonNull(requestEvent);
    }

    public @NotNull
    CreateEvent getCreateEvent (final @NotNull Caller caller) {
        final CreateEvent createEvent;
        try {
            createEvent = this.objectMapper.convertValue(this.requestEvent.getBody(), CreateEvent.class);
        } catch (final IllegalArgumentException e) {
            this.logger.log("<MEMBER,`%s`> submitted invalid Create request".formatted(caller.memberId()), WARN);
            this.logTrace(e, WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_BAD_REQUEST));
        }

        this.validateMemberIsUnique(createEvent.getMember()
                                               .getKey(), caller.memberId(), createEvent.getMember()
                                                                                        .getEmail());

        return createEvent;
    }

    private
    void validateMemberIsUnique (final @NotNull String memberKey, final @NotNull String callerMemberId, final @Nullable String memberEmail) {
        final QueryRequest keyRequest = QueryRequest.builder()
                                                    .tableName(DdbTable.MEMBERS.name())
                                                    .indexName(requireNonNull(MemberParams.KEY.gsiProps()).getIndexName())
                                                    .keyConditionExpression("%s = :key".formatted(MemberParams.KEY.gsiProps()
                                                                                                                  .getPartitionKey()
                                                                                                                  .getName()))
                                                    .expressionAttributeValues(singletonMap(":key", AttributeValue.fromS(memberKey)))
                                                    .limit(1)
                                                    .build();
        final QueryResponse keyResponse = this.dynamoDbClient.query(keyRequest);
        if (keyResponse.hasItems()) {
            final String keyMemberId = keyResponse.items()
                                                  .get(0)
                                                  .get(MemberParams.ID.jsonFieldName())
                                                  .s();
            this.logger.log("<MEMBER,`%s`> Requested Create for Existing <MEMBER,`%s`> Using <KEY,`%s`>".formatted(callerMemberId, keyMemberId, memberKey), WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_CONFLICT));
        } else if (nonNull(memberEmail) && !memberEmail.isBlank()) {
            final QueryRequest emailRequest = QueryRequest.builder()
                                                          .tableName(DdbTable.MEMBERS.name())
                                                          .indexName(requireNonNull(MemberParams.EMAIL.gsiProps()).getIndexName())
                                                          .keyConditionExpression("%s = :email".formatted(MemberParams.EMAIL.gsiProps()
                                                                                                                            .getPartitionKey()
                                                                                                                            .getName()))
                                                          .expressionAttributeValues(singletonMap(":email", AttributeValue.fromS(memberEmail)))
                                                          .limit(1)
                                                          .build();
            final QueryResponse emailResponse = this.dynamoDbClient.query(emailRequest);
            if (emailResponse.hasItems()) {
                final String emailResponseMemberId = emailResponse.items()
                                                                  .get(0)
                                                                  .get(MemberParams.ID.jsonFieldName())
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
    APIGatewayProxyRequestEvent getRequestEvent () {
        return this.requestEvent;
    }

    @Override
    public @NotNull
    DynamoDbClient getDynamoDbClient () {
        return this.dynamoDbClient;
    }

    public @NotNull
    TransactWriteItemsRequest buildCreateTransaction (final @NotNull Caller caller, final @NotNull CreateEvent createEvent) {
        final List<TransactWriteItem> transactionItems = new ArrayList<>();
        final Map<String, AttributeValue> callerFamily = ofNullable(this.getDdbItem(caller.familyId(), FAMILIES)).orElseThrow();
        final String inputFamilyId;
        if (caller.memberId()
                  .equals(caller.familyId()) && createEvent.getIsSpouse())
        {
            if (ofNullable(callerFamily.get(SPOUSE.jsonFieldName())).map(AttributeValue::s)
                                                                    .filter(Predicate.not(String::isBlank))
                                                                    .isEmpty())
            {
                this.logger.log("<MEMBER,`%s`> Creating Spouse".formatted(caller.memberId()), INFO);
                inputFamilyId = caller.familyId();
                transactionItems.add(TransactWriteItem.builder()
                                                      .update(Update.builder()
                                                                    .tableName(FAMILIES.name())
                                                                    .key(singletonMap(PK.getName(), AttributeValue.fromS(caller.familyId())))
                                                                    .updateExpression("SET %s = :spouseKey".formatted(SPOUSE.jsonFieldName()))
                                                                    .expressionAttributeValues(singletonMap(":spouseKey", AttributeValue.fromS(this.inputMemberId.toString())))
                                                                    .build())
                                                      .build());
            } else {
                this.logger.log("<MEMBER,`%s`> Spouse already exists".formatted(caller.memberId()), WARN);
                throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_CONFLICT));
            }
        } else if (!createEvent.getIsSpouse()) {
            this.logger.log("<MEMBER,`%s`> Creating Descendant".formatted(caller.memberId()), INFO);
            inputFamilyId = this.inputMemberId.toString();
            final String descendantsUpdateExpression = (ofNullable(callerFamily.get(DESCENDANTS.jsonFieldName())).filter(Predicate.not(AttributeValue::hasSs))
                                                                                                                 .map(AttributeValue::ss)
                                                                                                                 .filter(Predicate.not(List::isEmpty))
                                                                                                                 .isEmpty())
                    ? "SET %s = :descendants".formatted(DESCENDANTS.jsonFieldName())
                    : "SET %s = list_append(%s, :descendants)".formatted(DESCENDANTS.jsonFieldName(), DESCENDANTS.jsonFieldName());
            transactionItems.add(TransactWriteItem.builder()
                                                  .update(Update.builder()
                                                                .tableName(FAMILIES.name())
                                                                .key(singletonMap(PK.getName(), AttributeValue.fromS(caller.familyId())))
                                                                .updateExpression(descendantsUpdateExpression)
                                                                .expressionAttributeValues(singletonMap(":descendants", AttributeValue.fromSs(singletonList(inputFamilyId))))
                                                                .build())
                                                  .build());
            transactionItems.add(TransactWriteItem.builder()
                                                  .put(Put.builder()
                                                          .tableName(FAMILIES.name())
                                                          .item(Map.of(PK.getName(), AttributeValue.fromS(inputFamilyId), ANCESTOR.jsonFieldName(), AttributeValue.fromS(caller.familyId())))
                                                          .build())
                                                  .build());
        } else {
            this.logger.log("Naturalized <MEMBER,`%s`> Attempted to Create New Spouse".formatted(caller.memberId()), WARN);
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_UNAUTHORIZED));
        }
        transactionItems.add(TransactWriteItem.builder()
                                              .put(Put.builder()
                                                      .tableName(MEMBERS.name())
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

        for (final MemberParams field : MemberParams.values()) {
            switch (field) {
                case ID -> member.put(PK.getName(), AttributeValue.fromS(this.inputMemberId.toString()));
                case KEY -> member.put(field.jsonFieldName(), AttributeValue.fromS(createEvent.getMember()
                                                                                              .getKey()));
                case FIRST_NAME -> member.put(field.jsonFieldName(), AttributeValue.fromS(createEvent.getMember()
                                                                                                     .getFirstName()));
                case MIDDLE_NAME -> ofNullable(createEvent.getMember()
                                                          .getMiddleName()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s)));
                case LAST_NAME -> member.put(field.jsonFieldName(), AttributeValue.fromS(createEvent.getMember()
                                                                                                    .getLastName()));
                case SUFFIX -> ofNullable(createEvent.getMember()
                                                     .getSuffix()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s.value())));
                case BIRTHDAY -> member.put(field.jsonFieldName(), AttributeValue.fromS(createEvent.getMember()
                                                                                                   .getBirthdayString()));
                case DEATHDAY -> ofNullable(createEvent.getMember()
                                                       .getDeathdayString()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s)));
                case EMAIL -> ofNullable(createEvent.getMember()
                                                    .getEmail()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.fromS(s)));
                case PHONES -> ofNullable(createEvent.getMember()
                                                     .getPhonesDdbMap()).ifPresent(m -> member.put(field.jsonFieldName(), AttributeValue.fromM(m)));
                case ADDRESS -> ofNullable(createEvent.getMember()
                                                      .getAddress()).ifPresent(ss -> member.put(field.jsonFieldName(), AttributeValue.fromSs(ss)));
                case FAMILY_ID -> member.put(field.jsonFieldName(), AttributeValue.fromS(inputFamilyId));
                default -> throw new IllegalStateException("Unhandled Member Parameter: `%s`".formatted(field.jsonFieldName()));
            }
        }

        return member;
    }
}
