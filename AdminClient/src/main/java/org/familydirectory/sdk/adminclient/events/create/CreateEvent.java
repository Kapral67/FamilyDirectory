package org.familydirectory.sdk.adminclient.events.create;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.UUID;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.sdk.adminclient.enums.create.CreateOptions;
import org.familydirectory.sdk.adminclient.events.model.EventHelper;
import org.familydirectory.sdk.adminclient.events.model.MemberRecord;
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

    private @NotNull
    TransactWriteItemsRequest buildCreateTransaction (final @NotNull MemberRecord memberRecord, final @Nullable Map<String, AttributeValue> family) {
        final List<TransactWriteItem> transactionItems = new ArrayList<>();
        final String familyId;

        switch (this.createOption) {
            case ROOT -> {
                familyId = memberRecord.id()
                                       .toString();
                transactionItems.add(TransactWriteItem.builder()
                                                      .put(Put.builder()
                                                              .tableName(DdbTable.FAMILY.name())
                                                              .item(Map.of(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(memberRecord.id()
                                                                                                                                                     .toString()),
                                                                           FamilyTableParameter.ANCESTOR.jsonFieldName(), AttributeValue.fromS(familyId)))
                                                              .build())
                                                      .build());
            }
            case SPOUSE -> {
                familyId = ofNullable(requireNonNull(family).get(FamilyTableParameter.ID.jsonFieldName())).map(AttributeValue::s)
                                                                                                          .filter(Predicate.not(String::isBlank))
                                                                                                          .orElseThrow();
                transactionItems.add(TransactWriteItem.builder()
                                                      .update(Update.builder()
                                                                    .tableName(DdbTable.FAMILY.name())
                                                                    .key(singletonMap(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(familyId)))
                                                                    .updateExpression("SET %s = :spouseKey".formatted(FamilyTableParameter.SPOUSE.jsonFieldName()))
                                                                    .expressionAttributeValues(singletonMap(":spouseKey", AttributeValue.fromS(memberRecord.id()
                                                                                                                                                           .toString())))
                                                                    .build())
                                                      .build());
            }
            case DESCENDANT -> {
                familyId = ofNullable(requireNonNull(family).get(FamilyTableParameter.ID.jsonFieldName())).map(AttributeValue::s)
                                                                                                          .filter(Predicate.not(String::isBlank))
                                                                                                          .orElseThrow();
                final String descendantsUpdateExpression = (ofNullable(family.get(FamilyTableParameter.DESCENDANTS.jsonFieldName())).map(AttributeValue::ss)
                                                                                                                                    .filter(Predicate.not(List::isEmpty))
                                                                                                                                    .isEmpty())
                        ? "SET %s = :descendants".formatted(FamilyTableParameter.DESCENDANTS.jsonFieldName())
                        : "SET %s = list_append(%s, :descendants)".formatted(FamilyTableParameter.DESCENDANTS.jsonFieldName(), FamilyTableParameter.DESCENDANTS.jsonFieldName());
                transactionItems.add(TransactWriteItem.builder()
                                                      .update(Update.builder()
                                                                    .tableName(DdbTable.FAMILY.name())
                                                                    .key(singletonMap(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(familyId)))
                                                                    .updateExpression(descendantsUpdateExpression)
                                                                    .expressionAttributeValues(singletonMap(":descendants", AttributeValue.fromSs(singletonList(memberRecord.id()
                                                                                                                                                                            .toString()))))
                                                                    .build())
                                                      .build());
                transactionItems.add(TransactWriteItem.builder()
                                                      .put(Put.builder()
                                                              .tableName(DdbTable.FAMILY.name())
                                                              .item(Map.of(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(memberRecord.id()
                                                                                                                                                     .toString()),
                                                                           FamilyTableParameter.ANCESTOR.jsonFieldName(), AttributeValue.fromS(familyId)))
                                                              .build())
                                                      .build());
            }
            default -> throw new IllegalStateException("Unhandled CreateOption: %s".formatted(this.createOption.name()));
        }

        transactionItems.add(TransactWriteItem.builder()
                                              .put(Put.builder()
                                                      .tableName(DdbTable.MEMBER.name())
                                                      .item(this.buildMember(memberRecord, familyId))
                                                      .build())
                                              .build());
        return TransactWriteItemsRequest.builder()
                                        .transactItems(transactionItems)
                                        .build();
    }

    @Override
    public @NotNull
    Scanner scanner () {
        return this.scanner;
    }

    @Override
    public
    void execute () {
        final Map<String, AttributeValue> family;
        final MemberRecord memberRecord;
        switch (this.createOption) {
            case ROOT -> {
                if (nonNull(this.getDdbItem(ROOT_ID, DdbTable.MEMBER))) {
                    throw new IllegalStateException("ROOT Member Already Exists");
                }
                System.out.println("ROOT Creation Event is for Creating the Oldest Native Member in the Family Directory.");
                memberRecord = this.buildMemberRecord(UUID.fromString(ROOT_ID));
                family = null;
            }
            case SPOUSE -> {
                System.out.println("SPOUSE Creation Events are for Creating Non-Native Members.");
                System.out.println("To Create a SPOUSE for an Existing Member, Please Provide the Existing Member's FAMILY_ID (see MEMBER Table):");
                final UUID familyId = UUID.fromString(this.scanner()
                                                          .nextLine()
                                                          .trim());
                family = this.getDdbItem(familyId.toString(), DdbTable.FAMILY);
                if (isNull(family)) {
                    throw new NoSuchElementException("FAMILY_ID `%s` Does Not Exist".formatted(familyId.toString()));
                } else if (ofNullable(family.get(FamilyTableParameter.SPOUSE.jsonFieldName())).map(AttributeValue::s)
                                                                                              .filter(Predicate.not(String::isBlank))
                                                                                              .isPresent())
                {
                    throw new IllegalStateException("SPOUSE already exists for FAMILY_ID `%s`".formatted(familyId.toString()));
                }
                memberRecord = this.buildMemberRecord(UUID.randomUUID());
            }
            case DESCENDANT -> {
                System.out.println("DESCENDANT Creation Events are for Creating Native Members.");
                System.out.println("To Create a DESCENDANT, Please Provide any Parent's FAMILY_ID (see MEMBER Table):");
                final UUID familyId = UUID.fromString(this.scanner()
                                                          .nextLine()
                                                          .trim());
                family = this.getDdbItem(familyId.toString(), DdbTable.FAMILY);
                if (isNull(family)) {
                    throw new NoSuchElementException("FAMILY_ID `%s` Does Not Exist".formatted(familyId.toString()));
                }
                memberRecord = this.buildMemberRecord(UUID.randomUUID());
            }
            default -> throw new IllegalStateException("Unhandled CreateOption: %s".formatted(this.createOption.name()));
        }

        this.validateMemberEmailIsUnique(memberRecord.member()
                                                     .getEmail());

        this.dynamoDbClient.transactWriteItems(this.buildCreateTransaction(memberRecord, family));
    }
}
