package org.familydirectory.assets.lambda.function.pdf.helper;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

final
class PDPageHelper implements Closeable {
    private static final char DAGGER = '†';
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
    private static final float BODY_CONTENT_END_Y = TOP_BOTTOM_MARGIN + THREE_HALF_LINE_SPACING;
    private static final float DOUBLE_LINE_SPACING = STANDARD_LINE_SPACING * 2.0f;
    private final @NotNull Location location = new Location(0.0f, 0.0f);
    private final @NotNull PDPageContentStream contents;
    private final @NotNull PDPage page;
    private final float bodyContentStartY;
    private int currentColumn = 1;

    PDPageHelper (final @NotNull PDDocument pdf, final @NotNull PDPage page, final @NotNull String title, final @NotNull String subtitle, final int pageNumber) throws IOException {
        super();
        this.page = page;
        pdf.addPage(this.page);
        this.contents = new PDPageContentStream(pdf, this.page);
        this.addTitle(title);
        this.addSubtitle(subtitle);
        this.addTopLine();
        this.bodyContentStartY = this.location.y - THREE_HALF_LINE_SPACING;
        this.addBottomLine();
        this.addColumnLines();
        this.addPageNumber(pageNumber);
        this.initBody();
    }

    private static
    float inch2px (final float inch) {
        return inch * PX_IN_INCH;
    }

    private static @Nullable
    Integer getLongestLineWidthInList (final @NotNull List<String> list, final @NotNull PDFont font, final float fontSize) throws IOException {
        float longestLineWidth = 0.0f;
        Integer longestLine = null;
        for (int i = 0; i < list.size(); ++i) {
            final float lineWidth = getTextWidth(font, fontSize, list.get(i));
            if (lineWidth > longestLineWidth) {
                longestLineWidth = lineWidth;
                longestLine = i;
            }
        }
        return longestLine;
    }

    private static
    float getTextWidth (final @NotNull PDFont font, final float fontSize, final @NotNull String text) throws IOException {
        return font.getStringWidth(text) / 1000.0f * fontSize;
    }

    private
    void addTitle (final @NotNull String title) throws IOException {
        final float titleWidth = getTextWidth(TITLE_FONT, TITLE_FONT_SIZE, title);
        this.contents.setFont(TITLE_FONT, TITLE_FONT_SIZE);
        this.location.x = this.centerX() - titleWidth * 0.5f;
        this.location.y = this.height() - TOP_BOTTOM_MARGIN;
        this.addColumnAgnosticText(title);
    }

    private
    void addSubtitle (final @NotNull String subtitle) throws IOException {
        final float subtitleWidth = getTextWidth(SUBTITLE_FONT, SUBTITLE_FONT_SIZE, subtitle);
        this.contents.setFont(SUBTITLE_FONT, SUBTITLE_FONT_SIZE);
        this.location.x = this.centerX() - subtitleWidth * 0.5f;
        this.location.y -= STANDARD_LINE_SPACING;
        this.addColumnAgnosticText(subtitle);
    }

    private
    void addTopLine () throws IOException {
        this.location.x = LEFT_RIGHT_MARGIN;
        this.location.y -= HALF_LINE_SPACING;
        this.contents.moveTo(this.location.x, this.location.y);
        this.contents.lineTo(this.width() - LEFT_RIGHT_MARGIN, this.location.y);
        this.contents.stroke();
    }

    private
    void addBottomLine () throws IOException {
        this.location.x = LEFT_RIGHT_MARGIN;
        this.location.y = TOP_BOTTOM_MARGIN;
        this.contents.moveTo(this.location.x, this.location.y);
        this.contents.lineTo(this.width() - LEFT_RIGHT_MARGIN, this.location.y);
        this.contents.stroke();
    }

    private
    void addColumnLines () throws IOException {
        this.location.x = LEFT_RIGHT_MARGIN + this.columnWidth();
        this.location.y = this.bodyContentStartY + STANDARD_LINE_SPACING;
        for (int i = 1; i < MAX_COLUMNS; ++i, this.location.x += this.columnWidth()) {
            this.contents.moveTo(this.location.x, this.location.y);
            this.contents.lineTo(this.location.x, BODY_CONTENT_END_Y - STANDARD_LINE_SPACING);
            this.contents.stroke();
        }
        this.contents.moveTo(this.location.x, this.location.y);
    }

