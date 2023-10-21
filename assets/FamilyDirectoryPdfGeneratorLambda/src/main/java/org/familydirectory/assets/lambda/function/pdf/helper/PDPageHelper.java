package org.familydirectory.assets.lambda.function.pdf.helper;

import java.io.Closeable;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.jetbrains.annotations.NotNull;

final
class PDPageHelper implements Closeable {
    private static final float PX_IN_INCH = 72.0f;
    private static final float TOP_BOTTOM_MARGIN = inch2px(0.75f);
    private static final float LEFT_RIGHT_MARGIN = inch2px(0.5f);
    private static final int MAX_COLUMNS = 4;
    private static final PDFont TITLE_FONT = PDType1Font.HELVETICA_BOLD;
    private static final float TITLE_FONT_SIZE = 14.0f;
    private static final PDFont SUBTITLE_FONT = PDType1Font.HELVETICA_OBLIQUE;
    private static final float SUBTITLE_FONT_SIZE = 8.0f;
    private static final PDFont STANDARD_FONT = PDType1Font.HELVETICA;
    private static final float STANDARD_FONT_SIZE = 10.0f;
    private static final float STANDARD_LINE_SPACING = inch2px(0.2f);
    private static final float HALF_LINE_SPACING = STANDARD_LINE_SPACING * 0.5f;
    private static final float THREE_HALF_LINE_SPACING = STANDARD_LINE_SPACING * 1.5f;
    private final @NotNull Location location = new Location(0.0f, 0.0f);
    private final @NotNull PDPageContentStream contents;
    private final @NotNull PDPage page;
    private int currentColumn = 1;

    PDPageHelper (final @NotNull PDDocument pdf, final @NotNull PDPage page) throws IOException {
        super();
        this.page = page;
        pdf.addPage(this.page);
        this.contents = new PDPageContentStream(pdf, this.page);
    }

    private static
    float inch2px (final float inch) {
        return inch * PX_IN_INCH;
    }

    boolean nextColumn () {
        if (this.currentColumn == 4) {
            return false;
        }
        ++this.currentColumn;
        return true;
    }

    private
    float centerY () {
        return this.height() * 0.5f;
    }

    private
    float height () {
        return this.page.getMediaBox()
                        .getHeight();
    }

    void addTitle (final @NotNull String title) throws IOException {
        final float titleWidth = getTextWidth(TITLE_FONT, TITLE_FONT_SIZE, title);
        this.contents.setFont(TITLE_FONT, TITLE_FONT_SIZE);
        this.location.x = this.centerX() - titleWidth * 0.5f;
        this.location.y = this.height() - TOP_BOTTOM_MARGIN;
        this.addColumnAgnosticText(title);
    }

    private
    float centerX () {
        return this.width() * 0.5f;
    }

    private
    float width () {
        return this.page.getMediaBox()
                        .getWidth();
    }

    private static
    float getTextWidth (final @NotNull PDFont font, final float fontSize, final @NotNull String text) throws IOException {
        return font.getStringWidth(text) / 1000.0f * fontSize;
    }

    private
    void addColumnAgnosticText (final @NotNull String line) throws IOException {
        this.contents.beginText();
        this.contents.newLineAtOffset(this.location.x, this.location.y);
        this.contents.showText(line);
        this.contents.endText();
    }

    void addSubtitle (final @NotNull String subtitle) throws IOException {
        final float subtitleWidth = getTextWidth(SUBTITLE_FONT, SUBTITLE_FONT_SIZE, subtitle);
        this.contents.setFont(SUBTITLE_FONT, SUBTITLE_FONT_SIZE);
        this.location.x = this.centerX() - subtitleWidth * 0.5f;
        this.location.y = this.height() - TOP_BOTTOM_MARGIN - STANDARD_LINE_SPACING;
        this.addColumnAgnosticText(subtitle);
    }

    void addTopLine () throws IOException {
        this.location.x = LEFT_RIGHT_MARGIN;
        this.location.y = this.height() - TOP_BOTTOM_MARGIN - STANDARD_LINE_SPACING - HALF_LINE_SPACING;
        this.contents.moveTo(this.location.x, this.location.y);
        this.contents.lineTo(this.width() - LEFT_RIGHT_MARGIN, this.location.y);
        this.contents.stroke();
    }

    void addBottomLine () throws IOException {
        this.location.x = LEFT_RIGHT_MARGIN;
        this.location.y = TOP_BOTTOM_MARGIN;
        this.contents.moveTo(this.location.x, this.location.y);
        this.contents.lineTo(this.width() - LEFT_RIGHT_MARGIN, this.location.y);
        this.contents.stroke();
    }

    void addPageNumber (final int pageNumber) throws IOException {
        this.contents.setFont(STANDARD_FONT, SUBTITLE_FONT_SIZE);
        final String pageNum = "%d".formatted(pageNumber);
        final float pageNumWidth = getTextWidth(STANDARD_FONT, SUBTITLE_FONT_SIZE, pageNum);
        this.location.x = this.centerX() - pageNumWidth * 0.5f;
        this.location.y = TOP_BOTTOM_MARGIN - TOP_BOTTOM_MARGIN * 0.5f;
        this.addColumnAgnosticText(pageNum);
    }

    void addColumnLines () throws IOException {
        this.location.x = LEFT_RIGHT_MARGIN + this.columnWidth();
        this.location.y = this.bodyContentStartY();
        for (int i = 1; i < MAX_COLUMNS; ++i, this.location.x += this.columnWidth()) {
            this.contents.moveTo(this.location.x, this.location.y);
            this.contents.lineTo(this.location.x, this.bodyContentEndY());
            this.contents.stroke();
        }
        this.contents.moveTo(this.location.x, this.location.y);
    }

    private
    float columnWidth () {
        return (this.width() - LEFT_RIGHT_MARGIN * 2.0f) / (float) MAX_COLUMNS;
    }

    private
    float bodyContentStartY () {
        return this.height() - TOP_BOTTOM_MARGIN - STANDARD_LINE_SPACING - HALF_LINE_SPACING - THREE_HALF_LINE_SPACING;
    }

    private
    float bodyContentEndY () {
        return TOP_BOTTOM_MARGIN + THREE_HALF_LINE_SPACING;
    }

    void initBody () throws IOException {
        this.location.x = LEFT_RIGHT_MARGIN;
        this.location.y = this.bodyContentStartY();
        this.contents.moveTo(this.location.x, this.location.y);
    }

    @Override
    public
    void close () throws IOException {
        this.contents.close();
    }

    private static final
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
}
