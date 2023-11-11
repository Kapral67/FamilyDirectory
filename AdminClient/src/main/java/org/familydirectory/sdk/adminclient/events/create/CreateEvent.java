package org.familydirectory.sdk.adminclient.events.create;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.sdk.adminclient.enums.create.CreateOptions;
import org.familydirectory.sdk.adminclient.events.model.EventHelper;
import org.familydirectory.sdk.adminclient.events.model.MemberRecord;
import org.familydirectory.sdk.adminclient.utility.Logger;
import org.familydirectory.sdk.adminclient.utility.MemberPicker;
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
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final
class CreateEvent implements EventHelper {
    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final @NotNull CreateOptions createOption;
    private final @NotNull Scanner scanner;

    public
    CreateEvent (final @NotNull Scanner scanner, final @NotNull CreateOptions createOption) {
        super();
        this.scanner = requireNonNull(scanner);
        this.createOption = createOption;
    }

    @Override
    public @NotNull
    DynamoDbClient getDynamoDbClient () {
        return this.dynamoDbClient;
    }

    @Override
    public @NotNull
    Scanner scanner () {
        return this.scanner;
    }

    @Override
    public
    void execute () {
        final UUID id;
        final MemberRecord memberRecord;
        switch (this.createOption) {
            case ROOT -> {
                if (nonNull(this.getDdbItem(ROOT_ID, DdbTable.MEMBER))) {
                    throw new IllegalStateException("ROOT Member Already Exists");
                }
                Logger.info("ROOT Creation Event is for Creating the Oldest Native Member in the Family Directory.");
                id = UUID.fromString(ROOT_ID);
                memberRecord = this.buildMemberRecord(id, id);
            }
            case SPOUSE -> {
                if (MemberPicker.getEntries()
                                .isEmpty())
                {
                    throw new IllegalStateException("ROOT Member Must Exist");
                }
                Logger.info("SPOUSE Creation Events are for Creating Non-Native Members.");
                final MemberRecord nativeSpouse = this.getExistingMember("To Create a SPOUSE for an Existing Member, Please Select the Existing Member:");
                id = nativeSpouse.familyId();
                final Map<String, AttributeValue> family = requireNonNull(this.getDdbItem(id.toString(), DdbTable.FAMILY));
                if (ofNullable(family.get(FamilyTableParameter.SPOUSE.jsonFieldName())).map(AttributeValue::s)
                                                                                       .filter(Predicate.not(String::isBlank))
                                                                                       .isPresent())
                {
                    throw new IllegalStateException("SPOUSE already exists");
                }
                memberRecord = this.buildMemberRecord(UUID.randomUUID(), id);
            }
            case DESCENDANT -> {
                if (MemberPicker.getEntries()
                                .isEmpty())
                {
                    throw new IllegalStateException("ROOT Member Must Exist");
                }
                Logger.info("DESCENDANT Creation Events are for Creating Native Members.");
                final MemberRecord parent = this.getExistingMember("To Create a DESCENDANT, Please Select any Parent of this DESCENDANT:");
                id = parent.familyId();
                final UUID descendantId = UUID.randomUUID();
                memberRecord = this.buildMemberRecord(descendantId, descendantId);
            }
            default -> throw new IllegalStateException("Unhandled CreateOption: %s".formatted(this.createOption.name()));
        }

        this.validateMemberEmailIsUnique(memberRecord.member()
                                                     .getEmail());

        this.dynamoDbClient.transactWriteItems(this.buildCreateTransaction(memberRecord, id));

        MemberPicker.addEntry(memberRecord);
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
                final Map<String, AttributeValue> ancestorMap = requireNonNull(this.getDdbItem(requireNonNull(ancestorId).toString(), DdbTable.FAMILY));
                final String descendantsUpdateExpression = (ofNullable(ancestorMap.get(FamilyTableParameter.DESCENDANTS.jsonFieldName())).map(AttributeValue::ss)
                                                                                                                                         .filter(Predicate.not(List::isEmpty))
                                                                                                                                         .isEmpty())
                        ? "SET %s = :descendants".formatted(FamilyTableParameter.DESCENDANTS.jsonFieldName())
                        : "SET %s = list_append(%s, :descendants)".formatted(FamilyTableParameter.DESCENDANTS.jsonFieldName(), FamilyTableParameter.DESCENDANTS.jsonFieldName());
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
                                                      .item(EventHelper.buildMember(memberRecord))
                                                      .build())
                                              .build());
        return TransactWriteItemsRequest.builder()
                                        .transactItems(transactionItems)
                                        .build();
    }
}
