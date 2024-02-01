package org.familydirectory.sdk.adminclient.utility.pickers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.utility.pickers.model.PickerModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final
class CognitoUserPicker implements PickerModel {
    @NotNull
    private final Map<MemberRecord, String> entries;

    private
    CognitoUserPicker () {
        super();
        this.entries = new HashMap<>();
        try (final DynamoDbClient dbClient = DynamoDbClient.create()) {
            Map<String, AttributeValue> lastEvaluatedKey = emptyMap();
            do {
                final ScanRequest.Builder scanRequestBuilder = ScanRequest.builder()
                                                                          .tableName(DdbTable.COGNITO.name())
                                                                          .consistentRead(true);
                if (!lastEvaluatedKey.isEmpty()) {
                    scanRequestBuilder.exclusiveStartKey(lastEvaluatedKey);
                }
                final ScanResponse scanResponse = dbClient.scan(scanRequestBuilder.build());

                for (final Map<String, AttributeValue> cognitoEntry : scanResponse.items()) {
                    final String sub = requireNonNull(cognitoEntry.get(CognitoTableParameter.ID.jsonFieldName())
                                                                  .s());
                    final String memberId = requireNonNull(cognitoEntry.get(CognitoTableParameter.MEMBER.jsonFieldName())
                                                                       .s());
                    final Map<String, AttributeValue> ddbMember = dbClient.getItem(GetItemRequest.builder()
                                                                                                 .tableName(DdbTable.MEMBER.name())
                                                                                                 .key(singletonMap(MemberTableParameter.ID.jsonFieldName(), AttributeValue.fromS(memberId)))
                                                                                                 .build())
                                                                          .item();
                    this.add_or_overwrite_entry(new MemberRecord(UUID.fromString(memberId), Member.convertDdbMap(ddbMember), UUID.fromString(
                            ddbMember.get(MemberTableParameter.FAMILY_ID.jsonFieldName())
                                     .s())));
                }

                lastEvaluatedKey = scanResponse.lastEvaluatedKey();
            } while (!lastEvaluatedKey.isEmpty());
        }
    }

    private
    void add_or_overwrite_entry (final @NotNull MemberRecord entry) {
        this.entries.remove(entry);
        final String sub = getCognitoSubFromMemberRecord(entry);
        if (nonNull(sub)) {
            this.entries.put(entry, sub);
        }
    }

    @Nullable
    private static
    String getCognitoSubFromMemberRecord (final @NotNull MemberRecord memberRecord) {
        try (final DynamoDbClient dbClient = DynamoDbClient.create()) {
            final QueryRequest cognitoMemberQueryRequest = QueryRequest.builder()
                                                                       .tableName(DdbTable.COGNITO.name())
                                                                       .indexName(requireNonNull(CognitoTableParameter.MEMBER.gsiProps()).getIndexName())
                                                                       .keyConditionExpression("#memberId = :memberId")
                                                                       .expressionAttributeNames(singletonMap("#memberId", CognitoTableParameter.MEMBER.gsiProps()
                                                                                                                                                       .getPartitionKey()
                                                                                                                                                       .getName()))
                                                                       .expressionAttributeValues(singletonMap(":memberId", AttributeValue.fromS(memberRecord.id()
                                                                                                                                                             .toString())))
                                                                       .limit(1)
                                                                       .build();
            final QueryResponse cognitoMemberQueryResponse = dbClient.query(cognitoMemberQueryRequest);
            if (!cognitoMemberQueryResponse.items()
                                           .isEmpty())
            {
                return ofNullable(cognitoMemberQueryResponse.items()
                                                            .getFirst()
                                                            .get(CognitoTableParameter.ID.jsonFieldName())).map(AttributeValue::s)
                                                                                                           .filter(Predicate.not(String::isBlank))
                                                                                                           .orElseThrow();
            } else {
                return null;
            }
        }
    }

    public static
    boolean isEmpty () {
        return Singleton.getInstance().entries.isEmpty();
    }

    @NotNull
    public static
    List<MemberRecord> getEntries () {
        return Singleton.getInstance().entries.keySet()
                                              .stream()
                                              .sorted(LAST_NAME_COMPARATOR)
                                              .toList();
    }

    @NotNull
    public static
    String getCognitoSub (final @NotNull MemberRecord memberRecord) {
        return ofNullable(Singleton.getInstance().entries.get(requireNonNull(memberRecord))).orElseThrow();
    }

    public static
    void removeEntry (final @NotNull MemberRecord entry) {
        Singleton.getInstance()
                 .remove_entry(entry);
    }

    private
    void remove_entry (final @NotNull MemberRecord entry) {
        this.entries.remove(entry);
    }

    public static
    void addEntry (final @NotNull MemberRecord entry) {
        Singleton.getInstance()
                 .add_or_overwrite_entry(entry);
    }

    private
    enum Singleton {
        ;
        private static final CognitoUserPicker INSTANCE = new CognitoUserPicker();

        private static
        CognitoUserPicker getInstance () {
            return INSTANCE;
        }
    }
}
