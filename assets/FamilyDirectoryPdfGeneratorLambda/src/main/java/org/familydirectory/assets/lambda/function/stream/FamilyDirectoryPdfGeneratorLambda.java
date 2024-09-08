package org.familydirectory.assets.lambda.function.stream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.familydirectory.assets.lambda.function.stream.IO.IgnoredCloseOutputStream;
import org.familydirectory.assets.lambda.function.stream.IO.S3ByteArrayOutputStream;
import org.familydirectory.assets.lambda.function.stream.helper.PdfHelper;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.DEBUG;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;
import static java.lang.System.getenv;
import static java.util.Objects.requireNonNull;

public
class FamilyDirectoryPdfGeneratorLambda implements RequestHandler<DynamodbEvent, Void> {

    @Override
    public
    Void handleRequest (final DynamodbEvent dynamodbEvent, final @NotNull Context context) {
        final LambdaLogger logger = context.getLogger();
        try {
            logger.log(Objects.toString(dynamodbEvent), DEBUG);
            final String pdfKey;
            final RequestBody pdfRequestBody;
            try (final PdfHelper pdfHelper = new PdfHelper(logger)) {
                final String rootMemberSurname = pdfHelper.getRootMemberSurname();
                pdfKey = pdfHelper.getPdfS3Key(rootMemberSurname);
                pdfRequestBody = zipPdfBundle(pdfHelper, rootMemberSurname);
            }

            final PutObjectRequest pdfPutRequest = PutObjectRequest.builder()
                                                                   .bucket(getenv(LambdaUtils.EnvVar.S3_PDF_BUCKET_NAME.name()))
                                                                   .key(pdfKey)
                                                                   .contentType("application/zip")
                                                                   .build();
            try (final S3Client s3Client = S3Client.create()) {
                s3Client.putObject(pdfPutRequest, pdfRequestBody);
            }

            return null;

        } catch (final Throwable e) {
            LambdaUtils.logTrace(logger, e, FATAL);
            throw new RuntimeException(e);
        }
    }

    private static @NotNull
    RequestBody zipPdfBundle (final @NotNull PdfHelper pdfHelper, final @NotNull String rootMemberSurname) throws IOException {
        try (final var bos = new S3ByteArrayOutputStream(); final var zos = new ZipOutputStream(bos); final var ios = new IgnoredCloseOutputStream(zos)) {
            final ZipEntry familyDirectoryZip = new ZipEntry("%sFamilyDirectory.pdf".formatted(requireNonNull(rootMemberSurname)));
            zos.putNextEntry(familyDirectoryZip);
            pdfHelper.saveDirectoryPdf(ios);
            zos.closeEntry();

            final ZipEntry birthdayZip = new ZipEntry("%sFamilyBirthdays.pdf".formatted(rootMemberSurname));
            zos.putNextEntry(birthdayZip);
            pdfHelper.saveBirthdayPdf(ios);
            zos.closeEntry();

            return bos.requestBody();
        }
    }
}
