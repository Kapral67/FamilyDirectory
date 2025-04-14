package org.familydirectory.assets.lambda.function.stream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.transformers.v2.DynamodbEventTransformer;
import com.fasterxml.uuid.Generators;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.member.MemberTableParameter;
import org.familydirectory.assets.ddb.enums.sync.SyncTableParameter;
import org.familydirectory.assets.ddb.utils.DdbUtils;
import org.familydirectory.assets.lambda.function.stream.helper.SyncHelper;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.Record;
import software.amazon.awssdk.services.dynamodb.model.StreamRecord;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.Update;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;
import static java.util.Collections.singletonMap;

public
class FamilyDirectorySyncLambda implements RequestHandler<DynamodbEvent, Void> {

    @Override
    public
    Void handleRequest (final DynamodbEvent dynamodbEvent, final @NotNull Context context) {
        final LambdaLogger logger = context.getLogger();
        try (final SyncHelper syncHelper = new SyncHelper(logger)) {

            final UUID thisToken = Generators.timeBasedEpochRandomGenerator().generate();

            final Set<UUID> updatedMembers = DynamodbEventTransformer.toRecordsV2(dynamodbEvent)
                                                                     .stream()
                                                                     .map(Record::dynamodb)
                                                                     .map(StreamRecord::keys)
                                                                     .map(keys -> keys.get(MemberTableParameter.ID.jsonFieldName()))
                                                                     .map(AttributeValue::s)
                                                                     .map(UUID::fromString)
                                                                     .collect(Collectors.toUnmodifiableSet());

            final UUID previousToken = Optional.ofNullable(syncHelper.getDdbItem(SyncHelper.LATEST, DdbTable.SYNC))
                                               .map(map -> map.get(SyncTableParameter.NEXT.jsonFieldName()))
                                               .map(AttributeValue::s)
                                               .map(UUID::fromString)
                                               .orElse(null);

            final List<TransactWriteItem> transactionItems = new ArrayList<>(3);

            if (previousToken == null) {
                // create latestToken next targeting thisToken
                final Map<String, AttributeValue> latestTokenMap = Map.of(
                    SyncTableParameter.ID.jsonFieldName(), AttributeValue.fromS(SyncHelper.LATEST),
                    SyncTableParameter.NEXT.jsonFieldName(), AttributeValue.fromS(thisToken.toString())
                );
                final Put latestTokenPut = Put.builder()
                                              .tableName(DdbTable.SYNC.name())
                                              .item(latestTokenMap)
                                              .build();
                transactionItems.add(TransactWriteItem.builder().put(latestTokenPut).build());
            } else {
                // update previousToken: next targeting thisToken, ttl now + x days
                final Update previousTokenUpdate = Update.builder()
                                                         .tableName(DdbTable.SYNC.name())
                                                         .key(singletonMap(SyncTableParameter.ID.jsonFieldName(), AttributeValue.fromS(previousToken.toString())))
                                                         .updateExpression("SET %s = :nextKey, %s = :ttlKey".formatted(SyncTableParameter.NEXT.jsonFieldName(), SyncTableParameter.TTL.jsonFieldName()))
                                                         .expressionAttributeValues(Map.of(
                                                             ":nextKey", AttributeValue.fromS(thisToken.toString()),
                                                             ":ttlKey", AttributeValue.fromN(String.valueOf(Instant.now(Clock.systemUTC()).plus(DdbUtils.SYNC_TOKEN_TTL).getEpochSecond()))
                                                         ))
                                                         .build();
                transactionItems.add(TransactWriteItem.builder().update(previousTokenUpdate).build());
                // update latestToken next targeting thisToken
                final Update latestTokenUpdate = Update.builder()
                                                       .tableName(DdbTable.SYNC.name())
                                                       .key(singletonMap(SyncTableParameter.ID.jsonFieldName(), AttributeValue.fromS(SyncHelper.LATEST)))
                                                       .updateExpression("SET %s = :nextKey".formatted(SyncTableParameter.NEXT.jsonFieldName()))
                                                       .expressionAttributeValues(singletonMap(":nextKey", AttributeValue.fromS(thisToken.toString())))
                                                       .build();
                transactionItems.add(TransactWriteItem.builder().update(latestTokenUpdate).build());
            }

            // create thisToken
            final Map<String, AttributeValue> thisTokenMap = Map.of(
                SyncTableParameter.ID.jsonFieldName(), AttributeValue.fromS(thisToken.toString()),
                SyncTableParameter.MEMBERS.jsonFieldName(), AttributeValue.fromSs(updatedMembers.stream().map(UUID::toString).toList())
            );
            final Put thisTokenPut = Put.builder()
                                        .tableName(DdbTable.SYNC.name())
                                        .item(thisTokenMap)
                                        .build();
            transactionItems.add(TransactWriteItem.builder().put(thisTokenPut).build());

            syncHelper.getDynamoDbClient()
                      .transactWriteItems(TransactWriteItemsRequest.builder()
                                                                   .transactItems(transactionItems)
                                                                   .build());

            return null;
        } catch (final Throwable e) {
            LambdaUtils.logTrace(logger, e, FATAL);
            throw new RuntimeException(e);
        }
    }
}
