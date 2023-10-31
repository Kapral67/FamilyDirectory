package org.familydirectory.assets.lambda.function.pdf;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import org.familydirectory.assets.lambda.function.pdf.helper.PdfHelper;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import static java.lang.System.getenv;

public
class FamilyDirectoryPdfGeneratorLambda implements RequestHandler<DynamodbEvent, Void> {
    private static final S3Client S3_CLIENT = S3Client.create();

    @Override
    public
    Void handleRequest (final DynamodbEvent dynamodbEvent, final @NotNull Context context) {
        try {
            final String pdfKey;
            final RequestBody pdfRequestBody;
            try (final PdfHelper pdfHelper = new PdfHelper(context.getLogger())) {
                pdfKey = pdfHelper.getPdfS3Key();
                pdfRequestBody = pdfHelper.getPdf();
            } catch (final Throwable e) {
                context.getLogger()
                       .log("FATAL: Issue Exists in PdfHelper/PDPageHelper", LogLevel.FATAL);
                throw new RuntimeException(e);
            }

            final PutObjectRequest pdfPutRequest = PutObjectRequest.builder()
                                                                   .bucket(getenv(LambdaUtils.EnvVar.S3_PDF_BUCKET_NAME.name()))
                                                                   .key(pdfKey)
                                                                   .contentType("application/pdf")
                                                                   .build();

            S3_CLIENT.putObject(pdfPutRequest, pdfRequestBody);

            return null;

        } catch (final Throwable e) {
            LambdaUtils.logTrace(context.getLogger(), e, LogLevel.FATAL);
            throw e;
        }
    }
}
