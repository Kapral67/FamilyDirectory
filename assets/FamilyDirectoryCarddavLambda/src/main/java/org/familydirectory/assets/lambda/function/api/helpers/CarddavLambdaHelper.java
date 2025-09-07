package org.familydirectory.assets.lambda.function.api.helpers;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.lambda.function.api.helper.ApiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import static io.milton.http.ResponseStatus.SC_BAD_REQUEST;
import static java.util.Collections.emptyMap;

public final
class CarddavLambdaHelper extends ApiHelper {

    @NotNull
    private final Set<MemberRecord> memberRecords = new HashSet<>(200);

    public
    CarddavLambdaHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) throws ApiHelper.ResponseException {
        super(logger, requestEvent);
        if (!Boolean.TRUE.equals(requestEvent.getIsBase64Encoded())) {
            throw new ResponseException(new APIGatewayProxyResponseEvent().withStatusCode(SC_BAD_REQUEST));
        }
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
}
