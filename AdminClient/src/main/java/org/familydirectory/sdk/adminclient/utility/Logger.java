package org.familydirectory.sdk.adminclient.utility;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import io.leego.banana.Ansi;
import io.leego.banana.BananaUtils;
import io.leego.banana.Font;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public final
class Logger implements LambdaLogger {
    private static final Set<LogLevel> ERROR_LEVELS = Set.of(LogLevel.FATAL, LogLevel.ERROR, LogLevel.WARN);

    public
    Logger () {
        super();
    }

    public static
    void custom (final @Nullable String message, final Ansi... styles) {
        custom(LogLevel.UNDEFINED, message, styles);
    }

    public static
    void custom (final @NotNull LogLevel logLevel, final @Nullable String message, final Ansi... styles)
    {
        final StringBuilder sb = new StringBuilder();
        final PrintStream printStream = ERROR_LEVELS.contains(logLevel)
                ? System.err
                : System.out;
        if (!logLevel.equals(LogLevel.UNDEFINED)) {
            sb.append("[%s] ".formatted(logLevel.name()));
        }
        if (nonNull(message)) {
            sb.append("%s".formatted(message));
        }
        if (!sb.isEmpty()) {
            printStream.println(BananaUtils.bananansi(sb.toString(), Font.TERM, styles));
        }
    }

    @Override
    public
    void log (final String message) {
        custom(LogLevel.UNDEFINED, message);
    }

    @Override
    public
    void log (final byte[] message) {
        custom(LogLevel.UNDEFINED, Arrays.toString(message));
    }

    @Override
    public
    void log (final @Nullable String message, final @Nullable LogLevel logLevel) {
        if (isNull(message)) {
            return;
        }
        if (isNull(logLevel)) {
            this.log(message);
            return;
        }
        switch (logLevel) {
            case FATAL -> fatal(message);
            case ERROR -> error(message);
            case WARN -> warn(message);
            case INFO -> info(message);
            case DEBUG -> debug(message);
            case TRACE -> trace(message);
            default -> this.log(message);
        }
    }

    public static
    void fatal (final @Nullable String fatal) {
        custom(LogLevel.FATAL, fatal, Ansi.BLACK, Ansi.BG_RED);
    }

    public static
    void error (final @Nullable String error) {
        custom(LogLevel.ERROR, error, Ansi.RED);
    }

    public static
    void warn (final @Nullable String warn) {
        custom(LogLevel.WARN, warn, Ansi.YELLOW);
    }

    public static
    void info (final @Nullable String info) {
        custom(LogLevel.INFO, info, Ansi.CYAN);
    }

    public static
    void debug (final @Nullable String debug) {
        custom(LogLevel.DEBUG, debug, Ansi.GREEN);
    }

    public static
    void trace (final @Nullable String trace) {
        custom(LogLevel.TRACE, trace, Ansi.FAINT);
    }

    @Override
    public
    void log (final byte[] message, final @Nullable LogLevel logLevel) {
        this.log(Arrays.toString(message), logLevel);
    }
}
