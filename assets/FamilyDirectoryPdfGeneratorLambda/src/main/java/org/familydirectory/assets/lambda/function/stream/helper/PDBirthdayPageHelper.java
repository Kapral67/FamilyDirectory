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

final
class PDBirthdayPageHelper extends PDPageHelperModel {
    private static final DateTimeFormatter BIRTHDAY_FORMATTER = DateTimeFormatter.ofPattern("d, yyyy");
    private static final DateTimeFormatter DEATHDAY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

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
            this.contents.setFont(TITLE_FONT, this.getColumnFittedFontSize(line, TITLE_FONT, STANDARD_FONT_SIZE));
            this.addColumnAgnosticText(line);
            this.newLine(STANDARD_LINE_SPACING);
        }
        final String birthdayLine = getBirthdayLine(memberRecord);
        this.contents.setFont(STANDARD_FONT, this.getColumnFittedFontSize(birthdayLine, STANDARD_FONT, STANDARD_FONT_SIZE));
        this.addColumnAgnosticText(birthdayLine);
        this.newLine(STANDARD_LINE_SPACING);
    }

    @NotNull
    private static
    String getBirthdayLine (final @NotNull MemberRecord memberRecord) {
        final LocalDate birthday = memberRecord.member()
                                               .getBirthday();
        final StringBuilder birthdayLineStringBuilder = new StringBuilder(TAB + birthday.format(BIRTHDAY_FORMATTER) + TAB);
        if (birthday.getDayOfMonth() < 10) {
            birthdayLineStringBuilder.append(TAB);
        }
        birthdayLineStringBuilder.append(memberRecord.member()
                                                     .getDisplayName());
        final LocalDate deathday = memberRecord.member()
                                               .getDeathday();
        if (nonNull(deathday)) {
            birthdayLineStringBuilder.append(" %c%s".formatted(MemberModel.DAGGER, deathday.format(DEATHDAY_FORMATTER)));
        }
        return birthdayLineStringBuilder.toString();
    }
}
