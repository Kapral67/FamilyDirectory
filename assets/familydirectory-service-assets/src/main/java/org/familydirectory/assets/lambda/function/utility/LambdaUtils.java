package org.familydirectory.assets.lambda.function.utility;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.DEBUG;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.TRACE;
import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;

public
enum LambdaUtils {
    ;

    public static @NotNull
    String sendEmail (final @NotNull List<String> addresses, final @NotNull String subject, final @NotNull String body) {
        final Message message = Message.builder()
                                       .subject(Content.builder()
                                                       .data(subject)
                                                       .build())
                                       .body(Body.builder()
                                                 .text(Content.builder()
                                                              .data(body)
                                                              .build())
                                                 .build())
                                       .build();
        try (final SesV2Client sesClient = SesV2Client.create()) {
            return sesClient.sendEmail(SendEmailRequest.builder()
                                                       .destination(Destination.builder()
                                                                               .toAddresses(addresses)
                                                                               .build())
                                                       .content(EmailContent.builder()
                                                                            .simple(message)
                                                                            .build())
                                                       .fromEmailAddress("no-reply@%s".formatted(getenv(EnvVar.SES_MAIL_FROM_DOMAIN.name())))
                                                       .build())
                            .messageId();
        }
    }

    public static
    void logTrace (final @NotNull LambdaLogger logger, final @NotNull Throwable e, final @NotNull LogLevel logLevel) {
        logger.log(e.getMessage(), logLevel);
        ofNullable(e.getCause()).ifPresent(t -> logger.log(t.getMessage(), DEBUG));
        ofNullable(e.getSuppressed()).ifPresent(suppressed -> Arrays.stream(suppressed)
                                                                    .forEach(t -> logger.log(t.getMessage(), DEBUG)));
        logger.log(Arrays.toString(e.getStackTrace()), TRACE);
    }

    public
    enum EnvVar {
        COGNITO_USER_POOL_ID,
        ROOT_ID,
        SES_EMAIL_IDENTITY_NAME,
        SES_MAIL_FROM_DOMAIN
    }
}
