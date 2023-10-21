package org.familydirectory.assets.lambda.function.pdf.helper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;

public final
class PdfHelper {
    private static final float PX_IN_INCH = 72.0f;
    private static final char BULLET = '•';
    private static final float TOP_BOTTOM_MARGIN = inch2px(0.75f);
    private static final float LEFT_RIGHT_MARGIN = inch2px(0.5f);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private static final float TITLE_FONT_SIZE = 14.0f;
    private static final float STANDARD_FONT_SIZE = 10.0f;
    private static final float DATE_FONT_SIZE = 8.0f;
    private static final PDFont BOLD_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDFont ITALIC_FONT = PDType1Font.HELVETICA_OBLIQUE;
    private static final PDFont STANDARD_FONT = PDType1Font.HELVETICA;
    private static final float STANDARD_LINE_SPACING = inch2px(0.2f);
    private static final float HALF_LINE_SPACING = STANDARD_LINE_SPACING * 0.5f;
    private static final float THREE_HALF_LINE_SPACING = STANDARD_LINE_SPACING * 1.5f;
    private static final float BODY_CONTENT_START_Y = 695.0f;
    @NotNull
    private final PDDocument pdf = new PDDocument();
    @NotNull
    private final String date = LocalDate.now()
                                         .format(DATE_FORMATTER);
    @NotNull
    private final String rootMemberSurname;
    private int pageNumber = 0;
    private PDPageHelper page;

    public
    PdfHelper (final @NotNull String rootMemberSurname) throws IOException {
        super();
        this.rootMemberSurname = Optional.of(rootMemberSurname)
                                         .filter(Predicate.not(String::isBlank))
                                         .orElseThrow();
        this.makeHeader();
    }

    private
    void makeHeader () throws IOException {
        this.page = new PDPageHelper(this.pdf, new PDPage());

        { // TITLE
            this.page.contents()
                     .setFont(BOLD_FONT, TITLE_FONT_SIZE);
            final String title = "%s FAMILY DIRECTORY".formatted(this.rootMemberSurname.toUpperCase());
            final float titleWidth = getTextWidth(BOLD_FONT, TITLE_FONT_SIZE, title);
            this.page.location.x = this.page.centerX() - titleWidth * 0.5f;
            this.page.location.y = this.page.height() - TOP_BOTTOM_MARGIN;
            this.page.addLine(title);
        }

        { // DATE
            this.page.contents()
                     .setFont(ITALIC_FONT, DATE_FONT_SIZE);
            final float dateWidth = getTextWidth(ITALIC_FONT, DATE_FONT_SIZE, this.date);
            this.page.location.x = this.page.centerX() - dateWidth * 0.5f;
            this.page.location.y -= STANDARD_LINE_SPACING;
            this.page.addLine(this.date);
        }

        { // TOP LINE
            this.page.location.x = LEFT_RIGHT_MARGIN;
            this.page.location.y -= HALF_LINE_SPACING;
            this.page.contents()
                     .moveTo(this.page.location.x, this.page.location.y);
            this.page.contents()
                     .lineTo(this.page.width() - LEFT_RIGHT_MARGIN, this.page.location.y);
            this.page.contents()
                     .stroke();
        }

        { // BOTTOM LINE
            this.page.location.y = TOP_BOTTOM_MARGIN;
            this.page.contents()
                     .moveTo(this.page.location.x, this.page.location.y);
            this.page.contents()
                     .lineTo(this.page.width() - LEFT_RIGHT_MARGIN, this.page.location.y);
            this.page.contents()
                     .stroke();
        }

        { // PAGE NUMBER
            this.page.contents()
                     .setFont(STANDARD_FONT, STANDARD_FONT_SIZE);
            final String pageNum = "%d".formatted(++this.pageNumber);
            final float pageNumWidth = getTextWidth(STANDARD_FONT, STANDARD_FONT_SIZE, pageNum);
            this.page.location.x = this.page.centerX() - pageNumWidth * 0.5f;
            this.page.location.y -= TOP_BOTTOM_MARGIN / 2.0f;
            this.page.addLine(pageNum);
        }

        this.page.location.x = LEFT_RIGHT_MARGIN;
        this.page.location.y = BODY_CONTENT_START_Y;
        this.page.contents()
                 .moveTo(this.page.location.x, this.page.location.y);
    }

    private static
    float getTextWidth (final @NotNull PDFont font, final float fontSize, final @NotNull String text) throws IOException {
        return font.getStringWidth(text) / 1000.0f * fontSize;
    }

    private static
    float inch2px (final float inch) {
        return inch * PX_IN_INCH;
    }

    public
    void addFamily (final @NotNull Member member, final @NotNull Member spouse) {

    }

    /**
     * TODO: DELETE THIS
     */
    public @NotNull
    PDDocument getPdf () throws IOException {
        this.page.close();
        return this.pdf;
    }
}
