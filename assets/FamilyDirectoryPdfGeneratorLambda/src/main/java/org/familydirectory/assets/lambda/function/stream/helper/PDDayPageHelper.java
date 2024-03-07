package org.familydirectory.assets.lambda.function.stream.helper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.familydirectory.assets.ddb.models.member.MemberModel;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.assets.lambda.function.stream.helper.models.PDPageHelperModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

final
class PDDayPageHelper extends PDPageHelperModel {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d, yyyy");

    PDDayPageHelper (final @NotNull PDDocument pdf, final @NotNull PDPage page, final @NotNull String title, final @NotNull LocalDate subtitle, final int pageNumber) throws IOException {
        super(pdf, page, title, subtitle, pageNumber);
    }

    @Override
    protected
    int maxColumns () {
        return 3;
    }

    void addDay (final @NotNull MemberRecord memberRecord, final @Nullable Month month, final @NotNull Day day) throws NewPageException, IOException {
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
            this.contents.setFont(TITLE_FONT, this.getColumnFittedFontSize(line, TITLE_FONT, STANDARD_FONT_SIZE));
            this.addColumnAgnosticText(line);
            this.newLine(STANDARD_LINE_SPACING);
        }
        final String line = getLine(memberRecord, requireNonNull(day));
        this.contents.setFont(STANDARD_FONT, this.getColumnFittedFontSize(line, STANDARD_FONT, STANDARD_FONT_SIZE));
        this.addColumnAgnosticText(line);
        this.newLine(STANDARD_LINE_SPACING);
    }

    @NotNull
    private static
    String getLine (final @NotNull MemberRecord memberRecord, final @NotNull Day day) {
        final LocalDate date = (requireNonNull(day).equals(Day.DEATH))
                ? requireNonNull(memberRecord.member()
                                             .getDeathday())
                : memberRecord.member()
                              .getBirthday();

        final StringBuilder lineBuilder = new StringBuilder(TAB + date.format(DATE_FORMATTER) + TAB);
        if (date.getDayOfMonth() < 10) {
            lineBuilder.append(TAB);
        }
        if (day.equals(Day.DEATH)) {
            lineBuilder.append(MemberModel.DAGGER);
        }
        lineBuilder.append(memberRecord.member()
                                       .getDisplayName());
        return lineBuilder.toString();
    }

    public
    enum Day {
        BIRTH,
        DEATH
    }
}
