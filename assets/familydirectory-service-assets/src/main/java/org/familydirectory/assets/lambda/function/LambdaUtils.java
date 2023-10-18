package org.familydirectory.assets.lambda.function;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.GetEmailIdentityRequest;
import software.amazon.awssdk.services.sesv2.model.GetEmailIdentityResponse;
import software.amazon.awssdk.services.sesv2.model.IdentityInfo;
import software.amazon.awssdk.services.sesv2.model.ListEmailIdentitiesRequest;
import software.amazon.awssdk.services.sesv2.model.ListEmailIdentitiesResponse;
import software.amazon.awssdk.services.sesv2.model.MailFromAttributes;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.DEBUG;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.TRACE;
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
            final String identityName = ofNullable(sesClient.listEmailIdentities(ListEmailIdentitiesRequest.builder()
                                                                                                           .pageSize(1)
                                                                                                           .build())).filter(ListEmailIdentitiesResponse::hasEmailIdentities)
                                                                                                                     .map(ListEmailIdentitiesResponse::emailIdentities)
                                                                                                                     .map(l -> l.get(0))
                                                                                                                     .map(IdentityInfo::identityName)
                                                                                                                     .filter(Predicate.not(String::isBlank))
                                                                                                                     .orElseThrow();
            final String mailFromDomain = ofNullable(sesClient.getEmailIdentity(GetEmailIdentityRequest.builder()
                                                                                                       .emailIdentity(identityName)
                                                                                                       .build())).map(GetEmailIdentityResponse::mailFromAttributes)
                                                                                                                 .map(MailFromAttributes::mailFromDomain)
                                                                                                                 .filter(Predicate.not(String::isBlank))
                                                                                                                 .orElseThrow();
            return sesClient.sendEmail(SendEmailRequest.builder()
                                                       .destination(Destination.builder()
                                                                               .toAddresses(addresses)
                                                                               .build())
                                                       .content(EmailContent.builder()
                                                                            .simple(message)
                                                                            .build())
                                                       .fromEmailAddress("no-reply@%s".formatted(mailFromDomain))
                                                       .build())
                            .messageId();
        }
    }

    public static
    void logTrace (final @NotNull LambdaLogger logger, final @NotNull Throwable e, final @NotNull LogLevel logLevel) {
        logger.log(e.getMessage(), logLevel);
        ofNullable(e.getCause()).ifPresent(throwable -> logger.log(throwable.getMessage(), DEBUG));
        logger.log(Arrays.toString(e.getStackTrace()), TRACE);
    }
}
