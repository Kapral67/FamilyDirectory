package org.familydirectory.assets.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.member.MemberParams;
import org.familydirectory.assets.ddb.member.Member;
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
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static java.util.Map.of;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.familydirectory.assets.ddb.enums.DdbTable.FAMILIES;
import static org.familydirectory.assets.ddb.enums.DdbTable.MEMBERS;
import static org.familydirectory.assets.ddb.enums.DdbTable.PK;
import static org.familydirectory.assets.ddb.enums.family.FamilyParams.DESCENDANTS;
import static org.familydirectory.assets.ddb.enums.family.FamilyParams.SPOUSE;
import static software.amazon.awssdk.http.HttpStatusCode.BAD_REQUEST;
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

    @Override
    public
    APIGatewayProxyResponseEvent handleRequest (APIGatewayProxyRequestEvent event, Context context) {
//      Get Caller List
        final List<Map<String, AttributeValue>> callerPKs;
        try {
            @SuppressWarnings("unchecked")
            final Map<String, Object> callerClaims = (Map<String, Object>) requireNonNull(event.getRequestContext()
                                                                                               .getAuthorizer()
                                                                                               .get("claims"));
            final String callerEmail = Optional.of(callerClaims)
                                               .map(map -> map.get("email"))
                                               .map(Object::toString)
                                               .filter(Predicate.not(String::isBlank))
                                               .orElseThrow(NullPointerException::new);

            context.getLogger()
                   .log(format("Caller email: `%s`", callerEmail), INFO);

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
            if (!callerQueryResponse.hasItems()) {
                throw new NullPointerException();
            }
            callerPKs = callerQueryResponse.items();

        } catch (final NullPointerException | ClassCastException e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(UNAUTHORIZED)
                                                     .withBody("Caller Email Not Found");
        }

//      Deserialization
        final Member input;
        try {
            input = mapper.convertValue(event.getBody(), Member.class);
        } catch (final IllegalArgumentException e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(BAD_REQUEST)
                                                     .withBody("Invalid Member");
        }

//      Check Caller Authorization
        {
            boolean isCallerAuthorized = false;
            for (final Map<String, AttributeValue> pk : callerPKs) {
                final String callerPk = pk.get(PK.getName())
                                          .s();
                // TODO: SOLVE THE SPOUSE DILEMMA
                if (callerPk.equals(input.getPrimaryKey()) || (!isNull(input.getAncestor()) && callerPk.equals(input.getAncestor()
                                                                                                                    .getPrimaryKey())))
                {
                    isCallerAuthorized = true;
                    context.getLogger()
                           .log(format("Caller `%s` Creating Member `%s`", callerPk, input.getPrimaryKey()), INFO);
                    break;
                }
            }
            if (!isCallerAuthorized) {
                return new APIGatewayProxyResponseEvent().withStatusCode(UNAUTHORIZED)
                                                         .withBody("Caller Not Authorized To Create Requested Member");
            }
        }

//      Check If Member Already Exists
        final GetItemRequest getItemRequest = GetItemRequest.builder()
                                                            .tableName(MEMBERS.name())
                                                            .key(singletonMap(PK.getName(), AttributeValue.builder()
                                                                                                          .s(input.getPrimaryKey())
                                                                                                          .build()))
                                                            .build();
        final GetItemResponse getItemResponse = client.getItem(getItemRequest);
        if (getItemResponse.hasItem()) {
            // FIXME: Should Allow Overwrites Given Restrictive Authorization
            return new APIGatewayProxyResponseEvent().withStatusCode(UNAUTHORIZED)
                                                     .withBody(format("EEXIST: '%s' Born: '%s' Already Exists", input.getFullName(), input.getBirthdayString()));
        }

//      Build New Item
        final Map<String, AttributeValue> member = new HashMap<>();
        member.put(PK.getName(), AttributeValue.builder()
                                               .s(input.getPrimaryKey())
                                               .build());

        for (final MemberParams field : MemberParams.values()) {
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
                case DEATHDAY -> ofNullable(input.getDeathdayString()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                                                                                      .s(s)
                                                                                                                                      .build()));
                case EMAIL -> ofNullable(input.getEmail()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                                                                          .s(s)
                                                                                                                          .build()));
                case PHONES -> ofNullable(input.getPhonesDdbMap()).ifPresent(m -> member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                                                                                  .m(m)
                                                                                                                                  .build()));
                case ADDRESS -> ofNullable(input.getAddress()).ifPresent(ss -> member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                                                                               .ss(ss)
                                                                                                                               .build()));
                case ANCESTOR -> ofNullable(input.getAncestor()).ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                                                                                .s(s.getPrimaryKey())
                                                                                                                                .build()));
                case IS_ANCESTOR_SPOUSE -> ofNullable(input.getIsAncestorSpouse()).ifPresent(bool -> member.put(field.jsonFieldName(), AttributeValue.builder()
                                                                                                                                                     .bool(bool)
                                                                                                                                                     .build()));
                default -> {
                }
            }
        }

//      Build Transaction List
        final List<TransactWriteItem> transactWriteItems = new ArrayList<>();
        if (nonNull(input.getAncestor())) {
            if (nonNull(input.getIsAncestorSpouse())) {
//              Set spouse field in existing entry in FAMILIES Table
                // TODO: INVENT SOME MECHANISM TO FREE UNWANTED SPOUSES
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
//              Append to dependents list in existing entry in FAMILIES Table
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
                if (input.isAdult()) {
//                  Create new entry in FAMILIES Table
                    transactWriteItems.add(TransactWriteItem.builder()
                                                            .put(Put.builder()
                                                                    .tableName(FAMILIES.name())
                                                                    .item(of(PK.getName(), AttributeValue.builder()
                                                                                                         .s(input.getPrimaryKey())
                                                                                                         .build()))
                                                                    .build())
                                                            .build());
                }
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
    }

    private
    enum CallerAuthorization {
        UNAUTHORIZED,
        AUTHORIZED
    }
}
