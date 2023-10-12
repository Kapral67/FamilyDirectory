package org.familydirectory.assets.lambda.function.api.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final
class DeleteHelper extends ApiHelper {
    @Override
    public @NotNull
    LambdaLogger getLogger () {
        return null;
    }

    @Override
    public @NotNull
    APIGatewayProxyRequestEvent getRequestEvent () {
        return null;
    }

    @Override
    public @NotNull
    DynamoDbClient getDynamoDbClient () {
        return null;
    }
}
