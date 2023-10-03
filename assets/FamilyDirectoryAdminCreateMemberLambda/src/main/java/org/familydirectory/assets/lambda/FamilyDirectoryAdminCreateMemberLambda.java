package org.familydirectory.assets.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.member.MemberParams;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.member.MemberReference;
import org.familydirectory.assets.lambda.exceptions.ResponseThrowable;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.Update;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.DEBUG;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.ERROR;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.TRACE;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.WARN;
import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static java.util.Map.of;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.familydirectory.assets.ddb.enums.DdbTable.FAMILIES;
import static org.familydirectory.assets.ddb.enums.DdbTable.MEMBERS;
import static org.familydirectory.assets.ddb.enums.DdbTable.PK;
import static org.familydirectory.assets.ddb.enums.family.FamilyParams.ANCESTOR;
import static org.familydirectory.assets.ddb.enums.family.FamilyParams.DESCENDANTS;
import static org.familydirectory.assets.ddb.enums.family.FamilyParams.SPOUSE;
import static software.amazon.awssdk.http.HttpStatusCode.BAD_REQUEST;
import static software.amazon.awssdk.http.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static software.amazon.awssdk.http.HttpStatusCode.OK;
import static software.amazon.awssdk.http.HttpStatusCode.UNAUTHORIZED;
import static software.amazon.awssdk.services.dynamodb.DynamoDbClient.create;

