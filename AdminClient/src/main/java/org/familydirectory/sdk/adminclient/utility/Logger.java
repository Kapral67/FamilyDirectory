package org.familydirectory.sdk.adminclient.utility;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import io.leego.banana.Ansi;
import io.leego.banana.BananaUtils;
import io.leego.banana.Font;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.DEBUG;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.ERROR;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.TRACE;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.UNDEFINED;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.WARN;
import static io.leego.banana.Ansi.BG_BLACK;
import static io.leego.banana.Ansi.BG_RED;
import static io.leego.banana.Ansi.BLACK;
import static io.leego.banana.Ansi.CYAN;
import static io.leego.banana.Ansi.PURPLE;
import static io.leego.banana.Ansi.RED;
import static io.leego.banana.Ansi.WHITE;
import static io.leego.banana.Ansi.YELLOW;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public final
class Logger implements LambdaLogger {
    public
    Logger () {
        super();
    }

    public static
    void customLine (final @Nullable String message, final Ansi... styles) {
        customLine(UNDEFINED, message, styles);
    }

    public static
    void customLine (final @NotNull LogLevel logLevel, final @Nullable String message, final Ansi... styles)
    {
        final StringBuilder sb = new StringBuilder();
        if (!logLevel.equals(UNDEFINED)) {
            sb.append("[%s] ".formatted(logLevel.name()));
        }
        if (nonNull(message)) {
            sb.append("%s".formatted(message));
        }
        if (!sb.isEmpty()) {
            System.out.println(BananaUtils.bananansi(sb.toString(), Font.TERM, styles));
        }
    }

    public static
    void custom (final @NotNull String prepended, final @Nullable String message, final Ansi... styles) {
        custom(UNDEFINED, prepended, message, styles);
    }

    public static
    void custom (final @NotNull LogLevel logLevel, final @NotNull String prepended, final @Nullable String message, final Ansi... styles)
    {
        final StringBuilder sb = new StringBuilder();
        if (!logLevel.equals(UNDEFINED)) {
            sb.append("[%s] ".formatted(logLevel.name()));
        }
        if (nonNull(message)) {
            sb.append("%s".formatted(message));
        }
        if (!sb.isEmpty()) {
            System.out.printf("%s%s", prepended, BananaUtils.bananansi(sb.toString(), Font.TERM, styles));
        }
    }

    @Override
    public
    void log (final String message) {
        customLine(UNDEFINED, message);
    }

    @Override
    public
    void log (final byte[] message) {
        customLine(UNDEFINED, Arrays.toString(message));
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
        customLine(FATAL, fatal, BLACK, BG_RED);
    }

    public static
    void error (final @Nullable String error) {
        customLine(ERROR, error, RED);
    }

    public static
    void warn (final @Nullable String warn) {
        customLine(WARN, warn, YELLOW);
    }

    public static
    void info (final @Nullable String info) {
        customLine(INFO, info, CYAN);
    }

    public static
    void debug (final @Nullable String debug) {
        customLine(DEBUG, debug, PURPLE);
    }

    public static
    void trace (final @Nullable String trace) {
        customLine(TRACE, trace, BG_BLACK, WHITE);
    }

    @Override
    public
    void log (final byte[] message, final @Nullable LogLevel logLevel) {
        this.log(Arrays.toString(message), logLevel);
    }
}
