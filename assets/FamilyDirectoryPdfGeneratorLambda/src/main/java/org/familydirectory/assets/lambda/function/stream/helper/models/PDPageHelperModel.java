package org.familydirectory.assets.lambda.function.stream.helper.models;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.jetbrains.annotations.NotNull;
import static java.util.Objects.requireNonNull;
import static org.familydirectory.assets.ddb.models.member.MemberModel.DAGGER;

public abstract
class PDPageHelperModel implements Closeable {
    protected static final float SUBTITLE_FONT_SIZE = 8.0f;
    protected static final PDFont STANDARD_FONT = PDType1Font.HELVETICA;
    protected static final float STANDARD_FONT_SIZE = 10.0f;
    protected static final float TITLE_FONT_SIZE = 14.0f;
    protected static final float PX_IN_INCH = 72.0f;
    protected static final float STANDARD_LINE_SPACING = inch2px(0.2f);
    protected static final float HALF_LINE_SPACING = STANDARD_LINE_SPACING / 2.0f;
    protected static final float DOUBLE_LINE_SPACING = STANDARD_LINE_SPACING * 2.0f;
    protected static final float THREE_HALF_LINE_SPACING = STANDARD_LINE_SPACING * 1.5f;
    protected static final float TEXT_RISE = inch2px(1.0f / 32.0f);
    protected static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    protected static final char BULLET = '•';
    protected static final String TAB = "  ";
    protected static final float TOP_BOTTOM_MARGIN = inch2px(0.75f);
    protected static final float BODY_CONTENT_END_Y = TOP_BOTTOM_MARGIN + THREE_HALF_LINE_SPACING;
    protected static final float LEFT_RIGHT_MARGIN = inch2px(0.5f);
    protected static final PDFont TITLE_FONT = PDType1Font.HELVETICA_BOLD;
    protected static final PDFont SUBTITLE_FONT = PDType1Font.HELVETICA_OBLIQUE;
    protected final @NotNull PDPage page;
    protected final @NotNull PDPageContentStream contents;
    protected final @NotNull Location location = new Location(0.0f, 0.0f);
    protected final float bodyContentStartY;
    protected int currentColumn = 1;
    protected int maxColumns;

    protected
    PDPageHelperModel (final @NotNull PDDocument pdf, final @NotNull PDPage page, final @NotNull String title, final @NotNull LocalDate subtitle, final int pageNumber) throws IOException {
        super();
        this.page = requireNonNull(page);
        requireNonNull(pdf).addPage(this.page);
        this.contents = new PDPageContentStream(pdf, this.page);
        this.addTitle(requireNonNull(title));
        this.addSubtitle(requireNonNull(subtitle).format(DISPLAY_DATE_FORMATTER));
        this.addTopLine();
        this.bodyContentStartY = this.location.y - THREE_HALF_LINE_SPACING;
        this.addBottomLine();
        this.addColumnLines();
        this.addPageNumber(pageNumber);
        this.initBody();
    }

    protected static
    float getTextSpaceUnits (final @NotNull PDFont font, final @NotNull String text) throws IOException {
        // https://javadoc.io/static/org.apache.pdfbox/pdfbox/2.0.30/org/apache/pdfbox/pdmodel/font/PDFont.html#getStringWidth-java.lang.String-
        return font.getStringWidth(text) / 1000.0f;
    }

    protected static
    float inch2px (final float inch) {
        return inch * PX_IN_INCH;
    }

    protected static
    float getTextWidth (final @NotNull PDFont font, final float fontSize, final @NotNull String text) throws IOException {
        return getTextSpaceUnits(font, text) * fontSize;
    }

    protected
    float getColumnFittedFontSize (final @NotNull String line, final @NotNull PDFont font, final float defaultFontSize) throws IOException {
        final float fontSize = (this.columnWidth() - STANDARD_LINE_SPACING) / getTextSpaceUnits(font, line);
        if (Float.isNaN(fontSize) || Float.isInfinite(fontSize) || fontSize <= 0.0f) {
            throw new RuntimeException("text: `%s` not single-line-fittable with font: `%s`".formatted(line, font.getName()));
        }
        return Math.min(defaultFontSize, fontSize);
    }

    protected final
    void addColumnLines () throws IOException {
        this.location.x = LEFT_RIGHT_MARGIN + this.columnWidth();
        this.location.y = this.bodyContentStartY + STANDARD_LINE_SPACING;
        for (int i = 1; i < this.maxColumns; ++i, this.location.x += this.columnWidth()) {
            this.contents.moveTo(this.location.x, this.location.y);
            this.contents.lineTo(this.location.x, BODY_CONTENT_END_Y - STANDARD_LINE_SPACING);
            this.contents.stroke();
        }
        this.contents.moveTo(this.location.x, this.location.y);
    }

    protected final
    void addTitle (final @NotNull String title) throws IOException {
        final float titleWidth = getTextWidth(TITLE_FONT, TITLE_FONT_SIZE, title);
        this.contents.setFont(TITLE_FONT, TITLE_FONT_SIZE);
        this.location.x = this.centerX() - titleWidth / 2.0f;
        this.location.y = this.height() - TOP_BOTTOM_MARGIN;
        this.addColumnAgnosticText(title);
    }

    protected final
    float centerX () {
        return this.width() / 2.0f;
    }

    protected final
    float height () {
        return this.page.getMediaBox()
                        .getHeight();
    }

