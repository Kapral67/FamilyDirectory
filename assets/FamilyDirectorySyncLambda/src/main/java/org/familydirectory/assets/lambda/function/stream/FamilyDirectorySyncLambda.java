package org.familydirectory.assets.lambda.function.stream;

import com.amazonaws.services.lambda.runtime.Context;
//import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
//import com.fasterxml.uuid.Generators;
//import com.fasterxml.uuid.impl.UUIDUtil;
//import java.time.Clock;
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
//import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;

public
class FamilyDirectorySyncLambda implements RequestHandler<DynamodbEvent, Void> {

    @Override
    public
    Void handleRequest (final DynamodbEvent dynamodbEvent, final @NotNull Context context) {
//        final LambdaLogger logger = context.getLogger();
        try {
//            final Instant newUUIDInstant = Instant.ofEpochMilli(UUIDUtil.extractTimestamp(Generators.timeBasedEpochRandomGenerator().generate()));
//            if (newUUIDInstant.isBefore(Instant.now(Clock.systemUTC()).minus(1, ChronoUnit.YEARS))) {
//                // invalid sync_token
//            }
            return null;
        } catch (final Throwable e) {
//            LambdaUtils.logTrace(logger, e, FATAL);
            throw new RuntimeException(e);
        }
    }
}
