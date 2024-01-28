package org.familydirectory.assets.lambda.function.stream.helper.models;

import java.io.Closeable;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.jetbrains.annotations.NotNull;
import static org.familydirectory.assets.ddb.models.member.MemberModel.DAGGER;

public
interface IPDPageHelper extends Closeable {
    DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    char BULLET = '•';
    String TAB = "  ";
    float PX_IN_INCH = 72.0f;
    float TOP_BOTTOM_MARGIN = inch2px(0.75f);
    float LEFT_RIGHT_MARGIN = inch2px(0.5f);
    PDFont TITLE_FONT = PDType1Font.HELVETICA_BOLD;
    float TITLE_FONT_SIZE = 14.0f;
    PDFont SUBTITLE_FONT = PDType1Font.HELVETICA_OBLIQUE;
    float SUBTITLE_FONT_SIZE = 8.0f;
    PDFont STANDARD_FONT = PDType1Font.HELVETICA;
    float STANDARD_FONT_SIZE = 10.0f;
    float STANDARD_LINE_SPACING = inch2px(0.2f);
    float HALF_LINE_SPACING = STANDARD_LINE_SPACING / 2.0f;
    float DOUBLE_LINE_SPACING = STANDARD_LINE_SPACING * 2.0f;
    float THREE_HALF_LINE_SPACING = STANDARD_LINE_SPACING * 1.5f;
    float BODY_CONTENT_END_Y = TOP_BOTTOM_MARGIN + THREE_HALF_LINE_SPACING;
    float TEXT_RISE = inch2px(1.0f / 32.0f);

    static
    float inch2px (final float inch) {
        return inch * PX_IN_INCH;
    }

    default
    float getColumnFittedFontSize (final @NotNull String line, final @NotNull PDFont font) throws IOException {
        final float fontSize = (this.columnWidth() - STANDARD_LINE_SPACING) / getTextSpaceUnits(font, line);
        if (Float.isNaN(fontSize) || Float.isInfinite(fontSize) || fontSize <= 0.0f) {
            throw new RuntimeException("text: `%s` not single-line-fittable with font: `%s`".formatted(line, font.getName()));
        }
        return Math.min(STANDARD_FONT_SIZE, fontSize);
    }

    default
    void addColumnLines () throws IOException {
        this.getLocation().x = LEFT_RIGHT_MARGIN + this.columnWidth();
        this.getLocation().y = this.getBodyContentStartY() + STANDARD_LINE_SPACING;
        for (int i = 1; i < this.getMaxColumns(); ++i, this.getLocation().x += this.columnWidth()) {
            this.getContents()
                .moveTo(this.getLocation().x, this.getLocation().y);
            this.getContents()
                .lineTo(this.getLocation().x, BODY_CONTENT_END_Y - STANDARD_LINE_SPACING);
            this.getContents()
                .stroke();
        }
        this.getContents()
            .moveTo(this.getLocation().x, this.getLocation().y);
    }

    default
    float columnWidth () {
        return (this.width() - LEFT_RIGHT_MARGIN * 2.0f) / (float) this.getMaxColumns();
    }

    default
    float width () {
        return this.getPage()
                   .getMediaBox()
                   .getWidth();
    }

    @NotNull
    PDPage getPage ();

    int getMaxColumns ();

    @NotNull
    Location getLocation ();

    @NotNull
    PDPageContentStream getContents ();

    float getBodyContentStartY ();

    default
    void addTitle (final @NotNull String title) throws IOException {
        final float titleWidth = getTextWidth(TITLE_FONT, TITLE_FONT_SIZE, title);
        this.getContents()
            .setFont(TITLE_FONT, TITLE_FONT_SIZE);
        this.getLocation().x = this.centerX() - titleWidth * 0.5f;
        this.getLocation().y = this.height() - TOP_BOTTOM_MARGIN;
        this.addColumnAgnosticText(title);
    }

    static
    float getTextWidth (final @NotNull PDFont font, final float fontSize, final @NotNull String text) throws IOException {
        return getTextSpaceUnits(font, text) * fontSize;
    }

    static
    float getTextSpaceUnits (final @NotNull PDFont font, final @NotNull String text) throws IOException {
        // https://javadoc.io/static/org.apache.pdfbox/pdfbox/2.0.30/org/apache/pdfbox/pdmodel/font/PDFont.html#getStringWidth-java.lang.String-
        return font.getStringWidth(text) / 1000.0f;
    }

    default
    float centerX () {
        return this.width() / 2.0f;
    }

    default
    float height () {
        return this.getPage()
                   .getMediaBox()
                   .getHeight();
    }

    default
    void addColumnAgnosticText (final @NotNull String line) throws IOException {
        this.getContents()
            .beginText();
        this.getContents()
            .newLineAtOffset(this.getLocation().x, this.getLocation().y);

        final String daggerStr = String.valueOf(DAGGER);
        if (line.contains(daggerStr)) {
            final String[] splitLines = line.split(daggerStr);
            for (int i = 0; i < splitLines.length; ++i) {
                if (!splitLines[i].isEmpty()) {
                    this.getContents()
                        .showText(splitLines[i]);
                }
                if (i < splitLines.length - 1) {
                    this.getContents()
                        .setTextRise(TEXT_RISE);
                    this.getContents()
                        .showText(daggerStr);
                    this.getContents()
                        .setTextRise(0.0f);
                }
            }
        } else {
            this.getContents()
                .showText(line);
        }

        this.getContents()
            .endText();
    }

