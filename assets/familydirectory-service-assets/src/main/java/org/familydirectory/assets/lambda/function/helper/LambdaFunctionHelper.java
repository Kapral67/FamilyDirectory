package org.familydirectory.assets.lambda.function.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.util.Map;
import org.familydirectory.assets.ddb.enums.DdbTable;
import org.familydirectory.assets.ddb.models.DdbTableParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import static java.util.Collections.singletonMap;

public
interface LambdaFunctionHelper {
    @NotNull
    LambdaLogger getLogger ();

    @Nullable
    default
    Map<String, AttributeValue> getDdbItem (final @NotNull String primaryKey, final @NotNull DdbTable ddbTable) {
        final GetItemRequest request = GetItemRequest.builder()
                                                     .tableName(ddbTable.name())
                                                     .key(singletonMap(DdbTableParameter.PK.getName(), AttributeValue.fromS(primaryKey)))
                                                     .build();
        final GetItemResponse response = this.getDynamoDbClient()
                                             .getItem(request);
        return (response.hasItem())
                ? response.item()
                : null;
    }

    @NotNull
    DynamoDbClient getDynamoDbClient ();
}
