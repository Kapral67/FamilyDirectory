package org.familydirectory.assets.lambda.function;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.ListHostedZonesResponse;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
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
        final String hostedZoneName;
        try (final Route53Client route53Client = Route53Client.create()) {
            hostedZoneName = ofNullable(route53Client.listHostedZones()).filter(ListHostedZonesResponse::hasHostedZones)
                                                                        .map(ListHostedZonesResponse::hostedZones)
                                                                        .map(l -> l.get(0))
                                                                        .map(HostedZone::name)
                                                                        .orElseThrow();
        }
        try (final SesV2Client sesV2Client = SesV2Client.create()) {
            return sesV2Client.sendEmail(SendEmailRequest.builder()
                                                         .destination(Destination.builder()
                                                                                 .toAddresses(addresses)
                                                                                 .build())
                                                         .content(EmailContent.builder()
                                                                              .simple(message)
                                                                              .build())
                                                         .fromEmailAddress("no-reply@%s".formatted(hostedZoneName))
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
