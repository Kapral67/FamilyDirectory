package org.familydirectory.sdk.adminclient.utility.pickers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.familydirectory.sdk.adminclient.utility.pickers.model.PickerModel;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import static java.util.Collections.emptyMap;

public final
class MemberPicker extends PickerModel {
    @NotNull
    private final Set<MemberRecord> entriesSet;
    @NotNull
    private final DynamoDbClient dynamoDbClient;

    public
    MemberPicker () {
        super();
        this.entriesSet = new HashSet<>();
        this.dynamoDbClient = SdkClientProvider.getSdkClientProvider()
                                               .getSdkClient(DynamoDbClient.class);
    }

    @Override
    protected
    boolean is_empty () {
        if (this.entriesList.size() != this.entriesSet.size()) {
            throw new IllegalStateException("MemberPicker Entries Have Incongruent Size");
        }
        return super.is_empty();
    }

    @Override
    protected
    void remove_entry (final @NotNull MemberRecord memberRecord) {
        if (this.entriesSet.remove(memberRecord)) {
            this.entriesList.remove(memberRecord);
        }
    }

    @Override
    protected
    void add_entry (final @NotNull MemberRecord memberRecord) {
        this.remove_entry(memberRecord);
        this.entriesSet.add(memberRecord);
        this.entriesList.add(memberRecord);
    }

    @Override
    protected
    void syncRun () {
        if (this.isInterrupted()) {
            return;
        }
        this.entriesSet.clear();
        if (this.isInterrupted()) {
            return;
        }
        this.entriesList.clear();

        Map<String, AttributeValue> lastEvaluatedKey = emptyMap();
        do {
            final ScanRequest.Builder scanRequestBuilder = ScanRequest.builder()
                                                                      .tableName(DdbTable.MEMBER.name())
                                                                      .consistentRead(true);
            if (!lastEvaluatedKey.isEmpty()) {
                scanRequestBuilder.exclusiveStartKey(lastEvaluatedKey);
            }

            if (this.isInterrupted()) {
                return;
            }
            final ScanResponse scanResponse = this.dynamoDbClient.scan(scanRequestBuilder.build());
            for (final Map<String, AttributeValue> entry : scanResponse.items()) {
                if (this.isInterrupted()) {
                    return;
                }
                this.add_entry(new MemberRecord(UUID.fromString(entry.get(MemberTableParameter.ID.jsonFieldName())
                                                                     .s()), Member.convertDdbMap(entry), UUID.fromString(entry.get(MemberTableParameter.FAMILY_ID.jsonFieldName())
                                                                                                                              .s())));
            }

            lastEvaluatedKey = scanResponse.lastEvaluatedKey();

        } while (!lastEvaluatedKey.isEmpty());

        if (this.isInterrupted()) {
            return;
        }
        this.entriesList.sort(LAST_NAME_COMPARATOR);
    }
}
