package org.familydirectory.assets.lambda.function.stream.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.uuid.impl.UUIDUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.familydirectory.assets.lambda.function.helper.LambdaFunctionHelper;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import static java.util.Objects.requireNonNull;

public final
class SyncHelper implements LambdaFunctionHelper {

    public static final String LATEST = UUIDUtil.nilUUID().toString();

    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final @NotNull LambdaLogger logger;

    public
    SyncHelper (final @NotNull LambdaLogger logger) {
        super();
        this.logger = requireNonNull(logger);
    }

    @Override
    public @NotNull
    LambdaLogger getLogger () {
        return this.logger;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    @Override
    public @NotNull
    DynamoDbClient getDynamoDbClient () {
        return this.dynamoDbClient;
    }
}