public
class FamilyDirectoryAdminCreateMemberLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final DynamoDbClient client = create();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String KEY = "#k";
    private static final String VALUE = ":v";
    @NotNull
    private static final GlobalSecondaryIndexProps MEMBERS_EMAIL_GSI;

    static {
        assert MEMBERS.globalSecondaryIndexProps() != null;
        MEMBERS_EMAIL_GSI = MEMBERS.globalSecondaryIndexProps();
    }

    private LambdaLogger logger = null;
    private Map<String, AttributeValue> inputAncestor = null;
    private final Map<String, AttributeValue> inputAncestorsSpouse = null;

    @Override
    public
    APIGatewayProxyResponseEvent handleRequest (APIGatewayProxyRequestEvent event, Context context)
    {
        this.logger = context.getLogger();
        try {
//      Get Caller List
            final List<String> callerPKs;
            final String callerEmail;
            try {
                @SuppressWarnings("unchecked")
                final Map<String, Object> callerClaims = (Map<String, Object>) requireNonNull(event.getRequestContext()
                                                                                                   .getAuthorizer()
                                                                                                   .get("claims"));
                callerEmail = Optional.of(callerClaims)
                                      .map(map -> map.get("email"))
                                      .map(Object::toString)
                                      .filter(Predicate.not(String::isBlank))
                                      .orElseThrow(NullPointerException::new);

                this.logger.log(format("Caller <EMAIL,`%s`>", callerEmail), INFO);

                // TODO: Get the item from the table that is the caller's primary key
                final QueryRequest callerQuery = QueryRequest.builder()
                                                             .tableName(MEMBERS.name())
                                                             .indexName(MEMBERS_EMAIL_GSI.getIndexName())
                                                             .keyConditionExpression(format("%s = %s", KEY, VALUE))
                                                             .expressionAttributeNames(of(KEY, MEMBERS_EMAIL_GSI.getPartitionKey()
                                                                                                                .getName()))
                                                             .expressionAttributeValues(of(VALUE, AttributeValue.builder()
                                                                                                                .s(callerEmail)
                                                                                                                .build()))
                                                             .build();
                final QueryResponse callerQueryResponse = client.query(callerQuery);
                callerPKs = Optional.of(callerQueryResponse.items())
                                    .map(obj -> obj.stream()
                                                   .map(m -> m.get(PK.getName())
                                                              .s())
                                                   .filter(String::isBlank)
                                                   .toList())
                                    .orElseThrow(NullPointerException::new);

                this.logger.log(format("Caller <EMAIL,`%s`> authenticated as one of PK: `%s`", callerEmail, callerPKs), INFO);

            } catch (final NullPointerException | ClassCastException e) {
                this.logger.log(e.getMessage(), ERROR);
                this.logger.log(e.getCause()
                                 .toString(), DEBUG);
                this.logger.log(Arrays.toString(e.getStackTrace()), TRACE);
                return new APIGatewayProxyResponseEvent().withStatusCode(UNAUTHORIZED)
                                                         .withBody("Caller Email Not Found");
            }

//      Deserialization
            final Member input;
            try {
                input = mapper.convertValue(event.getBody(), Member.class);
            } catch (final IllegalArgumentException e) {
                this.logger.log(format("Caller <EMAIL, `%s`> submitted invalid request", callerEmail), WARN);
                this.logger.log(e.getMessage(), WARN);
                this.logger.log(e.getCause()
                                 .toString(), DEBUG);
                this.logger.log(Arrays.toString(e.getStackTrace()), TRACE);
                return new APIGatewayProxyResponseEvent().withStatusCode(BAD_REQUEST)
                                                         .withBody("Invalid Member");
            }

            final String callerPK = this.checkCallerAuthorization(callerPKs, input, callerEmail);

            // Get PreExisting Member Item if Existing
            final Map<String, AttributeValue> preExistMemberEntryItem = getDdbItem(input.getPrimaryKey());
//            TODO: FUNCTION CALL

//      Build Transaction List
            final List<TransactWriteItem> transactWriteItems = new ArrayList<>();
            if (nonNull(input.getAncestor())) {
                if (nonNull(input.getIsAncestorSpouse()) && input.getIsAncestorSpouse()) {
//              Set spouse field in existing entry in FAMILIES Table
                    transactWriteItems.add(TransactWriteItem.builder()
                                                            .update(Update.builder()
                                                                          .tableName(FAMILIES.name())
                                                                          .key(of(PK.getName(), AttributeValue.builder()
                                                                                                              .s(input.getAncestor()
                                                                                                                      .getPrimaryKey())
                                                                                                              .build()))
                                                                          .updateExpression(format("SET %s = %s", KEY, VALUE))
                                                                          .expressionAttributeNames(of(KEY, SPOUSE.jsonFieldName()))
                                                                          .expressionAttributeValues(of(VALUE, AttributeValue.builder()
                                                                                                                             .s(input.getPrimaryKey())
                                                                                                                             .build()))
                                                                          .build())
                                                            .build());
                } else {
//              Append to dependents list in existing entry in FAMILIES Table if not already present
                    final GetItemRequest inputAncestorFamilyEntryItemRequest = GetItemRequest.builder()
                                                                                             .tableName(FAMILIES.name())
                                                                                             .key(singletonMap(PK.getName(), AttributeValue.builder()
                                                                                                                                           .s(input.getAncestor()
                                                                                                                                                   .getPrimaryKey())
                                                                                                                                           .build()))
                                                                                             .build();
                    final GetItemResponse inputAncestorFamilyEntryItemResponse = client.getItem(inputAncestorFamilyEntryItemRequest);
                    if (inputAncestorFamilyEntryItemResponse.hasItem()) {
                        final AttributeValue inputAncestorFamilyEntryDescendantsAttributeValue = inputAncestorFamilyEntryItemResponse.item()
                                                                                                                                     .get(DESCENDANTS.jsonFieldName());
                        if (inputAncestorFamilyEntryDescendantsAttributeValue.hasSs() && !inputAncestorFamilyEntryDescendantsAttributeValue.ss()
                                                                                                                                           .contains(input.getPrimaryKey()))
                        {
                            transactWriteItems.add(TransactWriteItem.builder()
                                                                    .update(Update.builder()
                                                                                  .tableName(FAMILIES.name())
                                                                                  .key(of(PK.getName(), AttributeValue.builder()
                                                                                                                      .s(input.getAncestor()
                                                                                                                              .getPrimaryKey())
                                                                                                                      .build()))
                                                                                  .updateExpression(format("SET %s = list_append(%s, %s)", KEY, KEY, VALUE))
                                                                                  .expressionAttributeNames(of(KEY, DESCENDANTS.jsonFieldName()))
                                                                                  .expressionAttributeValues(of(VALUE, AttributeValue.builder()
                                                                                                                                     .s(input.getPrimaryKey())
                                                                                                                                     .build()))
                                                                                  .build())
                                                                    .build());
                        }
                    }
//                  Create new entry in FAMILIES Table
                    transactWriteItems.add(TransactWriteItem.builder()
                                                            .put(Put.builder()
                                                                    .tableName(FAMILIES.name())
                                                                    .item(of(PK.getName(), AttributeValue.builder()
                                                                                                         .s(input.getPrimaryKey())
                                                                                                         .build(), ANCESTOR.jsonFieldName(), AttributeValue.builder()
                                                                                                                                                           .s(input.getAncestor()
                                                                                                                                                                   .getPrimaryKey())
                                                                                                                                                           .build()))
                                                                    .build())
                                                            .build());
                }
            } else {
                // TODO: Extra Authorization Needed For Creating Members with No Ancestry
                // FIXME: Rudimentary, Broad, Solution
                return new APIGatewayProxyResponseEvent().withStatusCode(UNAUTHORIZED);
            }

//      Create new entry in MEMBERS Table
            transactWriteItems.add(TransactWriteItem.builder()
                                                    .put(Put.builder()
                                                            .tableName(MEMBERS.name())
                                                            .item(member)
                                                            .build())
                                                    .build());

//      Execute Transaction
            client.transactWriteItems(TransactWriteItemsRequest.builder()
                                                               .transactItems(transactWriteItems)
                                                               .build());

            return new APIGatewayProxyResponseEvent().withStatusCode(OK);
        } catch (final ResponseThrowable e) {
            return e.getResponseEvent();
        } catch (final Exception e) {
            this.logger.log(e.getMessage(), FATAL);
            Optional.ofNullable(e.getCause())
                    .ifPresent(throwable -> this.logger.log(throwable.toString(), DEBUG));
            this.logger.log(Arrays.toString(e.getStackTrace()), TRACE);
            return new APIGatewayProxyResponseEvent().withStatusCode(INTERNAL_SERVER_ERROR);
        }
    }

    private
    String checkCallerAuthorization (final List<String> callerPKs, final Member input, final String callerEmail) throws ResponseThrowable {
        for (final String pk : callerPKs) {
            // THEMSELF
            if (pk.equals(input.getPrimaryKey())) {
                this.logger.log(format("Caller <PK,`%s`> <EMAIL,`%s`> Editing Self", pk, callerEmail), INFO);
                return pk;
            }
            // SPOUSE
            if (nonNull(input.getSpouse()) && pk.equals(input.getSpouse()
                                                             .getPrimaryKey()))
            {
                this.logger.log(format("Caller <PK,`%s`> Upserting Spouse <PK,`%s`>", pk, input.getPrimaryKey()), INFO);
                return pk;
            }
            // DESCENDANTS
            if (nonNull(input.getAncestor())) {
                if (pk.equals(input.getAncestor()
                                   .getPrimaryKey()))
                {
                    this.logger.log(format("Caller <PK,`%s`> Upserting Descendant <PK,`%s`>", pk, input.getPrimaryKey()), INFO);
                    return pk;
                } else {
                    this.inputAncestor = getDdbItem(input.getAncestor()
                                                         .getPrimaryKey());
                    this.inputAncestorsSpouse = ofNullable(this.inputAncestor).flatMap(a -> {
                        return ofNullable()
                    }).orElse(null);
                }
            }
        }
        this.logger.log(format("Caller <PKs,`%s`> cannot upsert Member <PK,`%s`>", callerPKs, input.getPrimaryKey()), WARN);
        throw new ResponseThrowable(new APIGatewayProxyResponseEvent().withStatusCode(UNAUTHORIZED)
                                                                      .withBody("Caller Not Authorized To Create Requested Member"));
    }

    private static
    Map<String, AttributeValue> getDdbItem (final String primaryKey) {
        final GetItemRequest request = GetItemRequest.builder()
                                                     .tableName(DdbTable.MEMBERS.name())
                                                     .key(singletonMap(PK.getName(), AttributeValue.builder()
                                                                                                   .s(primaryKey)
                                                                                                   .build()))
                                                     .build();
        final GetItemResponse response = client.getItem(request);
        return (response.hasItem())
                ? response.item()
                : null;
    }

    private
    Map<String, AttributeValue> buildMember (final Member input, final String callerPK, final Map<String, AttributeValue> preExistMemberEntryItem) {

//      Build New Item
        final Map<String, AttributeValue> member = new HashMap<>();
        member.put(PK.getName(), AttributeValue.builder()
                                               .s(input.getPrimaryKey())
                                               .build());

        for (final MemberParams field : MemberParams.values()) {
            final Optional<Object> preExistMemberField = ofNullable(preExistMemberEntryItem).flatMap(m -> ofNullable(m.get(field.jsonFieldName())))
                                                                                            .flatMap(f -> {
                                                                                                switch (field.ddbType()) {
                                                                                                    case STR -> {
                                                                                                        return ofNullable(f.s());
                                                                                                    }
                                                                                                    case NUM -> {
                                                                                                        return ofNullable(f.n());
                                                                                                    }
                                                                                                    case BIN -> {
                                                                                                        return ofNullable(f.b());
                                                                                                    }
                                                                                                    case STR_SET -> {
                                                                                                        return ofNullable(f.ss());
                                                                                                    }
                                                                                                    case NUM_SET -> {
                                                                                                        return ofNullable(f.ns());
                                                                                                    }
                                                                                                    case BIN_SET -> {
                                                                                                        return ofNullable(f.bs());
                                                                                                    }
                                                                                                    case MAP -> {
                                                                                                        return ofNullable(f.m());
                                                                                                    }
                                                                                                    case LIST -> {
                                                                                                        return ofNullable(f.l());
                                                                                                    }
                                                                                                    case BOOL -> {
                                                                                                        return ofNullable(f.bool());
                                                                                                    }
                                                                                                    default -> {
                                                                                                        return Optional.empty();
                                                                                                    }
                                                                                                }
                                                                                            });
            switch (field) {
                case FIRST_NAME -> member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                                   .s(input.getFirstName())
                                                                                   .build());
                case LAST_NAME -> member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                                  .s(input.getLastName())
                                                                                  .build());
                case SUFFIX -> ofNullable(input.getSuffix()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                                                                            .s(s.value())
                                                                                                                            .build()));
                case BIRTHDAY -> member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                                 .s(input.getBirthdayString())
                                                                                 .build());
                case DEATHDAY -> {
                    ofNullable(input.getDeathdayString()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                                                                         .s(s)
                                                                                                                         .build()));

                    preExistMemberField.ifPresent(s -> {
                        if (!s.equals(input.getDeathdayString())) {
                            this.logger.log(format("Caller <PK,`%s`> Altering Member <PK,`%s`> DEATHDAY from `%s` to `%s`", callerPK, input.getPrimaryKey(), s, input.getDeathdayString()), WARN);
                        }
                    });
                }
                case EMAIL -> {
                    ofNullable(input.getEmail()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                                                                .s(s)
                                                                                                                .build()));
                    preExistMemberField.ifPresent(s -> {
                        if (!s.equals(input.getEmail())) {
                            this.logger.log(format("Caller <PK,`%s`> Altering Member <PK,`%s`> EMAIL from `%s` to `%s`", callerPK, input.getPrimaryKey(), s, input.getEmail()), WARN);
                        }
                    });
                }
                case PHONES -> {
                    ofNullable(input.getPhonesDdbMap()).ifPresent(m -> member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                                                                       .m(m)
                                                                                                                       .build()));
                    preExistMemberField.ifPresent(m -> {
                        if (!m.equals(input.getPhonesDdbMap())) {
                            this.logger.log(format("Caller <PK,`%s`> Altering Member <PK,`%s`> PHONES from `%s` to `%s`", callerPK, input.getPrimaryKey(), m, input.getPhonesDdbMap()), WARN);
                        }
                    });
                }
                case ADDRESS -> {
                    ofNullable(input.getAddress()).ifPresent(ss -> member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                                                                   .ss(ss)
                                                                                                                   .build()));
                    preExistMemberField.ifPresent(ss -> {
                        if (!ss.equals(input.getAddress())) {
                            this.logger.log(format("Caller <PK,`%s`> Altering Member <PK,`%s`> ADDRESS from `%s` to `%s`", callerPK, input.getPrimaryKey(), ss, input.getAddress()), WARN);
                        }
                    });
                }
                case ANCESTOR -> preExistMemberField.ifPresent(s -> {
                    final String ancestorPK = ofNullable(input.getAncestor()).map(MemberReference::getPrimaryKey)
                                                                             .orElse(null);
                    if (!s.equals(ancestorPK)) {
                        this.logger.log(format("Caller <PK,`%s`> Altering Member <PK,`%s`> ANCESTOR from `%s` to `%s`", callerPK, input.getPrimaryKey(), s, ancestorPK), WARN);
                    }
                });
                case IS_ANCESTOR_SPOUSE -> preExistMemberField.ifPresent(b -> {
                    if (!b.equals(input.getIsAncestorSpouse())) {
                        this.logger.log(format("Caller <PK,`%s`> Altering Member <PK,`%s`> IS_ANCESTOR_SPOUSE from `%s` to `%s`", callerPK, input.getPrimaryKey(), b, input.getIsAncestorSpouse()),
                                        WARN);
                    }
                });
                default -> {
                }
            }
        }
        return member;
    }
}
