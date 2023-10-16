package org.familydirectory.assets.lambda.function;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.DEBUG;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.TRACE;
import static java.util.Optional.ofNullable;

public final
class LambdaUtils {

    private
    LambdaUtils () {
    }

    public static
    void logTrace (final @NotNull LambdaLogger logger, final @NotNull Throwable e, final @NotNull LogLevel logLevel) {
        logger.log(e.getMessage(), logLevel);
        ofNullable(e.getCause()).ifPresent(throwable -> logger.log(throwable.getMessage(), DEBUG));
        logger.log(Arrays.toString(e.getStackTrace()), TRACE);
    }
}
