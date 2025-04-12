package org.familydirectory.sdk.adminclient.events.backfill;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.events.model.MemberEventHelper;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.familydirectory.sdk.adminclient.utility.pickers.MemberPicker;
import org.familydirectory.sdk.adminclient.utility.pickers.model.PickerModel;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;
import static java.util.Objects.requireNonNull;

public final
class BackfillEvent implements MemberEventHelper {
    private final @NotNull WindowBasedTextGUI windowBasedTextGUI;
    private final @NotNull MemberPicker memberPicker;

    public
    BackfillEvent (final @NotNull WindowBasedTextGUI windowBasedTextGUI, final @NotNull MemberPicker memberPicker) {
        super();
        this.windowBasedTextGUI = requireNonNull(windowBasedTextGUI);
        this.memberPicker = requireNonNull(memberPicker);
    }

    @Override
    public @NotNull
    WindowBasedTextGUI getGui () {
        return this.windowBasedTextGUI;
    }

    @Override
    public @NotNull
    PickerModel getPicker () {
        return this.memberPicker;
    }

    @Override
    public
    void run () {
        if (this.memberPicker.isEmpty()) {
            throw new IllegalStateException("No Members Exist");
        }
        final DynamoDbClient dbClient = SdkClientProvider.getSdkClientProvider()
                                                         .getSdkClient(DynamoDbClient.class);

        final List<MemberRecord> entries = this.memberPicker.getEntries();
        final Deque<List<WriteRequest>> pending = new ArrayDeque<>();
        List<WriteRequest> writeRequests = new ArrayList<>();
        for (int i = 0; i < entries.size(); ++i) {
            if (i > 0 && i % 25 == 0) {
                pending.push(Collections.unmodifiableList(writeRequests));
                writeRequests = new ArrayList<>();
            }
            entries.get(i).member().setLastModifiedNow();
            writeRequests.add(WriteRequest.builder()
                                          .putRequest(PutRequest.builder()
                                                                .item(Member.retrieveDdbMap(entries.get(i)))
                                                                .build())
                                          .build());
        }
        pending.push(Collections.unmodifiableList(writeRequests));

        while(!pending.isEmpty()) {
            dbClient.batchWriteItem(BatchWriteItemRequest.builder()
                                                         .requestItems(Map.of(DdbTable.MEMBER.name(), pending.pop()))
                                                         .build())
                    .unprocessedItems()
                    .values()
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(Predicate.not(Collection::isEmpty))
                    .forEach(pending::push);
        }
    }
}