    protected final
    void addColumnAgnosticText (final @NotNull String line) throws IOException {
        this.contents.beginText();
        this.contents.newLineAtOffset(this.location.x, this.location.y);

        final String daggerStr = String.valueOf(DAGGER);
        if (line.contains(daggerStr)) {
            final String[] splitLines = line.split(daggerStr);
            for (int i = 0; i < splitLines.length; ++i) {
                if (!splitLines[i].isEmpty()) {
                    this.contents.showText(splitLines[i]);
                }
                if (i < splitLines.length - 1) {
                    this.contents.setTextRise(TEXT_RISE);
                    this.contents.showText(daggerStr);
                    this.contents.setTextRise(0.0f);
                }
            }
        } else {
            this.contents.showText(line);
        }

        this.contents.endText();
    }

    protected final
    void addSubtitle (final @NotNull String subtitle) throws IOException {
        final float subtitleWidth = PDPageHelperModel.getTextWidth(SUBTITLE_FONT, SUBTITLE_FONT_SIZE, subtitle);
        this.contents.setFont(SUBTITLE_FONT, SUBTITLE_FONT_SIZE);
        this.location.x = this.centerX() - subtitleWidth / 2.0f;
        this.location.y = this.height() - TOP_BOTTOM_MARGIN - STANDARD_LINE_SPACING;
        this.addColumnAgnosticText(subtitle);
    }

    protected final
    void addTopLine () throws IOException {
        this.location.x = LEFT_RIGHT_MARGIN;
        this.location.y = this.height() - TOP_BOTTOM_MARGIN - STANDARD_LINE_SPACING - HALF_LINE_SPACING;
        this.contents.moveTo(this.location.x, this.location.y);
        this.contents.lineTo(this.width() - LEFT_RIGHT_MARGIN, this.location.y);
        this.contents.stroke();
    }

    protected final
    void addBottomLine () throws IOException {
        this.location.x = LEFT_RIGHT_MARGIN;
        this.location.y = TOP_BOTTOM_MARGIN;
        this.contents.moveTo(this.location.x, this.location.y);
        this.contents.lineTo(this.width() - LEFT_RIGHT_MARGIN, this.location.y);
        this.contents.stroke();
    }

    protected final
    void addPageNumber (final int pageNumber) throws IOException {
        this.contents.setFont(STANDARD_FONT, SUBTITLE_FONT_SIZE);
        final String pageNum = "%d".formatted(pageNumber);
        final float pageNumWidth = getTextWidth(STANDARD_FONT, SUBTITLE_FONT_SIZE, pageNum);
        this.location.x = this.centerX() - pageNumWidth / 2.0f;
        this.location.y = TOP_BOTTOM_MARGIN - TOP_BOTTOM_MARGIN / 2.0f;
        this.addColumnAgnosticText(pageNum);
    }

    protected final
    void initBody () {
        this.location.x = LEFT_RIGHT_MARGIN;
        this.location.y = this.bodyContentStartY;
    }

    protected final
    void newLine (final float lineSpacing) {
        this.alignXtoColumn();
        this.location.y -= lineSpacing;
    }

    protected final
    void alignXtoColumn () {
        this.location.x = LEFT_RIGHT_MARGIN;
        for (int i = 1; i < this.currentColumn; ++i) {
            this.location.x += this.columnWidth();
            if (i == 1) {
                this.location.x += HALF_LINE_SPACING;
            }
        }
    }

    protected final
    float columnWidth () {
        return (this.width() - LEFT_RIGHT_MARGIN * 2.0f) / (float) this.maxColumns;
    }

    protected final
    float width () {
        return this.page.getMediaBox()
                        .getWidth();
    }

    protected final
    boolean nextColumn () {
        if (this.currentColumn >= this.maxColumns) {
            return false;
        }
        ++this.currentColumn;
        this.alignXtoColumn();
        this.location.y = this.bodyContentStartY;
        return true;
    }

    protected final
    void addColumnCenteredText (final @NotNull String line, final @NotNull PDFont font, final float defaultFontSize) throws IOException {
        final float fontSize = this.getColumnFittedFontSize(line, font, defaultFontSize);
        this.contents.setFont(font, fontSize);
        final float columnCenterX = (this.currentColumn == 1 || this.currentColumn == this.maxColumns)
                ? (this.columnWidth() - HALF_LINE_SPACING) / 2.0f
                : (this.columnWidth() - STANDARD_LINE_SPACING) / 2.0f;
        this.location.x += columnCenterX - getTextWidth(font, fontSize, line) / 2.0f;
        this.addColumnAgnosticText(line);
    }

    protected final
    void addColumnRightJustifiedText (final @NotNull String line, final @NotNull PDFont font, final float defaultFontSize) throws IOException {
        final float fontSize = this.getColumnFittedFontSize(line, font, defaultFontSize);
        this.contents.setFont(font, fontSize);
        final float columnRightX = (this.currentColumn == 1 || this.currentColumn == this.maxColumns)
                ? this.columnWidth() - HALF_LINE_SPACING
                : this.columnWidth() - STANDARD_LINE_SPACING;
        this.location.x += columnRightX - PDPageHelperModel.getTextWidth(font, fontSize, line);
        this.addColumnAgnosticText(line);
    }

    protected final
    boolean isNewColumnNeeded (final float blockSizeYOffset) {
        return this.location.y < this.bodyContentStartY && (this.location.y - blockSizeYOffset) < BODY_CONTENT_END_Y;
    }

    @Override
    public final
    void close () throws IOException {
        this.contents.close();
    }

    public static final
    class NewPageException extends Exception {
    }

    protected static final
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