    private
    void addPageNumber (final int pageNumber) throws IOException {
        this.contents.setFont(STANDARD_FONT, SUBTITLE_FONT_SIZE);
        final String pageNum = "%d".formatted(pageNumber);
        final float pageNumWidth = getTextWidth(STANDARD_FONT, SUBTITLE_FONT_SIZE, pageNum);
        this.location.x = this.centerX() - pageNumWidth * 0.5f;
        this.location.y = TOP_BOTTOM_MARGIN - TOP_BOTTOM_MARGIN * 0.5f;
        this.addColumnAgnosticText(pageNum);
    }

    private
    void initBody () {
        this.location.x = LEFT_RIGHT_MARGIN;
        this.location.y = this.bodyContentStartY;
    }

    private
    boolean nextColumn () {
        if (this.currentColumn == MAX_COLUMNS) {
            return false;
        }
        ++this.currentColumn;
        this.location.x = LEFT_RIGHT_MARGIN + HALF_LINE_SPACING;
        for (int i = 1; i < this.currentColumn; ++i) {
            this.location.x += this.columnWidth();
        }
        this.location.y = this.bodyContentStartY;
        return true;
    }

    private
    float columnWidth () {
        return (this.width() - LEFT_RIGHT_MARGIN * 2.0f) / (float) MAX_COLUMNS;
    }

    private
    float width () {
        return this.page.getMediaBox()
                        .getWidth();
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

    private
    float centerX () {
        return this.width() * 0.5f;
    }

    private
    void addColumnAgnosticText (final @NotNull String line) throws IOException {
        this.contents.beginText();
        this.contents.newLineAtOffset(this.location.x, this.location.y);
        this.contents.showText(line);
        this.contents.endText();
    }

    void addBodyTextBlock (final @NotNull Member member, final @Nullable Member spouse, final @Nullable List<Member> deadEndDescendants) {
        if (isNull(spouse) && isNull(deadEndDescendants)) {
            // SINGLE MEMBER & NO DESCENDANTS
            this.addSingleMemberBodyTextBlock(member);
        } else if (isNull(spouse)) {
            // SINGLE PARENT
        } else if (isNull(deadEndDescendants)) {
            // PARENTS & NO DESCENDANTS
        } else {
            // PARENTS
        }
    }

    private
    void addSingleMemberBodyTextBlock (final @NotNull Member member) {
        final float blockSizeYOffset = calculateBlockSizeYOffset(member, null, null);
    }

    private static
    float calculateBlockSizeYOffset (final @NotNull Member member, final @Nullable Member spouse, final @Nullable List<Member> deadEndDescendants) {
        float blockSizeYOffset = 0.0f;
        if (isNull(spouse) && isNull(deadEndDescendants)) {
            // SINGLE MEMBER & NO DESCENDANTS
            if (nonNull(member.getEmail())) {
                blockSizeYOffset += STANDARD_LINE_SPACING;
            }
            if (nonNull(member.getAddress())) {
                blockSizeYOffset += STANDARD_LINE_SPACING * member.getAddress()
                                                                  .size();
            }
            if (nonNull(member.getPhones())) {
                blockSizeYOffset += STANDARD_LINE_SPACING * member.getPhones()
                                                                  .size();
            }
        } else if (isNull(spouse)) {
            // SINGLE PARENT
            if (nonNull(member.getEmail())) {
                blockSizeYOffset += STANDARD_LINE_SPACING;
            }
            if (nonNull(member.getAddress())) {
                blockSizeYOffset += STANDARD_LINE_SPACING * member.getAddress()
                                                                  .size();
            }
            if (nonNull(member.getPhones())) {
                blockSizeYOffset += STANDARD_LINE_SPACING * member.getPhones()
                                                                  .size();
            }
            for (final Member desc : deadEndDescendants) {
                blockSizeYOffset += STANDARD_LINE_SPACING;
                if (nonNull(desc.getEmail())) {
                    blockSizeYOffset += STANDARD_LINE_SPACING;
                }
                if (nonNull(member.getPhones())) {
                    blockSizeYOffset += STANDARD_LINE_SPACING;
                }
            }
        } else if (isNull(deadEndDescendants)) {
            // PARENTS & NO DESCENDANTS
            
        } else {
            // PARENTS
        }
        return blockSizeYOffset;
    }

    private
    float getColumnFittedFontSize (final @NotNull String line, final @NotNull PDFont font) throws IOException {
        float fontSize = STANDARD_FONT_SIZE;
        while (getTextWidth(font, fontSize, line) > this.columnWidth() - STANDARD_LINE_SPACING) {
            fontSize -= 0.01f;
        }
        return fontSize;
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

    public static final
    class NewPageException extends Exception {
    }
}
