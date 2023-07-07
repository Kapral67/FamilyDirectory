package org.familydirectory.assets.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.familydirectory.assets.ddb.models.members.MembersModel;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.lang.Boolean.TRUE;
import static org.familydirectory.assets.ddb.DdbTable.MEMBERS;
import static org.familydirectory.assets.ddb.DdbTable.PK;

public class FamilyDirectoryAdminCreateMemberLambda implements RequestHandler<Map<String, Object>, Boolean> {
    private static final DynamoDbClient client = DynamoDbClient.create();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Boolean handleRequest(Map<String, Object> event, Context context) {
        // Deserialization
        final MembersModel input = mapper.convertValue(event, MembersModel.class);

        // Member Primary Key
        final String primaryKey =
                DigestUtils.sha256Hex(String.format("%s%s", input.getFullName(), input.getBirthdayString()));

        // Check If Member Already Exists
        final String expressionAttributeValuesKey = ":v1";
        final Map<String, AttributeValue> expressionAttributeValues =
                Map.of(expressionAttributeValuesKey, AttributeValue.builder().s(primaryKey).build());
        final QueryRequest queryRequest = QueryRequest.builder().tableName(MEMBERS.name())
                .keyConditionExpression(PK.getName() + " = " + expressionAttributeValuesKey)
                .expressionAttributeValues(expressionAttributeValues).build();
        final QueryResponse queryResponse = client.query(queryRequest);
        if (!queryResponse.items().isEmpty()) {
            throw new UnsupportedOperationException(
                    String.format("EEXIST: '%s' Born: '%s' Already Exists", input.getFullName(),
                            input.getBirthdayString()));
        }

        // Build New Item
        final Map<String, AttributeValue> member = new HashMap<>();
        member.put(PK.getName(), AttributeValue.builder().s(primaryKey).build());
        member.put(MEMBERS.sortKey().getName(), AttributeValue.builder().s(input.getFullName()).build());

        for (final MembersModel.Params field : MembersModel.Params.values()) {
            switch (field) {
                case FIRST_NAME ->
                        member.put(field.jsonFieldName(), AttributeValue.builder().s(input.getFirstName()).build());
                case LAST_NAME ->
                        member.put(field.jsonFieldName(), AttributeValue.builder().s(input.getLastName()).build());
                case SUFFIX -> Optional.ofNullable(input.getSuffix()).ifPresent(
                        s -> member.put(field.jsonFieldName(), AttributeValue.builder().s(s.value()).build()));
                case BIRTHDAY -> member.put(field.jsonFieldName(),
                        AttributeValue.builder().s(input.getBirthdayString()).build());
                case DEATHDAY -> Optional.ofNullable(input.getDeathdayString())
                        .ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.builder().s(s).build()));
                case EMAIL -> Optional.ofNullable(input.getEmail())
                        .ifPresent(s -> member.put(field.jsonFieldName(), AttributeValue.builder().s(s).build()));
                case PHONES -> Optional.ofNullable(input.getPhonesDdbMap())
                        .ifPresent(m -> member.put(field.jsonFieldName(), AttributeValue.builder().m(m).build()));
                case ADDRESS -> Optional.ofNullable(input.getAddress())
                        .ifPresent(ss -> member.put(field.jsonFieldName(), AttributeValue.builder().ss(ss).build()));
                default -> throw new IllegalStateException("Invalid Member Table Field");
            }
        }

        // Submit PutItemRequest
        final PutItemRequest putItemRequest = PutItemRequest.builder().tableName(MEMBERS.name()).item(member).build();
        client.putItem(putItemRequest);
        return TRUE;
    }
}
