package org.familydirectory.sdk.adminclient.utility.pickers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.events.model.MemberEventHelper;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.familydirectory.sdk.adminclient.utility.pickers.model.PickerModel;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final
class SpousePicker extends PickerModel {
    @NotNull
    private final Set<MemberRecord> entriesSet;

    public
    SpousePicker () {
        super();
        this.entriesSet = new HashSet<>();
    }

    @Override
    protected
    boolean is_empty () {
        if (this.entriesList.size() != this.entriesSet.size()) {
            throw new IllegalStateException("SpousePicker Entries Have Incongruent Size");
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
        if (memberRecord.id()
                        .equals(memberRecord.familyId()) && ofNullable(requireNonNull(MemberEventHelper.getDdbItem(memberRecord.id()
                                                                                                                               .toString(), DdbTable.FAMILY)).get(
                FamilyTableParameter.SPOUSE.jsonFieldName())).map(AttributeValue::s)
                                                             .filter(Predicate.not(String::isBlank))
                                                             .isEmpty())
        {
            this.precheck_add_entry(memberRecord);
            this.entriesList.sort(FIRST_NAME_COMPARATOR);
        }
    }

    @Override
    protected
    void syncRun () {
        this.entriesSet.clear();

        Map<String, AttributeValue> lastEvaluatedKey = emptyMap();
        do {
            final ScanRequest.Builder scanRequestBuilder = ScanRequest.builder()
                                                                      .tableName(DdbTable.FAMILY.name())
                                                                      .consistentRead(true);
            if (!lastEvaluatedKey.isEmpty()) {
                scanRequestBuilder.exclusiveStartKey(lastEvaluatedKey);
            }

            final ScanResponse scanResponse = SdkClientProvider.getSdkClientProvider()
                                                               .getSdkClient(DynamoDbClient.class)
                                                               .scan(scanRequestBuilder.build());
            for (final Map<String, AttributeValue> entry : scanResponse.items()) {
                if (ofNullable(entry.get(FamilyTableParameter.SPOUSE.jsonFieldName())).map(AttributeValue::s)
                                                                                      .filter(Predicate.not(String::isBlank))
                                                                                      .isEmpty())
                {
                    final UUID id = UUID.fromString(entry.get(FamilyTableParameter.ID.jsonFieldName())
                                                         .s());
                    this.precheck_add_entry(new MemberRecord(id, Member.convertDdbMap(requireNonNull(MemberEventHelper.getDdbItem(id.toString(), DdbTable.MEMBER))), id));
                }
            }

            lastEvaluatedKey = scanResponse.lastEvaluatedKey();

        } while (!lastEvaluatedKey.isEmpty());
    }

    private
    void precheck_add_entry (final @NotNull MemberRecord memberRecord) {
        this.entriesSet.add(memberRecord);
        this.entriesList.add(memberRecord);
    }
}
