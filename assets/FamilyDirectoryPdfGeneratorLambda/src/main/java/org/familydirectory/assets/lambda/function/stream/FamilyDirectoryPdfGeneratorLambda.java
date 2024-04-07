package org.familydirectory.assets.lambda.function.stream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.familydirectory.assets.lambda.function.stream.helper.PdfHelper;
import org.familydirectory.assets.lambda.function.utility.LambdaUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import static com.amazonaws.services.lambda.runtime.logging.LogLevel.FATAL;
import static java.lang.System.getenv;
import static java.util.Objects.requireNonNull;

public
class FamilyDirectoryPdfGeneratorLambda implements RequestHandler<DynamodbEvent, Void> {

    @Override
    public
    Void handleRequest (final DynamodbEvent dynamodbEvent, final @NotNull Context context) {
        try {
            final String pdfKey;
            final RequestBody pdfRequestBody;
            try (final PdfHelper pdfHelper = new PdfHelper(context.getLogger())) {
                final String rootMemberSurname = pdfHelper.getRootMemberSurname();
                pdfKey = pdfHelper.getPdfS3Key(rootMemberSurname);
                pdfRequestBody = zipPdfBundle(pdfHelper, rootMemberSurname);
            } catch (final IOException e) {
                throw new RuntimeException(e);
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
            LambdaUtils.logTrace(context.getLogger(), e, FATAL);
            throw e;
        }
    }

    private static @NotNull
    RequestBody zipPdfBundle (final @NotNull PdfHelper pdfHelper, final @NotNull String rootMemberSurname) throws IOException {
        final PdfHelper.PdfBundle pdfBundle = requireNonNull(pdfHelper).getPdfBundle();
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(); final ZipOutputStream zos = new ZipOutputStream(bos)) {
            final ZipEntry familyDirectoryZip = new ZipEntry("%sFamilyDirectory.pdf".formatted(requireNonNull(rootMemberSurname)));
            zos.putNextEntry(familyDirectoryZip);
            zos.write(pdfBundle.familyDirectoryPdf());
            zos.closeEntry();

            final ZipEntry birthdayZip = new ZipEntry("%sFamilyBirthdays.pdf".formatted(rootMemberSurname));
            zos.putNextEntry(birthdayZip);
            zos.write(pdfBundle.birthdayPdf());
            zos.closeEntry();

            zos.close();
            return RequestBody.fromBytes(bos.toByteArray());
        }
    }
}
