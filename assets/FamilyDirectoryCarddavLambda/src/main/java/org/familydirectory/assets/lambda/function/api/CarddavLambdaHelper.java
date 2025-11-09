package org.familydirectory.assets.lambda.function.api;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.uuid.impl.UUIDUtil;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.enums.sync.SyncTableParameter;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.lambda.function.api.carddav.resource.FDResourceFactory;
import org.familydirectory.assets.lambda.function.api.helper.ApiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import static com.fasterxml.uuid.UUIDType.TIME_BASED_EPOCH;
import static io.milton.http.ResponseStatus.SC_BAD_REQUEST;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.familydirectory.assets.lambda.function.api.carddav.utils.CarddavConstants.INITIAL_RESOURCE_CONTAINER_SIZE;

public final
class CarddavLambdaHelper extends ApiHelper {
    private FDResourceFactory resourceFactory = null;
    @NotNull
    private final Set<MemberRecord> memberRecords = new HashSet<>(INITIAL_RESOURCE_CONTAINER_SIZE);

    CarddavLambdaHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) throws ApiHelper.ResponseException {
        super(logger, requestEvent);
        if (!Boolean.TRUE.equals(requestEvent.getIsBase64Encoded())) {
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_BAD_REQUEST));
        }
        new FDResourceFactory(this);
    }

    @NotNull
    @UnmodifiableView
    public
    Set<MemberRecord> scanMemberDdb () {
        if (!this.memberRecords.isEmpty()) {
            return Collections.unmodifiableSet(this.memberRecords);
        }

        Map<String, AttributeValue> lastEvaluatedKey = emptyMap();
        do {
            final ScanRequest.Builder scanRequestBuilder = ScanRequest.builder().tableName(DdbTable.MEMBER.name());

            if (!lastEvaluatedKey.isEmpty()) {
                scanRequestBuilder.exclusiveStartKey(lastEvaluatedKey);
            }

            final ScanResponse scanResponse = this.getDynamoDbClient().scan(scanRequestBuilder.build());

            scanResponse.items()
                        .stream()
                        .map(MemberRecord::convertDdbMap)
                        .forEach(this.memberRecords::add);

            lastEvaluatedKey = scanResponse.lastEvaluatedKey();

        } while (!lastEvaluatedKey.isEmpty());

        return Collections.unmodifiableSet(this.memberRecords);
    }

    @NotNull
    @UnmodifiableView
    public
    Set<UUID> traverseSyncDdb (UUID fromToken) {
        final var changedMemberIds = new HashSet<UUID>();
        Optional<UUID> nextToken = Optional.of(fromToken);
        while (nextToken.isPresent()) {
            final var tokenAttrMap = Optional.ofNullable(this.getDdbItem(fromToken.toString(), DdbTable.SYNC))
                                             .orElseThrow();
            nextToken = Optional.ofNullable(tokenAttrMap.get(SyncTableParameter.NEXT.toString()))
                                .map(AttributeValue::s)
                                .filter(Predicate.not(String::isBlank))
                                .map(UUID::fromString);
            changedMemberIds.addAll(Optional.ofNullable(tokenAttrMap.get(SyncTableParameter.MEMBERS.toString()))
                                            .map(AttributeValue::ss)
                                            .stream()
                                            .flatMap(List::stream)
                                            .map(UUID::fromString)
                                            .toList());
        }
        return Collections.unmodifiableSet(changedMemberIds);
    }

    public void registerResourceFactory(@NotNull FDResourceFactory resourceFactory) {
        if (this.resourceFactory != null) {
            throw new IllegalStateException("Resource factory already registered");
        }
        this.resourceFactory = requireNonNull(resourceFactory);
    }

    public
    FDResourceFactory getResourceFactory () {
        return requireNonNull(this.resourceFactory);
    }
}
