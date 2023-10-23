package org.familydirectory.assets.lambda.function.pdf.helper;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.familydirectory.assets.ddb.enums.PhoneType;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

final
class PDPageHelper implements Closeable {
    private static final char DAGGER = '†';
    private static final char BULLET = '•';
    private static final String TAB = "  ";
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

    private static
    float calculateBlockSizeYOffset (final @NotNull Member member, final @Nullable Member spouse, final @Nullable List<Member> deadEndDescendants) {
        float blockSizeYOffset = isNull(spouse)
                ? singleMemberBlockSizeYOffset(member)
                : parentsBlockSizeYOffset(member, spouse);
        if (nonNull(deadEndDescendants)) {
            blockSizeYOffset += descendantsBlockSizeYOffset(deadEndDescendants);
        }
        return blockSizeYOffset;
    }

    private static
    float descendantsBlockSizeYOffset (final @NotNull List<Member> descendants) {
        float blockSizeYOffset = 0.0f;
        for (final Member desc : descendants) {
            blockSizeYOffset += STANDARD_LINE_SPACING; // For Name
            if (nonNull(desc.getEmail())) {
                blockSizeYOffset += STANDARD_LINE_SPACING;
            }
            if (nonNull(desc.getPhones())) {
                blockSizeYOffset += STANDARD_LINE_SPACING;
            }
        }
        return blockSizeYOffset;
    }

    private static
    float parentsBlockSizeYOffset (final @NotNull Member member, final @NotNull Member spouse) {
        float blockSizeYOffset = 0.0f;
        if (member.getLastName()
                  .equals(spouse.getLastName()))
        {
            blockSizeYOffset += STANDARD_LINE_SPACING;
        } else {
            blockSizeYOffset += STANDARD_LINE_SPACING * 2.0f;
        }
        blockSizeYOffset += singleMemberBlockSizeYOffset(member);
        blockSizeYOffset += singleMemberBlockSizeYOffset(spouse);
        return blockSizeYOffset;
    }

