package org.familydirectory.sdk.adminclient.events.flag;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.AdminClientTui;
import org.familydirectory.sdk.adminclient.enums.flags.Flags;
import org.familydirectory.sdk.adminclient.events.model.EventHelper;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.familydirectory.sdk.adminclient.utility.lanterna.WaitingDialog;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final
class FlagEvent implements EventHelper {
    private final @NotNull WindowBasedTextGUI gui;
    private final @NotNull Flags flag;

    public
    FlagEvent (final @NotNull WindowBasedTextGUI gui, final @NotNull Flags flag) {
        super();
        this.gui = requireNonNull(gui);
        this.flag = requireNonNull(flag);
    }

    @Override
    public
    void run () {
        switch (this.flag) {
            case ISSUE_871 -> {
                final WaitingDialog waitDialog = WaitingDialog.createDialog(this.flag.name(), "Applying Database Migration, Please Wait");
                waitDialog.setHints(AdminClientTui.EXTRA_WINDOW_HINTS);
                waitDialog.showDialog(this.gui, false);
                final CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
                    try {
                        final SdkClientProvider sdkClientProvider = SdkClientProvider.getSdkClientProvider();
                        Map<String, AttributeValue> lastEvaluatedKey = emptyMap();
                        do {
                            final ScanRequest.Builder scanRequestBuilder = ScanRequest.builder()
                                                                                      .tableName(DdbTable.MEMBER.name())
                                                                                      .consistentRead(true);
                            if (!lastEvaluatedKey.isEmpty()) {
                                scanRequestBuilder.exclusiveStartKey(lastEvaluatedKey);
                            }

                            final ScanResponse scanResponse = sdkClientProvider.getSdkClient(DynamoDbClient.class)
                                                                               .scan(scanRequestBuilder.build());
                            for (final Map<String, AttributeValue> entry : scanResponse.items()) {
                                final UUID id = UUID.fromString(entry.get(MemberTableParameter.ID.jsonFieldName())
                                                                     .s());

                                ofNullable(entry.get(MemberTableParameter.ADDRESS.jsonFieldName())).filter(av -> isNull(av.l()) || av.l()
                                                                                                                                     .isEmpty())
                                                                                                   .map(AttributeValue::ss)
                                                                                                   .ifPresent(ss -> {
                                                                                                       Map<String, AttributeValue> newEntry = new HashMap<>(entry);

                                                                                                       newEntry.remove(MemberTableParameter.ADDRESS.jsonFieldName());
                                                                                                       if (!ss.isEmpty()) {
                                                                                                           newEntry.put(MemberTableParameter.ADDRESS.jsonFieldName(),
                                                                                                                        AttributeValue.fromL(Member.revertAddressDdb(ss)));
                                                                                                       }

                                                                                                       // lazy validation
                                                                                                       final MemberRecord memberRecord = new MemberRecord(id, Member.convertDdbMap(newEntry),
                                                                                                                                                          UUID.fromString(entry.get(
                                                                                                                                                                                       MemberTableParameter.FAMILY_ID.jsonFieldName())
                                                                                                                                                                               .s()));
                                                                                                       newEntry = Member.retrieveDdbMap(memberRecord);

                                                                                                       sdkClientProvider.getSdkClient(DynamoDbClient.class)
                                                                                                                        .deleteItem(DeleteItemRequest.builder()
                                                                                                                                                     .tableName(DdbTable.MEMBER.name())
                                                                                                                                                     .key(singletonMap(
                                                                                                                                                             MemberTableParameter.ID.jsonFieldName(),
                                                                                                                                                             AttributeValue.fromS(id.toString())))
                                                                                                                                                     .build());
                                                                                                       sdkClientProvider.getSdkClient(DynamoDbClient.class)
                                                                                                                        .putItem(PutItemRequest.builder()
                                                                                                                                               .tableName(DdbTable.MEMBER.name())
                                                                                                                                               .item(newEntry)
                                                                                                                                               .build());
                                                                                                   });
                            }

                            lastEvaluatedKey = scanResponse.lastEvaluatedKey();

                        } while (!lastEvaluatedKey.isEmpty());
                    } finally {
                        waitDialog.close();
                    }
                });
                waitDialog.waitUntilClosed();
                try {
                    future.get();
                } catch (final ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (final InterruptedException e) {
                    Thread.currentThread()
                          .interrupt();
                    throw new RuntimeException(e);
                }
            }
            default -> throw new IllegalStateException("Unhandled Flag: %s".formatted(this.flag.name()));
        }
    }
}
