package org.familydirectory.sdk.adminclient.events.create;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.enums.create.CreateOptions;
import org.familydirectory.sdk.adminclient.events.model.MemberEventHelper;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.familydirectory.sdk.adminclient.utility.pickers.MemberPicker;
import org.familydirectory.sdk.adminclient.utility.pickers.SpousePicker;
import org.familydirectory.sdk.adminclient.utility.pickers.model.PickerModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.Update;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final
class CreateEvent implements MemberEventHelper {
    private final @NotNull WindowBasedTextGUI gui;
    private final @NotNull CreateOptions createOption;
    private final @NotNull MemberPicker memberPicker;
    private final @NotNull SpousePicker spousePicker;

    public
    CreateEvent (final @NotNull WindowBasedTextGUI gui, final @NotNull CreateOptions createOption, final @NotNull MemberPicker memberPicker, final @NotNull SpousePicker spousePicker) {
        super();
        this.gui = requireNonNull(gui);
        this.createOption = requireNonNull(createOption);
        this.memberPicker = requireNonNull(memberPicker);
        this.spousePicker = requireNonNull(spousePicker);
    }

    @Override
    @NotNull
    public
    WindowBasedTextGUI getGui () {
        return this.gui;
    }

    @Override
    public @NotNull
    PickerModel getPicker () {
        return this.createOption.equals(CreateOptions.SPOUSE)
                ? this.spousePicker
                : this.memberPicker;
    }

    @Override
    public
    void run () {
        final UUID id;
        final MemberRecord memberRecord;
        switch (this.createOption) {
            case ROOT -> {
                if (nonNull(MemberEventHelper.getDdbItem(ROOT_ID, DdbTable.MEMBER))) {
                    throw new IllegalStateException("ROOT Member Already Exists");
                }
                id = UUID.fromString(ROOT_ID);
                memberRecord = this.buildMemberRecord(id, id);
            }
            case SPOUSE -> {
                if (isNull(MemberEventHelper.getDdbItem(ROOT_ID, DdbTable.MEMBER))) {
                    throw new IllegalStateException("ROOT Member Must Exist");
                }
                final MemberRecord nativeSpouse = this.getExistingMember(this.createOption.name(), "Please Select the Existing Member:", "Retrieving Members from AWS, Please Wait");
                id = nativeSpouse.familyId();
                final Map<String, AttributeValue> family = requireNonNull(MemberEventHelper.getDdbItem(id.toString(), DdbTable.FAMILY));
                if (ofNullable(family.get(FamilyTableParameter.SPOUSE.jsonFieldName())).map(AttributeValue::s)
                                                                                       .filter(Predicate.not(String::isBlank))
                                                                                       .isPresent())
                {
                    throw new IllegalStateException("SPOUSE already exists");
                }
                memberRecord = this.buildMemberRecord(UUID.randomUUID(), id);
                this.spousePicker.removeEntry(nativeSpouse);
            }
            case DESCENDANT -> {
                if (isNull(MemberEventHelper.getDdbItem(ROOT_ID, DdbTable.MEMBER))) {
                    throw new IllegalStateException("ROOT Member Must Exist");
                }
                final MemberRecord parent = this.getExistingMember(this.createOption.name(), "Please Select a Parent:", "Retrieving Parents From AWS, Please Wait");
                id = parent.familyId();
                final UUID descendantId = UUID.randomUUID();
                memberRecord = this.buildMemberRecord(descendantId, descendantId);
            }
            default -> throw new IllegalStateException("Unhandled CreateOption: %s".formatted(this.createOption.name()));
        }

        MemberEventHelper.validateMemberEmailIsUnique(memberRecord.member()
                                                                  .getEmail());

        SdkClientProvider.getSdkClientProvider()
                         .getSdkClient(DynamoDbClient.class)
                         .transactWriteItems(this.buildCreateTransaction(memberRecord, id));

        this.memberPicker.addEntry(memberRecord);
        if (!this.createOption.equals(CreateOptions.SPOUSE)) {
            this.spousePicker.addEntry(memberRecord);
        }
    }

    private @NotNull
    TransactWriteItemsRequest buildCreateTransaction (final @NotNull MemberRecord memberRecord, final @Nullable UUID ancestorId) {
        final List<TransactWriteItem> transactionItems = new ArrayList<>();

        switch (this.createOption) {
            case ROOT -> transactionItems.add(TransactWriteItem.builder()
                                                               .put(Put.builder()
                                                                       .tableName(DdbTable.FAMILY.name())
                                                                       .item(Map.of(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(memberRecord.id()
                                                                                                                                                              .toString()),
                                                                                    FamilyTableParameter.ANCESTOR.jsonFieldName(), AttributeValue.fromS(memberRecord.familyId()
                                                                                                                                                                    .toString())))
                                                                       .build())
                                                               .build());
            case SPOUSE -> transactionItems.add(TransactWriteItem.builder()
                                                                 .update(Update.builder()
                                                                               .tableName(DdbTable.FAMILY.name())
                                                                               .key(singletonMap(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(memberRecord.familyId()
                                                                                                                                                                           .toString())))
                                                                               .updateExpression("SET %s = :spouseKey".formatted(FamilyTableParameter.SPOUSE.jsonFieldName()))
                                                                               .expressionAttributeValues(singletonMap(":spouseKey", AttributeValue.fromS(memberRecord.id()
                                                                                                                                                                      .toString())))
                                                                               .build())
                                                                 .build());
            case DESCENDANT -> {
                final Map<String, AttributeValue> ancestorMap = requireNonNull(MemberEventHelper.getDdbItem(requireNonNull(ancestorId).toString(), DdbTable.FAMILY));
                final String descendantsUpdateExpression = (ofNullable(ancestorMap.get(FamilyTableParameter.DESCENDANTS.jsonFieldName())).map(AttributeValue::ss)
                                                                                                                                         .filter(Predicate.not(List::isEmpty))
                                                                                                                                         .isEmpty())
                        ? "SET %s = :descendants".formatted(FamilyTableParameter.DESCENDANTS.jsonFieldName())
                        : "ADD %s :descendants".formatted(FamilyTableParameter.DESCENDANTS.jsonFieldName());
                transactionItems.add(TransactWriteItem.builder()
                                                      .update(Update.builder()
                                                                    .tableName(DdbTable.FAMILY.name())
                                                                    .key(singletonMap(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(ancestorId.toString())))
                                                                    .updateExpression(descendantsUpdateExpression)
                                                                    .expressionAttributeValues(singletonMap(":descendants", AttributeValue.fromSs(singletonList(memberRecord.id()
                                                                                                                                                                            .toString()))))
                                                                    .build())
                                                      .build());
                transactionItems.add(TransactWriteItem.builder()
                                                      .put(Put.builder()
                                                              .tableName(DdbTable.FAMILY.name())
                                                              .item(Map.of(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(memberRecord.familyId()
                                                                                                                                                     .toString()),
                                                                           FamilyTableParameter.ANCESTOR.jsonFieldName(), AttributeValue.fromS(ancestorId.toString())))
                                                              .build())
                                                      .build());
            }
            default -> throw new IllegalStateException("Unhandled CreateOption: %s".formatted(this.createOption.name()));
        }

        transactionItems.add(TransactWriteItem.builder()
                                              .put(Put.builder()
                                                      .tableName(DdbTable.MEMBER.name())
                                                      .item(Member.retrieveDdbMap(memberRecord))
                                                      .build())
                                              .build());
        return TransactWriteItemsRequest.builder()
                                        .transactItems(transactionItems)
                                        .build();
    }
}
