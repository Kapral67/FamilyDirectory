package org.familydirectory.sdk.adminclient.events.delete;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.enums.family.FamilyTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.events.model.EventHelper;
import org.familydirectory.sdk.adminclient.utility.pickers.MemberPicker;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.Update;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final
class DeleteEvent implements EventHelper {
    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final @NotNull CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.create();
    private final @NotNull Scanner scanner;

    public
    DeleteEvent (final @NotNull Scanner scanner) {
        super();
        this.scanner = requireNonNull(scanner);
    }

    @Override
    public @NotNull
    Scanner scanner () {
        return this.scanner;
    }

    @Override
    public
    void execute () {
        if (MemberPicker.isEmpty()) {
            throw new IllegalStateException("No Members Exist to Delete");
        }
        final MemberRecord memberRecord = this.getExistingMember("Please Select Existing Member to DELETE:");

        final List<TransactWriteItem> transactionItems;
        if (memberRecord.id()
                        .equals(memberRecord.familyId()))
        {
            // NATIVE
            final Map<String, AttributeValue> familyMap = requireNonNull(this.getDdbItem(memberRecord.familyId()
                                                                                                     .toString(), DdbTable.FAMILY));
            ofNullable(familyMap.get(FamilyTableParameter.SPOUSE.jsonFieldName())).map(AttributeValue::s)
                                                                                  .filter(Predicate.not(String::isBlank))
                                                                                  .ifPresent(spouse -> {
                                                                                      throw new IllegalStateException(
                                                                                              "Member Cannot Be Deleted Because Member's Family has a SPOUSE: %s".formatted(spouse));
                                                                                  });
            ofNullable(familyMap.get(FamilyTableParameter.DESCENDANTS.jsonFieldName())).map(AttributeValue::ss)
                                                                                       .filter(Predicate.not(List::isEmpty))
                                                                                       .ifPresent(descendants -> {
                                                                                           throw new IllegalStateException(
                                                                                                   "Member Cannot Be Deleted Because Member's Family has DESCENDANTS: %s".formatted(descendants));
                                                                                       });
            final Delete deleteFamily = Delete.builder()
                                              .tableName(DdbTable.FAMILY.name())
                                              .key(singletonMap(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(memberRecord.familyId()
                                                                                                                                          .toString())))
                                              .build();
            final Delete deleteMember = Delete.builder()
                                              .tableName(DdbTable.MEMBER.name())
                                              .key(singletonMap(MemberTableParameter.ID.jsonFieldName(), AttributeValue.fromS(memberRecord.id()
                                                                                                                                          .toString())))
                                              .build();

            final String ancestorId = ofNullable(familyMap.get(FamilyTableParameter.ANCESTOR.jsonFieldName())).map(AttributeValue::s)
                                                                                                              .filter(Predicate.not(String::isBlank))
                                                                                                              .orElseThrow();
            final List<String> ancestorFamilyDescendantsList = ofNullable(this.getDdbItem(ancestorId, DdbTable.FAMILY)).map(m -> m.get(FamilyTableParameter.DESCENDANTS.jsonFieldName()))
                                                                                                                       .map(AttributeValue::ss)
                                                                                                                       .filter(Predicate.not(List::isEmpty))
                                                                                                                       .filter(l -> l.contains(memberRecord.id()
                                                                                                                                                           .toString()))
                                                                                                                       .orElseThrow();

            final Update.Builder updateAncestorFamilyBuilder = Update.builder()
                                                                     .tableName(DdbTable.FAMILY.name())
                                                                     .key(singletonMap(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(ancestorId)));
            if (ancestorFamilyDescendantsList.size() == 1) {
                updateAncestorFamilyBuilder.updateExpression("REMOVE %s".formatted(FamilyTableParameter.DESCENDANTS.jsonFieldName()));
            } else {
                updateAncestorFamilyBuilder.updateExpression("DELETE %s :descendants".formatted(FamilyTableParameter.DESCENDANTS.jsonFieldName()))
                                           .expressionAttributeValues(singletonMap(":descendants", AttributeValue.fromSs(singletonList(memberRecord.id()
                                                                                                                                                   .toString()))));
            }

            transactionItems = List.of(TransactWriteItem.builder()
                                                        .delete(deleteFamily)
                                                        .build(), TransactWriteItem.builder()
                                                                                   .delete(deleteMember)
                                                                                   .build(), TransactWriteItem.builder()
                                                                                                              .update(updateAncestorFamilyBuilder.build())
                                                                                                              .build());
        } else {
            // NATURALIZED
            final Update update = Update.builder()
                                        .tableName(DdbTable.FAMILY.name())
                                        .key(singletonMap(FamilyTableParameter.ID.jsonFieldName(), AttributeValue.fromS(memberRecord.familyId()
                                                                                                                                    .toString())))
                                        .updateExpression("REMOVE %s".formatted(FamilyTableParameter.SPOUSE.jsonFieldName()))
                                        .build();
            final Delete delete = Delete.builder()
                                        .tableName(DdbTable.MEMBER.name())
                                        .key(singletonMap(MemberTableParameter.ID.jsonFieldName(), AttributeValue.fromS(memberRecord.id()
                                                                                                                                    .toString())))
                                        .build();
            transactionItems = List.of(TransactWriteItem.builder()
                                                        .update(update)
                                                        .build(), TransactWriteItem.builder()
                                                                                   .delete(delete)
                                                                                   .build());
        }

        this.dynamoDbClient.transactWriteItems(TransactWriteItemsRequest.builder()
                                                                        .transactItems(transactionItems)
                                                                        .build());
        MemberPicker.removeEntry(memberRecord);
        this.deleteCognitoAccountAndNotify(memberRecord.id()
                                                       .toString());
    }

    private
    void deleteCognitoAccountAndNotify (final @NotNull String memberId) {
        final QueryRequest cognitoMemberQueryRequest = QueryRequest.builder()
                                                                   .tableName(DdbTable.COGNITO.name())
                                                                   .indexName(requireNonNull(CognitoTableParameter.MEMBER.gsiProps()).getIndexName())
                                                                   .keyConditionExpression("#memberId = :memberId")
                                                                   .expressionAttributeNames(singletonMap("#memberId", CognitoTableParameter.MEMBER.gsiProps()
                                                                                                                                                   .getPartitionKey()
                                                                                                                                                   .getName()))
                                                                   .expressionAttributeValues(singletonMap(":memberId", AttributeValue.fromS(memberId)))
                                                                   .limit(1)
                                                                   .build();
        final QueryResponse cognitoMemberQueryResponse = this.dynamoDbClient.query(cognitoMemberQueryRequest);
        if (!cognitoMemberQueryResponse.items()
                                       .isEmpty())
        {
            final String ddbMemberCognitoSub = ofNullable(cognitoMemberQueryResponse.items()
                                                                                    .getFirst()
                                                                                    .get(CognitoTableParameter.ID.jsonFieldName())).map(AttributeValue::s)
                                                                                                                                   .filter(Predicate.not(String::isBlank))
                                                                                                                                   .orElseThrow();
            this.deleteCognitoAccountAndNotify(this.cognitoClient, ddbMemberCognitoSub);
        }
    }

    @Override
    @NotNull
    public
    DynamoDbClient getDynamoDbClient () {
        return this.dynamoDbClient;
    }

    @Override
    public
    void close () {
        EventHelper.super.close();
        this.cognitoClient.close();
    }
}
