package org.familydirectory.assets.lambda.function.api.helper;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URL;
import java.time.Duration;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.INFO;
import static java.lang.System.getenv;
import static java.util.Objects.requireNonNull;

public final
class GetPdfHelper extends ApiHelper {
    private static final long SIGNATURE_DURATION_MINUTES = 10;
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final @NotNull S3Presigner s3Presigner = S3Presigner.create();
    private final @NotNull LambdaLogger logger;
    private final @NotNull APIGatewayProxyRequestEvent requestEvent;

    public
    GetPdfHelper (final @NotNull LambdaLogger logger, final @NotNull APIGatewayProxyRequestEvent requestEvent) {
        super();
        this.logger = requireNonNull(logger);
        this.requestEvent = requireNonNull(requestEvent);
    }

    public @NotNull
    URL getPresignedPdfUrl () {
        final Caller caller = this.getCaller();
        this.logger.log("<MEMBER,`%s`> GET PDF".formatted(caller.memberId()), INFO);
        final GetObjectRequest pdfRequest = GetObjectRequest.builder()
                                                            .bucket(getenv(LambdaUtils.EnvVar.S3_PDF_BUCKET_NAME.name()))
                                                            .key(this.getPdfS3Key())
                                                            .build();
        final GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                                                                              .signatureDuration(Duration.ofMinutes(SIGNATURE_DURATION_MINUTES))
                                                                              .getObjectRequest(pdfRequest)
                                                                              .build();
        return this.s3Presigner.presignGetObject(presignRequest)
                               .url();
    }

    @Override
    public @NotNull
    APIGatewayProxyRequestEvent getRequestEvent () {
        return this.requestEvent;
    }

    @Override
    public @NotNull
    LambdaLogger getLogger () {
        return this.logger;
    }

    @Override
    public @NotNull
    DynamoDbClient getDynamoDbClient () {
        return this.dynamoDbClient;
    }
}
