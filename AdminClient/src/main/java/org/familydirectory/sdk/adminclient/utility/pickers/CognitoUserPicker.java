package org.familydirectory.sdk.adminclient.utility.pickers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Predicate;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.cognito.CognitoTableParameter;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.utility.SdkClientProvider;
import org.familydirectory.sdk.adminclient.utility.pickers.model.PickerModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final
class CognitoUserPicker implements PickerModel {
    @NotNull
    private final Map<MemberRecord, String> entriesMap;
    @NotNull
    private final List<MemberRecord> entriesList;
    @NotNull
    private final DynamoDbClient dynamoDbClient;

    public
    CognitoUserPicker () {
        super();
        this.entriesMap = new HashMap<>();
        this.entriesList = new ArrayList<>();
        this.dynamoDbClient = SdkClientProvider.getSdkClientProvider()
                                               .getSdkClient(DynamoDbClient.class);
    }

    @NotNull
    public
    String getCognitoSub (final @NotNull MemberRecord memberRecord) throws NoSuchElementException {
        return ofNullable(this.entriesMap.get(requireNonNull(memberRecord))).orElseThrow();
    }

    @Override
    public
    void run () {
        this.entriesMap.clear();
        this.entriesList.clear();
        Map<String, AttributeValue> lastEvaluatedKey = emptyMap();
        do {
            final ScanRequest.Builder scanRequestBuilder = ScanRequest.builder()
                                                                      .tableName(DdbTable.COGNITO.name())
                                                                      .consistentRead(true);
            if (!lastEvaluatedKey.isEmpty()) {
                scanRequestBuilder.exclusiveStartKey(lastEvaluatedKey);
            }
            final ScanResponse scanResponse = this.dynamoDbClient.scan(scanRequestBuilder.build());

            for (final Map<String, AttributeValue> cognitoEntry : scanResponse.items()) {
                final String sub = requireNonNull(cognitoEntry.get(CognitoTableParameter.ID.jsonFieldName())
                                                              .s());
                final String memberId = requireNonNull(cognitoEntry.get(CognitoTableParameter.MEMBER.jsonFieldName())
                                                                   .s());
                final Map<String, AttributeValue> ddbMember = this.dynamoDbClient.getItem(GetItemRequest.builder()
                                                                                                        .tableName(DdbTable.MEMBER.name())
                                                                                                        .key(singletonMap(MemberTableParameter.ID.jsonFieldName(), AttributeValue.fromS(memberId)))
                                                                                                        .build())
                                                                                 .item();
                this.addEntry(new MemberRecord(UUID.fromString(memberId), Member.convertDdbMap(ddbMember), UUID.fromString(ddbMember.get(MemberTableParameter.FAMILY_ID.jsonFieldName())
                                                                                                                                    .s())));
            }

            lastEvaluatedKey = scanResponse.lastEvaluatedKey();
        } while (!lastEvaluatedKey.isEmpty());
        this.entriesList.sort(LAST_NAME_COMPARATOR);
    }

    @Override
    public
    boolean isEmpty () {
        if (this.entriesList.size() != this.entriesMap.size()) {
            throw new IllegalStateException("CognitoUserPicker Entries Have Incongruent Size");
        }
        return this.entriesList.isEmpty();
    }

    @Override
    public
    void removeEntry (final @NotNull MemberRecord memberRecord) {
        if (nonNull(this.entriesMap.remove(memberRecord))) {
            this.entriesList.remove(memberRecord);
        }
    }

    @Override
    public
    void addEntry (final @NotNull MemberRecord memberRecord) {
        this.removeEntry(memberRecord);
        final String sub = this.getCognitoSubFromMemberRecord(memberRecord);
        if (nonNull(sub)) {
            this.entriesMap.put(memberRecord, sub);
            this.entriesList.add(memberRecord);
        }
    }

    @Nullable
    private
    String getCognitoSubFromMemberRecord (final @NotNull MemberRecord memberRecord) {
        final QueryRequest cognitoMemberQueryRequest = QueryRequest.builder()
                                                                   .tableName(DdbTable.COGNITO.name())
                                                                   .indexName(requireNonNull(CognitoTableParameter.MEMBER.gsiProps()).getIndexName())
                                                                   .keyConditionExpression("#memberId = :memberId")
                                                                   .expressionAttributeNames(singletonMap("#memberId", CognitoTableParameter.MEMBER.gsiProps()
                                                                                                                                                   .getPartitionKey()
                                                                                                                                                   .getName()))
                                                                   .expressionAttributeValues(singletonMap(":memberId", AttributeValue.fromS(memberRecord.id()
                                                                                                                                                         .toString())))
                                                                   .limit(1)
                                                                   .build();
        final QueryResponse cognitoMemberQueryResponse = this.dynamoDbClient.query(cognitoMemberQueryRequest);
        if (!cognitoMemberQueryResponse.items()
                                       .isEmpty())
        {
            return ofNullable(cognitoMemberQueryResponse.items()
                                                        .getFirst()
                                                        .get(CognitoTableParameter.ID.jsonFieldName())).map(AttributeValue::s)
                                                                                                       .filter(Predicate.not(String::isBlank))
                                                                                                       .orElseThrow();
        } else {
            return null;
        }
    }

    @Override
    @Contract(pure = true)
    @NotNull
    @UnmodifiableView
    public
    List<MemberRecord> getEntries () {
        return Collections.unmodifiableList(this.entriesList);
    }
}
