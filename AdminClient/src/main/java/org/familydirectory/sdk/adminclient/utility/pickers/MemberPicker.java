package org.familydirectory.sdk.adminclient.utility.pickers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.familydirectory.sdk.adminclient.utility.pickers.model.PickerModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import static java.util.Collections.emptyMap;

public final
class MemberPicker implements PickerModel {
    @NotNull
    private final Set<MemberRecord> entriesSet;

    @NotNull
    private final List<MemberRecord> entriesList;

    public
    MemberPicker () {
        super();
        this.entriesSet = new HashSet<>();
        this.entriesList = new ArrayList<>();
    }

    @Override
    public
    boolean isEmpty () {
        if (this.entriesList.size() != this.entriesSet.size()) {
            throw new IllegalStateException("MemberPicker Entries Have Incongruent Size");
        }
        return this.entriesList.isEmpty();
    }

    @Override
    public
    void removeEntry (final @NotNull MemberRecord memberRecord) {
        if (this.entriesSet.remove(memberRecord)) {
            this.entriesList.remove(memberRecord);
        }
    }

    @Override
    public
    void addEntry (final @NotNull MemberRecord memberRecord) {
        this.removeEntry(memberRecord);
        this.entriesSet.add(memberRecord);
        this.entriesList.add(memberRecord);
    }

    @Override
    @Contract(pure = true)
    @NotNull
    @UnmodifiableView
    public
    List<MemberRecord> getEntries () {
        return Collections.unmodifiableList(this.entriesList);
    }

    @Override
    public
    void run () {
        this.entriesSet.clear();
        this.entriesList.clear();
        try (final DynamoDbClient dbClient = SdkClientProvider.getSdkClientProvider()
                                                              .getSdkClient(DynamoDbClient.class))
        {
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
                    this.addEntry(new MemberRecord(UUID.fromString(entry.get(MemberTableParameter.ID.jsonFieldName())
                                                                        .s()), Member.convertDdbMap(entry), UUID.fromString(entry.get(MemberTableParameter.FAMILY_ID.jsonFieldName())
                                                                                                                                 .s())));
                }

                lastEvaluatedKey = scanResponse.lastEvaluatedKey();
            } while (!lastEvaluatedKey.isEmpty());
        }
        this.entriesList.sort(LAST_NAME_COMPARATOR);
    }
}
