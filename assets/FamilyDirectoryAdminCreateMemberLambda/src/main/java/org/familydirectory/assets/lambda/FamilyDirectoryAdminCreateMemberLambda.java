package org.familydirectory.assets.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.familydirectory.assets.ddb.enums.member.MemberParams;
import org.familydirectory.assets.ddb.member.Member;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.Update;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Map.of;
import static java.util.Objects.nonNull;
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

public class FamilyDirectoryAdminCreateMemberLambda
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final DynamoDbClient client = create();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String KEY = "#k";
    private static final String VALUE = ":v";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
//      Check Authorization


//      Deserialization
        final Member input;
        try {
            input = mapper.convertValue(event.getBody(), Member.class);
        } catch (final IllegalArgumentException e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(BAD_REQUEST).withBody("Invalid Member");
        }

//      Check If Member Already Exists
        final QueryRequest queryRequest =
                QueryRequest.builder().tableName(MEMBERS.name()).keyConditionExpression(format("%s = %s", KEY, VALUE))
                        .expressionAttributeNames(of(KEY, PK.getName()))
                        .expressionAttributeValues(of(VALUE, AttributeValue.builder().s(input.getPrimaryKey()).build()))
                        .build();
        try {
            final QueryResponse queryResponse = client.query(queryRequest);
            if (!queryResponse.items().isEmpty()) {
                // FIXME: Should Allow Overwrites Given Restrictive Authorization
                return new APIGatewayProxyResponseEvent().withStatusCode(UNAUTHORIZED).withBody(
                        format("EEXIST: '%s' Born: '%s' Already Exists", input.getFullName(),
                                input.getBirthdayString()));
            }
        } catch (final ResourceNotFoundException ignored) {
            /* This exception is ignored because the whole purpose of this query is to ensure that the
             * Member-To-Be-Created does not already exist, so letting this exception propagate, or even logging this
             * exception, is not necessary as this exception is expected behavior.
             */
        }

//      Build New Item
        final Map<String, AttributeValue> member = new HashMap<>();
        member.put(PK.getName(), AttributeValue.builder().s(input.getPrimaryKey()).build());

        for (final MemberParams field : MemberParams.values()) {
            switch (field) {
                case FIRST_NAME ->
                        member.put(field.jsonFieldName(), AttributeValue.builder().s(input.getFirstName()).build());
                case LAST_NAME ->
                        member.put(field.jsonFieldName(), AttributeValue.builder().s(input.getLastName()).build());
                case SUFFIX -> ofNullable(input.getSuffix()).ifPresent(
                        s -> member.put(field.jsonFieldName(), AttributeValue.builder().s(s.value()).build()));
                case BIRTHDAY -> member.put(field.jsonFieldName(),
                        AttributeValue.builder().s(input.getBirthdayString()).build());
                case DEATHDAY -> ofNullable(input.getDeathdayString()).ifPresent(
                        s -> member.put(field.jsonFieldName(), AttributeValue.builder().s(s).build()));
                case EMAIL -> ofNullable(input.getEmail()).ifPresent(
                        s -> member.put(field.jsonFieldName(), AttributeValue.builder().s(s).build()));
                case PHONES -> ofNullable(input.getPhonesDdbMap()).ifPresent(
                        m -> member.put(field.jsonFieldName(), AttributeValue.builder().m(m).build()));
                case ADDRESS -> ofNullable(input.getAddress()).ifPresent(
                        ss -> member.put(field.jsonFieldName(), AttributeValue.builder().ss(ss).build()));
                default -> {
                }
            }
        }

//      Build Transaction List
        final List<TransactWriteItem> transactWriteItems = new ArrayList<>();
        if (nonNull(input.getAncestor())) {
            if (nonNull(input.getIsAncestorSpouse())) {
//              Set spouse field in existing entry in FAMILIES Table
                transactWriteItems.add(TransactWriteItem.builder().update(Update.builder().tableName(FAMILIES.name())
                        .key(of(PK.getName(), AttributeValue.builder().s(input.getAncestor().getPrimaryKey()).build()))
                        .updateExpression(format("SET %s = %s", KEY, VALUE))
                        .expressionAttributeNames(of(KEY, SPOUSE.jsonFieldName()))
                        .expressionAttributeValues(of(VALUE, AttributeValue.builder().s(input.getPrimaryKey()).build()))
                        .build()).build());
            } else {
//              Append to dependents list in existing entry in FAMILIES Table
                transactWriteItems.add(TransactWriteItem.builder().update(Update.builder().tableName(FAMILIES.name())
                        .key(of(PK.getName(), AttributeValue.builder().s(input.getAncestor().getPrimaryKey()).build()))
                        .updateExpression(format("SET %s = list_append(%s, %s)", KEY, KEY, VALUE))
                        .expressionAttributeNames(of(KEY, DESCENDANTS.jsonFieldName()))
                        .expressionAttributeValues(of(VALUE, AttributeValue.builder().s(input.getPrimaryKey()).build()))
                        .build()).build());
                if (input.isAdult()) {
//                  Create new entry in FAMILIES Table
                    transactWriteItems.add(TransactWriteItem.builder().put(Put.builder().tableName(FAMILIES.name())
                                    .item(of(PK.getName(), AttributeValue.builder().s(input.getPrimaryKey()).build())).build())
                            .build());
                }
            }
        } else {
            // TODO: Extra Authorization Needed For Creating Members with No Ancestry
            // FIXME: Rudimentary, Broad, Solution
            return new APIGatewayProxyResponseEvent().withStatusCode(UNAUTHORIZED);
        }

//      Create new entry in MEMBERS Table
        transactWriteItems.add(
                TransactWriteItem.builder().put(Put.builder().tableName(MEMBERS.name()).item(member).build()).build());

//      Execute Transaction
        client.transactWriteItems(TransactWriteItemsRequest.builder().transactItems(transactWriteItems).build());

        return new APIGatewayProxyResponseEvent().withStatusCode(OK);
    }
}
