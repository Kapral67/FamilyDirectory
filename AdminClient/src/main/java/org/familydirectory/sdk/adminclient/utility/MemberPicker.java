package org.familydirectory.sdk.adminclient.utility;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
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
class MemberPicker {
    private static final Comparator<MemberRecord> LAST_NAME_COMPARATOR = Comparator.comparing(memberRecord -> memberRecord.member()
                                                                                                                          .getLastName());
    @NotNull
    private final Set<MemberRecord> entries;
    @NotNull
    private final Map<MemberRecord, String> cognitoEntries;

    private
    MemberPicker () {
        super();
        this.entries = new HashSet<>();
        this.cognitoEntries = new HashMap<>();
        try (final DynamoDbClient dbClient = DynamoDbClient.create()) {
            Map<String, AttributeValue> lastEvaluatedKey = emptyMap();
            do {
                final ScanRequest.Builder scanRequestBuilder = ScanRequest.builder()
                                                                          .tableName(DdbTable.MEMBER.name())
                                                                          .consistentRead(true);
                if (!lastEvaluatedKey.isEmpty()) {
                    scanRequestBuilder.exclusiveStartKey(lastEvaluatedKey);
                }
                final ScanResponse scanResponse = dbClient.scan(scanRequestBuilder.build());

                for (final Map<String, AttributeValue> entry : scanResponse.items()) {
                    this.add_or_overwrite_entry(new MemberRecord(UUID.fromString(entry.get(MemberTableParameter.ID.jsonFieldName())
                                                                                      .s()), Member.convertDdbMap(entry), UUID.fromString(entry.get(MemberTableParameter.FAMILY_ID.jsonFieldName())
                                                                                                                                               .s())));
                }

                lastEvaluatedKey = scanResponse.lastEvaluatedKey();
            } while (!lastEvaluatedKey.isEmpty());
        }
        for (final MemberRecord memberRecord : this.entries) {
            final String sub = getCognitoSubFromMemberRecord(memberRecord);
            if (nonNull(sub)) {
                this.cognitoEntries.put(memberRecord, sub);
            }
        }
    }

    private
    void add_or_overwrite_entry (final @NotNull MemberRecord entry) {
        this.entries.remove(entry);
        this.entries.add(entry);
        this.cognitoEntries.remove(entry);
        final String sub = getCognitoSubFromMemberRecord(entry);
        if (nonNull(sub)) {
            this.cognitoEntries.put(entry, sub);
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

    public static
    boolean isCognitoEmpty () {
        return Singleton.getInstance().cognitoEntries.isEmpty();
    }

    @NotNull
    public static
    List<MemberRecord> getEntries () {
        return Singleton.getInstance().entries.stream()
                                              .sorted(LAST_NAME_COMPARATOR)
                                              .toList();
    }

    @NotNull
    public static
    List<MemberRecord> getCognitoMemberRecords () {
        return Singleton.getInstance().cognitoEntries.keySet()
                                                     .stream()
                                                     .sorted(LAST_NAME_COMPARATOR)
                                                     .toList();
    }

    @NotNull
    public static
    String getCognitoSub (final @NotNull MemberRecord memberRecord) throws NoSuchElementException {
        return ofNullable(Singleton.getInstance().cognitoEntries.get(requireNonNull(memberRecord))).orElseThrow();
    }

    public static
    void addEntry (final @NotNull MemberRecord entry) {
        Singleton.getInstance()
                 .add_or_overwrite_entry(entry);
    }

    public static
    void removeEntry (final @NotNull MemberRecord entry) {
        Singleton.getInstance()
                 .remove_entry(entry);
    }

    private
    void remove_entry (final @NotNull MemberRecord entry) {
        this.entries.remove(entry);
        this.cognitoEntries.remove(entry);
    }

    private
    enum Singleton {
        ;
        private static final MemberPicker INSTANCE = new MemberPicker();

        private static
        MemberPicker getInstance () {
            return INSTANCE;
        }
    }
}
