package org.familydirectory.assets.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.familydirectory.assets.ddb.enums.member.MemberParams;
import org.familydirectory.assets.ddb.member.Member;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
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
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.familydirectory.assets.ddb.enums.DdbTable.FAMILIES;
import static org.familydirectory.assets.ddb.enums.DdbTable.MEMBERS;
import static org.familydirectory.assets.ddb.enums.DdbTable.PK;
import static org.familydirectory.assets.ddb.enums.family.FamilyParams.DESCENDANTS;
import static org.familydirectory.assets.ddb.enums.family.FamilyParams.SPOUSE;
import static software.amazon.awssdk.http.HttpStatusCode.OK;
import static software.amazon.awssdk.services.dynamodb.DynamoDbClient.create;

public class FamilyDirectoryAdminCreateMemberLambda implements RequestHandler<Map<String, Object>, Integer> {
    private static final DynamoDbClient client = create();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Integer handleRequest(Map<String, Object> event, Context context) {
//      Deserialization
        final Member input = mapper.convertValue(event, Member.class);

//      Check If Member Already Exists
        final QueryRequest queryRequest =
                QueryRequest.builder().tableName(MEMBERS.name()).keyConditionExpression("#k = :v")
                        .expressionAttributeNames(of("#k", PK.getName()))
                        .expressionAttributeValues(of(":v", AttributeValue.builder().s(input.getPrimaryKey()).build()))
                        .build();
        final QueryResponse queryResponse = client.query(queryRequest);
        if (!queryResponse.items().isEmpty()) {
            throw new UnsupportedOperationException(
                    format("EEXIST: '%s' Born: '%s' Already Exists", input.getFullName(), input.getBirthdayString()));
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
            if (requireNonNull(input.getIsAncestorSpouse())) {
//              Set spouse field in existing entry in FAMILIES Table
                transactWriteItems.add(TransactWriteItem.builder().update(Update.builder().tableName(FAMILIES.name())
                        .key(of(PK.getName(), AttributeValue.builder().s(input.getAncestor().getPrimaryKey()).build()))
                        .updateExpression("SET #k = :v").expressionAttributeNames(of("#k", SPOUSE.jsonFieldName()))
                        .expressionAttributeValues(of(":v", AttributeValue.builder().s(input.getPrimaryKey()).build()))
                        .build()).build());
            } else {
//              Append to dependents list in existing entry in FAMILIES Table
                transactWriteItems.add(TransactWriteItem.builder().update(Update.builder().tableName(FAMILIES.name())
                        .key(of(PK.getName(), AttributeValue.builder().s(input.getAncestor().getPrimaryKey()).build()))
                        .updateExpression("SET #k = list_append(#k, :v)")
                        .expressionAttributeNames(of("#k", DESCENDANTS.jsonFieldName()))
                        .expressionAttributeValues(of(":v", AttributeValue.builder().s(input.getPrimaryKey()).build()))
                        .build()).build());
                if (input.isAdult()) {
//                  Create new entry in FAMILIES Table
                    transactWriteItems.add(TransactWriteItem.builder().put(Put.builder().tableName(FAMILIES.name())
                                    .item(of(PK.getName(), AttributeValue.builder().s(input.getPrimaryKey()).build())).build())
                            .build());
                }
            }
        }

//      Create new entry in MEMBERS Table
        transactWriteItems.add(
                TransactWriteItem.builder().put(Put.builder().tableName(MEMBERS.name()).item(member).build()).build());

//      Execute Transaction
        client.transactWriteItems(TransactWriteItemsRequest.builder().transactItems(transactWriteItems).build());

        return OK;
    }
}
