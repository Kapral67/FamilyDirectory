package org.familydirectory.assets.lambda.function.stream.helper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.lambda.function.stream.helper.models.PDPageHelperModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.nonNull;

final
class PDBirthdayPageHelper extends PDPageHelperModel {
    PDBirthdayPageHelper (final @NotNull PDDocument pdf, final @NotNull PDPage page, final @NotNull String title, final @NotNull LocalDate subtitle, final int pageNumber) throws IOException {
        super(pdf, page, title, subtitle, pageNumber);
    }

    @Override
    protected
    int maxColumns () {
        return 3;
    }

    void addBirthday (final @NotNull MemberRecord memberRecord, final @Nullable Month month) throws NewPageException, IOException {
        float blockSizeYOffset = STANDARD_LINE_SPACING;
        if (nonNull(month) && this.location.y < this.bodyContentStartY) {
            blockSizeYOffset += THREE_HALF_LINE_SPACING;
        }
        if (this.isNewColumnNeeded(blockSizeYOffset) && !this.nextColumn()) {
            throw new NewPageException();
        }
        if (nonNull(month)) {
            this.newLine(HALF_LINE_SPACING);
            final String line = month.getDisplayName(TextStyle.FULL, Locale.US);
            this.contents.setFont(TITLE_FONT, this.getColumnFittedFontSize(line, TITLE_FONT, TITLE_FONT_SIZE));
            this.addColumnAgnosticText(line);
            this.newLine(STANDARD_LINE_SPACING);
        }
        final LocalDate birthday = memberRecord.member()
                                               .getBirthday();
        final String line = "%s%d, %d".formatted(TAB, birthday.getDayOfMonth(), birthday.getYear());
        this.contents.setFont(STANDARD_FONT, this.getColumnFittedFontSize(line, STANDARD_FONT, STANDARD_FONT_SIZE));
        this.addColumnAgnosticText(line);
        this.newLine(STANDARD_LINE_SPACING);
    }
}
