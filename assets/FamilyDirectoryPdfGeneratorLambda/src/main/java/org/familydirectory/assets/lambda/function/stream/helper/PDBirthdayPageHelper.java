package org.familydirectory.assets.lambda.function.stream.helper;

import java.io.IOException;
import java.time.LocalDate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.familydirectory.assets.lambda.function.stream.helper.models.PDPageHelperModel;
import org.jetbrains.annotations.NotNull;

final
class PDBirthdayPageHelper extends PDPageHelperModel {

    PDBirthdayPageHelper (final @NotNull PDDocument pdf, final @NotNull PDPage page, final @NotNull String title, final @NotNull LocalDate subtitle, final int pageNumber) throws IOException {
        super(pdf, page, title, subtitle, pageNumber);
        this.maxColumns = 3;
    }
}
