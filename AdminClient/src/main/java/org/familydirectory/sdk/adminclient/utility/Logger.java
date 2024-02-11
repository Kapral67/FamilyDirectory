package org.familydirectory.sdk.adminclient.utility;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import io.leego.banana.Ansi;
import io.leego.banana.BananaUtils;
import io.leego.banana.Font;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
        customLine(LogLevel.UNDEFINED, message, styles);
    }

    public static
    void customLine (final @NotNull LogLevel logLevel, final @Nullable String message, final Ansi... styles)
    {
        final StringBuilder sb = new StringBuilder();
        if (!logLevel.equals(LogLevel.UNDEFINED)) {
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
        custom(LogLevel.UNDEFINED, prepended, message, styles);
    }

    public static
    void custom (final @NotNull LogLevel logLevel, final @NotNull String prepended, final @Nullable String message, final Ansi... styles)
    {
        final StringBuilder sb = new StringBuilder();
        if (!logLevel.equals(LogLevel.UNDEFINED)) {
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
        customLine(LogLevel.UNDEFINED, message);
    }

    @Override
    public
    void log (final byte[] message) {
        customLine(LogLevel.UNDEFINED, Arrays.toString(message));
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
        customLine(LogLevel.FATAL, fatal, Ansi.BLACK, Ansi.BG_RED);
    }

    public static
    void error (final @Nullable String error) {
        customLine(LogLevel.ERROR, error, Ansi.RED);
    }

    public static
    void warn (final @Nullable String warn) {
        customLine(LogLevel.WARN, warn, Ansi.YELLOW);
    }

    public static
    void info (final @Nullable String info) {
        customLine(LogLevel.INFO, info, Ansi.CYAN);
    }

    public static
    void debug (final @Nullable String debug) {
        customLine(LogLevel.DEBUG, debug, Ansi.PURPLE);
    }

    public static
    void trace (final @Nullable String trace) {
        customLine(LogLevel.TRACE, trace, Ansi.BG_BLACK, Ansi.WHITE);
    }

    @Override
    public
    void log (final byte[] message, final @Nullable LogLevel logLevel) {
        this.log(Arrays.toString(message), logLevel);
    }
}
