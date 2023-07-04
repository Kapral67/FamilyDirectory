package org.familydirectory.assets.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.familydirectory.assets.ddb.models.members.MembersModel;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.lang.Boolean.TRUE;
import static org.familydirectory.assets.ddb.DdbTable.MEMBERS;
import static org.familydirectory.assets.ddb.DdbTable.PK;

public class FamilyDirectoryAdminCreateMemberLambda implements RequestHandler<MembersModel, Boolean> {
    @Override
    public Boolean handleRequest(MembersModel event, Context context) {
        final Map<String, AttributeValue> member = new HashMap<>();
        member.put(PK.getName(), AttributeValue.builder().n(UUID.randomUUID().toString()).build());
        member.put(MEMBERS.sortKey().getName(), AttributeValue.builder().s(event.getFullName()).build());
        loop:
        for (final MembersModel.Params field : MembersModel.Params.values()) {
            switch (field) {
                case FIRST_NAME -> member.put("firstName", AttributeValue.builder().s(event.firstName()).build());
                case LAST_NAME -> member.put("lastName", AttributeValue.builder().s(event.lastName()).build());
                case SUFFIX ->
                        event.suffix().ifPresent(s -> member.put("suffix", AttributeValue.builder().s(s).build()));
                case BIRTHDAY -> member.put("birthday", AttributeValue.builder().s(event.birthday()).build());
                case DEATHDAY -> {
                    event.deathday().ifPresent(s -> member.put("deathday", AttributeValue.builder().s(s).build()));
                    break loop;
                }
                case EMAIL -> event.email().ifPresent(s -> member.put("email", AttributeValue.builder().s(s).build()));
                case PHONES -> event.getPhonesDdbMap()
                        .ifPresent(m -> member.put("phones", AttributeValue.builder().m(m).build()));
                case ADDRESS ->
                        event.address().ifPresent(ss -> member.put("address", AttributeValue.builder().ss(ss).build()));
                default -> throw new IllegalStateException("Invalid Member Table Field");
            }
        }

        PutItemRequest putItemRequest = PutItemRequest.builder().tableName(MEMBERS.name()).item(member).build();
        DynamoDbClient.create().putItem(putItemRequest);
        return TRUE;
    }
}
