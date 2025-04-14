package org.familydirectory.assets.lambda.function.stream.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.uuid.impl.UUIDUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.temporal.TemporalAmount;
import org.familydirectory.assets.lambda.function.helper.LambdaFunctionHelper;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import static java.lang.System.getenv;
import static java.time.Duration.ofDays;
import static java.util.Objects.requireNonNull;

public final
class SyncHelper implements LambdaFunctionHelper {
    public static final TemporalAmount SYNC_TOKEN_TTL = ofDays(Long.parseLong(requireNonNull(getenv(LambdaUtils.EnvVar.SYNC_TOKEN_DURATION_DAYS.name()))));
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
