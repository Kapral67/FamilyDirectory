package org.familydirectory.assets.lambda.function.api.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.URL;
import java.time.Duration;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static java.lang.System.getenv;

public final
class GetPdfHelper extends ApiHelper {
    private static final long SIGNATURE_DURATION_MINUTES = 10;
    private final @NotNull S3Presigner s3Presigner = S3Presigner.create();

    public
    GetPdfHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) {
        super(logger, requestEvent);
    }

    public @NotNull
    URL getPresignedPdfUrl () {
        final Caller caller = this.getCaller();
        this.logger.log("<MEMBER,`%s`> GET PDF".formatted(caller.caller().id().toString()), INFO);
        final GetObjectRequest pdfRequest = GetObjectRequest.builder()
                                                            .bucket(getenv(LambdaUtils.EnvVar.S3_PDF_BUCKET_NAME.name()))
                                                            .key(this.getPdfS3Key(this.getRootMemberSurname()))
                                                            .build();
        final GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                                                                              .signatureDuration(Duration.ofMinutes(SIGNATURE_DURATION_MINUTES))
                                                                              .getObjectRequest(pdfRequest)
                                                                              .build();
        return this.s3Presigner.presignGetObject(presignRequest).url();
    }

    @Override
    public
    void close () {
        super.close();
        this.s3Presigner.close();
    }
}