    private static
    float singleMemberBlockSizeYOffset (final @NotNull Member member) {
        float blockSizeYOffset = 0.0f;
        if (nonNull(member.getEmail())) {
            blockSizeYOffset += STANDARD_LINE_SPACING;
        }
        if (nonNull(member.getAddress())) {
            blockSizeYOffset += STANDARD_LINE_SPACING * member.getAddress()
                                                              .size();
        }
        if (nonNull(member.getPhones())) {
            if (member.getPhones()
                      .containsKey(PhoneType.LANDLINE))
            {
                blockSizeYOffset += STANDARD_LINE_SPACING;
            }
            if (member.getPhones()
                      .containsKey(PhoneType.MOBILE))
            {
                blockSizeYOffset += STANDARD_LINE_SPACING;
            }
        }
        return blockSizeYOffset;
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
    void addBodyTextBlockHeader (final @NotNull Member member, final @Nullable Member spouse) throws IOException {
        if (isNull(spouse)) {
            final String header = (nonNull(member.getDeathday()))
                    ? "%c%s".formatted(DAGGER, member.getDisplayName())
                    : member.getDisplayName();
            this.addColumnCenteredText(header, TITLE_FONT);
            this.newLine(STANDARD_LINE_SPACING);
        } else if (member.getLastName()
                         .equals(spouse.getLastName()))
        {
            final String memberFirstName = (nonNull(member.getDeathday()))
                    ? "%c%s".formatted(DAGGER, member.getFirstName())
                    : member.getFirstName();
            final String spouseFirstName = (nonNull(spouse.getDeathday()))
                    ? "%c%s".formatted(DAGGER, spouse.getFirstName())
                    : spouse.getFirstName();
            final String header = "%s & %s %s".formatted(memberFirstName, spouseFirstName, member.getLastName());
            this.addColumnCenteredText(header, TITLE_FONT);
            this.newLine(STANDARD_LINE_SPACING);
        } else {
            final String memberName = (nonNull(member.getDeathday()))
                    ? "%c%s".formatted(DAGGER, member.getDisplayName())
                    : member.getDisplayName();
            final String spouseName = (nonNull(spouse.getDeathday()))
                    ? "%c%s".formatted(DAGGER, spouse.getDisplayName())
                    : spouse.getDisplayName();
            if (getTextWidth(TITLE_FONT, STANDARD_FONT_SIZE, memberName) >= getTextWidth(TITLE_FONT, STANDARD_FONT_SIZE, spouseName)) {
                final String header = "%s &".formatted(memberName);
                this.addColumnCenteredText(header, TITLE_FONT);
            } else {
                this.addColumnCenteredText(memberName, TITLE_FONT);
            }
            this.newLine(STANDARD_LINE_SPACING);
            this.addColumnCenteredText(spouseName, TITLE_FONT);
            this.newLine(STANDARD_LINE_SPACING);
        }
    }

    private
    void addColumnCenteredText (final @NotNull String line, final @NotNull PDFont font) throws IOException {
        final float fontSize = this.getColumnFittedFontSize(line, font);
        this.contents.setFont(font, fontSize);
        final float columnCenterX = (this.currentColumn == 1 || this.currentColumn == MAX_COLUMNS)
                ? (this.columnWidth() - HALF_LINE_SPACING) * 0.5f
                : (this.columnWidth() - STANDARD_LINE_SPACING) * 0.5f;
        this.location.x += columnCenterX - getTextWidth(font, fontSize, line) * 0.5f;
        this.addColumnAgnosticText(line);
    }

    private
    void addColumnAgnosticText (final @NotNull String line) throws IOException {
        this.contents.beginText();
        this.contents.newLineAtOffset(this.location.x, this.location.y);
        this.contents.showText(line);
        this.contents.endText();
    }

    private
    float getColumnFittedFontSize (final @NotNull String line, final @NotNull PDFont font) throws IOException {
        float fontSize = STANDARD_FONT_SIZE;
        while (getTextWidth(font, fontSize, line) > this.columnWidth() - STANDARD_LINE_SPACING) {
            fontSize -= 0.01f;
        }
        return fontSize;
    }

    void addBodyTextBlock (final @NotNull Member member, final @Nullable Member spouse, final @Nullable List<Member> deadEndDescendants) throws NewPageException, IOException {
        if (this.bodyTextBlockNeedsNewColumn(calculateBlockSizeYOffset(member, spouse, deadEndDescendants)) && !this.nextColumn()) {
            throw new NewPageException();
        }

//  PRINT BLOCK HEADER
        this.addBodyTextBlockHeader(member, spouse);

//  PRINT ADDRESS TO PDF
        if (nonNull(member.getAddress())) {
            float addressFontSize = STANDARD_FONT_SIZE;
            for (final String line : member.getAddress()) {
                final float fontSize = this.getColumnFittedFontSize(line, STANDARD_FONT);
                if (fontSize < addressFontSize) {
                    addressFontSize = fontSize;
                }
            }
            this.contents.setFont(STANDARD_FONT, addressFontSize);
            for (final String line : member.getAddress()) {
                this.addColumnAgnosticText(line);
                this.newLine(STANDARD_LINE_SPACING);
            }
        }

//  PRINT LANDLINE PHONE TO PDF
        float phoneFontSize = STANDARD_FONT_SIZE;
        if (nonNull(spouse) && nonNull(spouse.getPhones())) {
            for (final Map.Entry<PhoneType, String> phone : spouse.getPhones()
                                                                  .entrySet()) {
                final float fontSize = this.getColumnFittedFontSize(phone.getValue(), STANDARD_FONT);
                if (fontSize < phoneFontSize) {
                    phoneFontSize = fontSize;
                }
            }
        }
        if (nonNull(member.getPhones())) {
            for (final Map.Entry<PhoneType, String> phone : member.getPhones()
                                                                  .entrySet()) {
                final float fontSize = this.getColumnFittedFontSize(phone.getValue(), STANDARD_FONT);
                if (fontSize < phoneFontSize) {
                    phoneFontSize = fontSize;
                }
            }
            if (member.getPhones()
                      .containsKey(PhoneType.LANDLINE))
            {
                this.contents.setFont(STANDARD_FONT, phoneFontSize);
                this.addColumnAgnosticText(member.getPhones()
                                                 .get(PhoneType.LANDLINE));
                this.newLine(STANDARD_LINE_SPACING);
            }
        }

//  PRINT MEMBER AND SPOUSE EMAILS (MEMBER FIRST)
        float emailFontSize = STANDARD_FONT_SIZE;
        if (nonNull(member.getEmail())) {
            final float fontSize = this.getColumnFittedFontSize(member.getEmail(), STANDARD_FONT);
            if (fontSize < emailFontSize) {
                emailFontSize = fontSize;
            }
        }
        if (nonNull(spouse) && nonNull(spouse.getEmail())) {
            final float fontSize = this.getColumnFittedFontSize(spouse.getEmail(), STANDARD_FONT);
            if (fontSize < emailFontSize) {
                emailFontSize = fontSize;
            }
        }
        if (nonNull(member.getEmail())) {
            this.contents.setFont(STANDARD_FONT, emailFontSize);
            this.addColumnAgnosticText(member.getEmail());
            this.newLine(STANDARD_LINE_SPACING);
        }
        if (nonNull(spouse) && nonNull(spouse.getEmail())) {
            this.contents.setFont(STANDARD_FONT, emailFontSize);
            this.addColumnAgnosticText(spouse.getEmail());
            this.newLine(STANDARD_LINE_SPACING);
        }

//  PRINT MEMBER AND SPOUSE MOBILE PHONES (MEMBER FIRST)
        if (nonNull(member.getPhones()) && member.getPhones()
                                                 .containsKey(PhoneType.MOBILE))
        {
            this.contents.setFont(STANDARD_FONT, phoneFontSize);
            this.addColumnAgnosticText(member.getPhones()
                                             .get(PhoneType.MOBILE));
            this.newLine(STANDARD_LINE_SPACING);
        }
        if (nonNull(spouse) && nonNull(spouse.getPhones()) && spouse.getPhones()
                                                                    .containsKey(PhoneType.MOBILE))
        {
            this.contents.setFont(STANDARD_FONT, phoneFontSize);
            this.addColumnAgnosticText(spouse.getPhones()
                                             .get(PhoneType.MOBILE));
            this.newLine(STANDARD_LINE_SPACING);
        }

//  PRINT DEAD-END DESCENDANTS TO PDF
        if (nonNull(deadEndDescendants)) {
            for (final Member desc : deadEndDescendants) {
//          ADD DESCENDANT NAME
                final String header = "%c %s".formatted(BULLET, (nonNull(desc.getDeathday()))
                        ? "%c%s".formatted(DAGGER, desc.getDisplayName())
                        : desc.getDisplayName());
                final float headerFontSize = this.getColumnFittedFontSize(header, STANDARD_FONT);
                this.contents.setFont(STANDARD_FONT, headerFontSize);
                this.addColumnAgnosticText(header);
                this.newLine(STANDARD_LINE_SPACING);

//          ADD DESCENDANT EMAIL
                if (nonNull(desc.getEmail())) {
                    final String email = "%s%s".formatted(TAB, desc.getEmail());
                    final float fontSize = this.getColumnFittedFontSize(email, STANDARD_FONT);
                    this.contents.setFont(STANDARD_FONT, fontSize);
                    this.addColumnAgnosticText(email);
                    this.newLine(STANDARD_LINE_SPACING);
                }

//          ADD DESCENDANT MOBILE PHONE
                if (nonNull(desc.getPhones()) && desc.getPhones()
                                                     .containsKey(PhoneType.MOBILE))
                {
                    final String phone = "%s%s".formatted(TAB, desc.getPhones()
                                                                   .get(PhoneType.MOBILE));
                    final float fontSize = this.getColumnFittedFontSize(phone, STANDARD_FONT);
                    this.contents.setFont(STANDARD_FONT, fontSize);
                    this.addColumnAgnosticText(phone);
                    this.newLine(STANDARD_LINE_SPACING);
                }
            }
        }

        this.newLine(HALF_LINE_SPACING);
    }

    private
    boolean nextColumn () {
        if (this.currentColumn == MAX_COLUMNS) {
            return false;
        }
        ++this.currentColumn;
        this.alignXtoColumn();
        this.location.y = this.bodyContentStartY;
        return true;
    }

    private
    void newLine (final float lineSpacing) {
        this.alignXtoColumn();
        this.location.y -= lineSpacing;
    }

    private
    void alignXtoColumn () {
        this.location.x = LEFT_RIGHT_MARGIN;
        for (int i = 1; i < this.currentColumn; ++i) {
            this.location.x += this.columnWidth();
            if (i == 1) {
                this.location.x += HALF_LINE_SPACING;
            }
        }
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
    boolean bodyTextBlockNeedsNewColumn (final float blockSizeYOffset) {
        return this.location.y < this.bodyContentStartY && (this.location.y - blockSizeYOffset) < BODY_CONTENT_END_Y;
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
