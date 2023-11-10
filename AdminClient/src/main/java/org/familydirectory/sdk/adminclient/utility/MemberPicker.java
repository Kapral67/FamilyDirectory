package org.familydirectory.sdk.adminclient.utility;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.sdk.adminclient.events.model.MemberRecord;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import static java.util.Collections.emptyMap;

public final
class MemberPicker {
    private final Set<MemberRecord> entries;

    private
    MemberPicker () {
        super();
        this.entries = new HashSet<>();
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
    }

    private
    void add_or_overwrite_entry (final @NotNull MemberRecord entry) {
        this.entries.remove(entry);
        this.entries.add(entry);
    }

    public static
    Set<MemberRecord> getEntries () {
        return Singleton.getInstance().entries;
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
