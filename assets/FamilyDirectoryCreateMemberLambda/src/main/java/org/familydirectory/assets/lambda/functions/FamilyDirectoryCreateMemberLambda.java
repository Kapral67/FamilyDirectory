package org.familydirectory.assets.lambda.functions;

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
import java.util.UUID;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.SuffixType;
import org.familydirectory.assets.ddb.enums.member.MemberParams;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.lambda.exceptions.ApiResponseAsRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.DEBUG;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.ERROR;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.TRACE;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.WARN;
import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.familydirectory.assets.ddb.enums.DdbTable.COGNITO;
import static org.familydirectory.assets.ddb.enums.DdbTable.MEMBERS;
import static org.familydirectory.assets.ddb.enums.DdbTable.PK;
import static org.familydirectory.assets.ddb.enums.cognito.CognitoParams.MEMBER;
import static org.familydirectory.assets.ddb.enums.member.MemberParams.SPOUSE;
import static software.amazon.awssdk.services.dynamodb.DynamoDbClient.create;

public
class FamilyDirectoryCreateMemberLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final DynamoDbClient client = create();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String KEY = "#k";
    private static final String VALUE = ":v";

    private LambdaLogger logger = null;
    private UUID inputId = null;
    private String callerMemberId = null;
    private Map<String, AttributeValue> callerMember = null;
    private Map<String, AttributeValue> inputAncestor = null;

    @Override
    public
    APIGatewayProxyResponseEvent handleRequest (APIGatewayProxyRequestEvent event, Context context)
    {
        this.logger = context.getLogger();
        try {
//      Get Caller
            try {
                @SuppressWarnings("unchecked")
                final Map<String, Object> callerClaims = (Map<String, Object>) requireNonNull(event.getRequestContext()
                                                                                                   .getAuthorizer()
                                                                                                   .get("claims"));
                final String callerSub = Optional.of(callerClaims)
                                                 .map(map -> map.get("sub"))
                                                 .map(Object::toString)
                                                 .filter(Predicate.not(String::isBlank))
                                                 .orElseThrow(NullPointerException::new);

                this.logger.log(format("<COGNITO_SUB,`%s`> Invoked CreateMember Lambda", callerSub), INFO);

                final Map<String, AttributeValue> callerCognito = ofNullable(getDdbItem(callerSub, COGNITO)).orElseThrow(NullPointerException::new);

                this.callerMemberId = ofNullable(callerCognito.get(MEMBER.jsonFieldName())).map(AttributeValue::s)
                                                                                           .filter(Predicate.not(String::isBlank))
                                                                                           .orElseThrow(NullPointerException::new);
                this.callerMember = ofNullable(getDdbItem(this.callerMemberId, MEMBERS)).orElseThrow(NullPointerException::new);

            } catch (final NullPointerException | ClassCastException e) {
                this.logger.log(e.getMessage(), ERROR);
                this.logger.log(e.getCause()
                                 .toString(), DEBUG);
                this.logger.log(Arrays.toString(e.getStackTrace()), TRACE);
                return new APIGatewayProxyResponseEvent().withStatusCode(SC_UNAUTHORIZED);
            }

            this.logger.log(format("<MEMBER,`%s`> Authenticated", this.callerMemberId), INFO);

//      Deserialization
            final Member input;
            try {
                input = mapper.convertValue(event.getBody(), Member.class);
            } catch (final IllegalArgumentException e) {
                this.logger.log(format("<MEMBER,`%s`> submitted invalid CreateMember request", this.callerMemberId), WARN);
                this.logger.log(e.getMessage(), WARN);
                this.logger.log(e.getCause()
                                 .toString(), DEBUG);
                this.logger.log(Arrays.toString(e.getStackTrace()), TRACE);
                return new APIGatewayProxyResponseEvent().withStatusCode(SC_BAD_REQUEST);
            }

            final String callerPK = this.checkCallerAuthorization(input);

//      Get PreExisting Member Item if Existing
            this.validateUniqueInput(input);

//      Build Member
            final Map<String, AttributeValue> member = this.buildMember(input, callerPK);

//      Build Transaction List
            // TODO: FUNCTION CALL

//      Execute Transaction
            client.transactWriteItems(TransactWriteItemsRequest.builder()
                                                               .transactItems(transactWriteItems)
                                                               .build());

            return new APIGatewayProxyResponseEvent().withStatusCode(SC_CREATED);
        } catch (final ApiResponseAsRuntimeException e) {
            return e.getResponseEvent();
        } catch (final Exception e) {
            this.logger.log(e.getMessage(), FATAL);
            ofNullable(e.getCause()).ifPresent(throwable -> this.logger.log(throwable.toString(), DEBUG));
            this.logger.log(Arrays.toString(e.getStackTrace()), TRACE);
            return new APIGatewayProxyResponseEvent().withStatusCode(SC_INTERNAL_SERVER_ERROR);
        }
    }

    private @NotNull
    String checkCallerAuthorization (final @NotNull Member input) {
        // THEMSELF
        if (pk.equals(input.getPrimaryKey())) {
            this.logger.log(format("Caller <PK,`%s`> <EMAIL,`%s`> Upserting Self", pk, callerEmail), INFO);
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
                this.logger.log(format("Caller <PK,`%s`> Upserting Descendant As Parent <PK,`%s`>", pk, input.getPrimaryKey()), INFO);
                return pk;
            } else {
                if (isNull(this.inputAncestor)) {
                    this.inputAncestor = getDdbItem(input.getAncestor()
                                                         .getPrimaryKey());
                }
                final String inputAncestorSpousePK = ofNullable(this.inputAncestor).flatMap(a -> ofNullable(a.get(SPOUSE.jsonFieldName())).map(AttributeValue::s))
                                                                                   .orElse(null);
                if (pk.equals(inputAncestorSpousePK)) {
                    this.logger.log(format("Caller <PK,`%s`> Upserting Descendant As Co-Parent <PK,`%s`>", pk, inputAncestorSpousePK), INFO);
                    return pk;
                }
            }
        }
        this.logger.log(format("Caller <PKs,`%s`> cannot upsert Member <PK,`%s`>", callerPKs, input.getPrimaryKey()), WARN);
        throw new ApiResponseAsRuntimeException(new APIGatewayProxyResponseEvent().withStatusCode(SC_UNAUTHORIZED)
                                                                                  .withBody("Caller Not Authorized To Create Requested Member"));
    }

    private
    void validateUniqueInput (final @NotNull Member input) {
        final QueryRequest request = QueryRequest.builder()
                                                 .tableName(MEMBERS.name())
                                                 .indexName(MemberParams.KEY.gsiProps()
                                                                            .getIndexName())
                                                 .keyConditionExpression(format("%s = %s", KEY, VALUE))
                                                 .expressionAttributeNames(Map.of(KEY, MemberParams.KEY.gsiProps()
                                                                                                       .getPartitionKey()
                                                                                                       .getName()))
                                                 .expressionAttributeValues(Map.of(VALUE, AttributeValue.builder()
                                                                                                        .s(input.getKey())
                                                                                                        .build()))
                                                 .build();
        if (client.query(request)
                  .hasItems())
        {
            throw new ApiResponseAsRuntimeException(new APIGatewayProxyResponseEvent().withStatusCode(SC_CONFLICT));
        }
    }

    private static @Nullable
    Map<String, AttributeValue> getDdbItem (final @NotNull String primaryKey, final @NotNull DdbTable ddbTable) {
        final GetItemRequest request = GetItemRequest.builder()
                                                     .tableName(ddbTable.name())
                                                     .key(singletonMap(PK.getName(), AttributeValue.builder()
                                                                                                   .s(primaryKey)
                                                                                                   .build()))
                                                     .build();
        final GetItemResponse response = client.getItem(request);
        return (response.hasItem())
                ? response.item()
                : null;
    }

    private @NotNull
    Map<String, AttributeValue> buildMember (final @NotNull Member input, final @NotNull String callerPK) {

//      Build New Item
        final Map<String, AttributeValue> member = new HashMap<>();
        this.inputId = UUID.randomUUID();
        member.put(PK.getName(), AttributeValue.builder()
                                               .s(this.inputId.toString())
                                               .build());

        for (final MemberParams field : MemberParams.values()) {
            final Optional<Object> preExistMemberField = ofNullable(this.inputMember).flatMap(m -> ofNullable(m.get(field.jsonFieldName())))
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
                case FIRST_NAME -> {
                    member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                    .s(input.getFirstName())
                                                                    .build());
                    preExistMemberField.ifPresent(s -> {
                        if (!s.equals(input.getFirstName())) {
                            this.logger.log(format("Caller <PK,`%s`> Altering Member <PK,`%s`> FIRST_NAME from `%s` to `%s`", callerPK, input.getPrimaryKey(), s, input.getFirstName()), ERROR);
                            throw new ApiResponseAsRuntimeException(new APIGatewayProxyResponseEvent().withStatusCode(SC_FORBIDDEN));
                        }
                    });
                }
                case LAST_NAME -> {
                    member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                    .s(input.getLastName())
                                                                    .build());
                    preExistMemberField.ifPresent(s -> {
                        if (!s.equals(input.getLastName())) {
                            this.logger.log(format("Caller <PK,`%s`> Altering Member <PK,`%s`> LAST_NAME from `%s` to `%s`", callerPK, input.getPrimaryKey(), s, input.getLastName()), WARN);
                            throw new ApiResponseAsRuntimeException(new APIGatewayProxyResponseEvent().withStatusCode(SC_FORBIDDEN));
                        }
                    });
                }
                case SUFFIX -> {
                    ofNullable(input.getSuffix()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                                                                 .s(s.value())
                                                                                                                 .build()));
                    preExistMemberField.ifPresent(s -> {
                        final String inputSuffix = ofNullable(input.getSuffix()).map(SuffixType::value)
                                                                                .orElse(null);
                        if (!s.equals(inputSuffix)) {
                            this.logger.log(format("Caller <PK,`%s`> Altering Member <PK,`%s`> SUFFIX from `%s` to `%s`", callerPK, input.getPrimaryKey(), s, inputSuffix), ERROR);
                            throw new ApiResponseAsRuntimeException(new APIGatewayProxyResponseEvent().withStatusCode(SC_FORBIDDEN));
                        }
                    });
                }
                case BIRTHDAY -> {
                    member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                    .s(input.getBirthdayString())
                                                                    .build());
                    preExistMemberField.ifPresent(s -> {
                        if (!s.equals(input.getBirthdayString())) {
                            this.logger.log(format("Caller <PK,`%s`> Altering Member <PK,`%s`> BIRTHDAY from `%s` to `%s`", callerPK, input.getPrimaryKey(), s, input.getBirthdayString()), ERROR);
                            throw new ApiResponseAsRuntimeException(new APIGatewayProxyResponseEvent().withStatusCode(SC_FORBIDDEN));
                        }
                    });
                }
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
                default -> throw new IllegalStateException();
            }
        }
        return member;
    }

    private @NotNull
    List<TransactWriteItem> buildTransaction (final @NotNull Map<String, AttributeValue> member) {
        final List<TransactWriteItem> transactWriteItems = new ArrayList<>();

        /*
         *
         *
         * */

//        if (nonNull(input.getAncestor())) {
//            if (nonNull(input.getIsAncestorSpouse()) && input.getIsAncestorSpouse()) {
////              Set spouse field in existing entry in FAMILIES Table
//                transactWriteItems.add(TransactWriteItem.builder()
//                                                        .update(Update.builder()
//                                                                      .tableName(FAMILIES.name())
//                                                                      .key(of(PK.getName(), AttributeValue.builder()
//                                                                                                          .s(input.getAncestor()
//                                                                                                                  .getPrimaryKey())
//                                                                                                          .build()))
//                                                                      .updateExpression(format("SET %s = %s", KEY, VALUE))
//                                                                      .expressionAttributeNames(of(KEY, SPOUSE.jsonFieldName()))
//                                                                      .expressionAttributeValues(of(VALUE, AttributeValue.builder()
//                                                                                                                         .s(input.getPrimaryKey())
//                                                                                                                         .build()))
//                                                                      .build())
//                                                        .build());
//            } else {
////              Append to dependents list in existing entry in FAMILIES Table if not already present
//                final GetItemRequest inputAncestorFamilyEntryItemRequest = GetItemRequest.builder()
//                                                                                         .tableName(FAMILIES.name())
//                                                                                         .key(singletonMap(PK.getName(), AttributeValue.builder()
//                                                                                                                                       .s(input.getAncestor()
//                                                                                                                                               .getPrimaryKey())
//                                                                                                                                       .build()))
//                                                                                         .build();
//                final GetItemResponse inputAncestorFamilyEntryItemResponse = client.getItem(inputAncestorFamilyEntryItemRequest);
//                if (inputAncestorFamilyEntryItemResponse.hasItem()) {
//                    final AttributeValue inputAncestorFamilyEntryDescendantsAttributeValue = inputAncestorFamilyEntryItemResponse.item()
//                                                                                                                                 .get(DESCENDANTS.jsonFieldName());
//                    if (inputAncestorFamilyEntryDescendantsAttributeValue.hasSs() && !inputAncestorFamilyEntryDescendantsAttributeValue.ss()
//                                                                                                                                       .contains(input.getPrimaryKey()))
//                    {
//                        transactWriteItems.add(TransactWriteItem.builder()
//                                                                .update(Update.builder()
//                                                                              .tableName(FAMILIES.name())
//                                                                              .key(of(PK.getName(), AttributeValue.builder()
//                                                                                                                  .s(input.getAncestor()
//                                                                                                                          .getPrimaryKey())
//                                                                                                                  .build()))
//                                                                              .updateExpression(format("SET %s = list_append(%s, %s)", KEY, KEY, VALUE))
//                                                                              .expressionAttributeNames(of(KEY, DESCENDANTS.jsonFieldName()))
//                                                                              .expressionAttributeValues(of(VALUE, AttributeValue.builder()
//                                                                                                                                 .s(input.getPrimaryKey())
//                                                                                                                                 .build()))
//                                                                              .build())
//                                                                .build());
//                    }
//                }
////                  Create new entry in FAMILIES Table
//                transactWriteItems.add(TransactWriteItem.builder()
//                                                        .put(Put.builder()
//                                                                .tableName(FAMILIES.name())
//                                                                .item(of(PK.getName(), AttributeValue.builder()
//                                                                                                     .s(input.getPrimaryKey())
//                                                                                                     .build(), ANCESTOR.jsonFieldName(), AttributeValue.builder()
//                                                                                                                                                       .s(input.getAncestor()
//                                                                                                                                                               .getPrimaryKey())
//                                                                                                                                                       .build()))
//                                                                .build())
//                                                        .build());
//            }
//        } else {
//            // TODO: Extra Authorization Needed For Creating Members with No Ancestry
//            // FIXME: Rudimentary, Broad, Solution
//            throw new ResponseThrowable(new APIGatewayProxyResponseEvent().withStatusCode(SC_UNAUTHORIZED));
//        }

//  Create new entry in MEMBERS Table
        transactWriteItems.add(TransactWriteItem.builder()
                                                .put(Put.builder()
                                                        .tableName(MEMBERS.name())
                                                        .item(member)
                                                        .build())
                                                .build());
        return transactWriteItems;
    }
}