    default
    void addSubtitle (final @NotNull String subtitle) throws IOException {
        final float subtitleWidth = IPDPageHelper.getTextWidth(SUBTITLE_FONT, SUBTITLE_FONT_SIZE, subtitle);
        this.getContents()
            .setFont(SUBTITLE_FONT, SUBTITLE_FONT_SIZE);
        this.getLocation().x = this.centerX() - subtitleWidth * 0.5f;
        this.getLocation().y = this.height() - TOP_BOTTOM_MARGIN - STANDARD_LINE_SPACING;
        this.addColumnAgnosticText(subtitle);
    }

    default
    void addTopLine () throws IOException {
        this.getLocation().x = LEFT_RIGHT_MARGIN;
        this.getLocation().y = this.height() - TOP_BOTTOM_MARGIN - STANDARD_LINE_SPACING - HALF_LINE_SPACING;
        this.getContents()
            .moveTo(this.getLocation().x, this.getLocation().y);
        this.getContents()
            .lineTo(this.width() - LEFT_RIGHT_MARGIN, this.getLocation().y);
        this.getContents()
            .stroke();
    }

    default
    void addBottomLine () throws IOException {
        this.getLocation().x = LEFT_RIGHT_MARGIN;
        this.getLocation().y = TOP_BOTTOM_MARGIN;
        this.getContents()
            .moveTo(this.getLocation().x, this.getLocation().y);
        this.getContents()
            .lineTo(this.width() - LEFT_RIGHT_MARGIN, this.getLocation().y);
        this.getContents()
            .stroke();
    }

    default
    void addPageNumber (final int pageNumber) throws IOException {
        this.getContents()
            .setFont(STANDARD_FONT, SUBTITLE_FONT_SIZE);
        final String pageNum = "%d".formatted(pageNumber);
        final float pageNumWidth = getTextWidth(STANDARD_FONT, SUBTITLE_FONT_SIZE, pageNum);
        this.getLocation().x = this.centerX() - pageNumWidth * 0.5f;
        this.getLocation().y = TOP_BOTTOM_MARGIN - TOP_BOTTOM_MARGIN * 0.5f;
        this.addColumnAgnosticText(pageNum);
    }

    default
    void initBody () {
        this.getLocation().x = LEFT_RIGHT_MARGIN;
        this.getLocation().y = this.getBodyContentStartY();
    }

    default
    void newLine (final float lineSpacing) {
        this.alignXtoColumn();
        this.getLocation().y -= lineSpacing;
    }

    default
    void alignXtoColumn () {
        this.getLocation().x = LEFT_RIGHT_MARGIN;
        for (int i = 1; i < this.getCurrentColumn(); ++i) {
            this.getLocation().x += this.columnWidth();
            if (i == 1) {
                this.getLocation().x += HALF_LINE_SPACING;
            }
        }
    }

    int getCurrentColumn ();

    void setCurrentColumn (int col);

    default
    boolean nextColumn () {
        if (this.getCurrentColumn() >= this.getMaxColumns()) {
            return false;
        }
        this.setCurrentColumn(this.getCurrentColumn() + 1);
        this.alignXtoColumn();
        this.getLocation().y = this.getBodyContentStartY();
        return true;
    }

    default
    void addColumnCenteredText (final @NotNull String line, final @NotNull PDFont font) throws IOException {
        final float fontSize = this.getColumnFittedFontSize(line, font);
        this.getContents()
            .setFont(font, fontSize);
        final float columnCenterX = (this.getCurrentColumn() == 1 || this.getCurrentColumn() == this.getMaxColumns())
                ? (this.columnWidth() - HALF_LINE_SPACING) * 0.5f
                : (this.columnWidth() - STANDARD_LINE_SPACING) * 0.5f;
        this.getLocation().x += columnCenterX - getTextWidth(font, fontSize, line) * 0.5f;
        this.addColumnAgnosticText(line);
    }

    default
    void addColumnRightJustifiedText (final @NotNull String line, final @NotNull PDFont font) throws IOException {
        final float fontSize = this.getColumnFittedFontSize(line, font);
        this.getContents()
            .setFont(font, fontSize);
        final float columnRightX = (this.getCurrentColumn() == 1 || this.getCurrentColumn() == this.getMaxColumns())
                ? this.columnWidth() - HALF_LINE_SPACING
                : this.columnWidth() - STANDARD_LINE_SPACING;
        this.getLocation().x += columnRightX - IPDPageHelper.getTextWidth(font, fontSize, line);
        this.addColumnAgnosticText(line);
    }

    @Override
    default
    void close () throws IOException {
        this.getContents()
            .close();
    }

    class Location {
        public float x;
        public float y;

        public
        Location (final float x, final float y) {
            super();
            this.x = x;
            this.y = y;
        }
    }

    final
    class NewPageException extends Exception {
    }
}
