package org.familydirectory.assets.lambda.function.stream.helper;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.familydirectory.assets.ddb.enums.PhoneType;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.utils.Pair;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.familydirectory.assets.ddb.models.member.MemberModel.DAGGER;

final
class PDPageHelper implements Closeable {
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");
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
    private static final float TEXT_RISE = inch2px(1.0f / 32.0f);
    private final @NotNull Location location = new Location(0.0f, 0.0f);
    private final @NotNull PDPageContentStream contents;
    private final @NotNull PDPage page;
    private final float bodyContentStartY;
    private int currentColumn = 1;

    PDPageHelper (final @NotNull PDDocument pdf, final @NotNull PDPage page, final @NotNull String title, final @NotNull LocalDate subtitle, final int pageNumber) throws IOException {
        super();
        this.page = page;
        pdf.addPage(this.page);
        this.contents = new PDPageContentStream(pdf, this.page);
        this.addTitle(title);
        this.addSubtitle(subtitle.format(DISPLAY_DATE_FORMATTER));
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
            blockSizeYOffset += DOUBLE_LINE_SPACING; // For Name & Birthday
            if (nonNull(desc.getDeathday())) {
                blockSizeYOffset += STANDARD_LINE_SPACING;
            }
            if (nonNull(desc.getEmail())) {
                blockSizeYOffset += STANDARD_LINE_SPACING;
            }
            if (ofNullable(desc.getPhones()).filter(m -> m.containsKey(PhoneType.MOBILE))
                                            .isPresent())
            {
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
        if (nonNull(member.getAddress()) && nonNull(spouse.getAddress()) && ofNullable(member.getPhones()).filter(m -> m.containsKey(PhoneType.LANDLINE))
                                                                                                          .isPresent() && ofNullable(spouse.getPhones()).filter(m -> m.containsKey(PhoneType.LANDLINE))
                                                                                                                                                        .isPresent())
        {
            blockSizeYOffset -= STANDARD_LINE_SPACING; // Don't Redundantly Account For Landline Phones
        }
        return blockSizeYOffset;
    }

    private static
    float singleMemberBlockSizeYOffset (final @NotNull Member member) {
        float blockSizeYOffset = STANDARD_LINE_SPACING; // ACCOUNT FOR BIRTHDAY
        if (nonNull(member.getDeathday())) {
            blockSizeYOffset += STANDARD_LINE_SPACING;
        }
        if (nonNull(member.getEmail())) {
            blockSizeYOffset += STANDARD_LINE_SPACING;
        }
        if (nonNull(member.getAddress())) {
            blockSizeYOffset += STANDARD_LINE_SPACING * member.getAddress()
                                                              .size();
            if (ofNullable(member.getPhones()).filter(m -> m.containsKey(PhoneType.LANDLINE))
                                              .isPresent())
            {
                blockSizeYOffset += STANDARD_LINE_SPACING;
            }
        }
        if (ofNullable(member.getPhones()).filter(m -> m.containsKey(PhoneType.MOBILE))
                                          .isPresent())
        {
            blockSizeYOffset += STANDARD_LINE_SPACING;
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
            final Pair<String, String> header = (getTextWidth(TITLE_FONT, STANDARD_FONT_SIZE, spouseName) > getTextWidth(TITLE_FONT, STANDARD_FONT_SIZE, memberName))
                    ? Pair.of("%s &".formatted(memberName), spouseName)
                    : Pair.of(memberName, "& %s".formatted(spouseName));
            this.addColumnCenteredText(header.left(), TITLE_FONT);
            this.newLine(STANDARD_LINE_SPACING);
            this.addColumnCenteredText(header.right(), TITLE_FONT);
            this.newLine(STANDARD_LINE_SPACING);
        }

//  ADD MEMBER BIRTHDAY
        this.addColumnRightJustifiedText(member.getBirthday()
                                               .format(DISPLAY_DATE_FORMATTER), STANDARD_FONT);
        this.newLine(STANDARD_LINE_SPACING);

//  ADD MEMBER DEATHDAY
        if (nonNull(member.getDeathday())) {
            this.addColumnRightJustifiedText("%c%s".formatted(DAGGER, member.getDeathday()
                                                                            .format(DISPLAY_DATE_FORMATTER)), STANDARD_FONT);
            this.newLine(STANDARD_LINE_SPACING);
        }

        if (nonNull(spouse)) {
//      ADD SPOUSE BIRTHDAY
            this.addColumnRightJustifiedText(spouse.getBirthday()
                                                   .format(DISPLAY_DATE_FORMATTER), STANDARD_FONT);
            this.newLine(STANDARD_LINE_SPACING);

//      ADD SPOUSE DEATHDAY
            if (nonNull(spouse.getDeathday())) {
                this.addColumnRightJustifiedText("%c%s".formatted(DAGGER, spouse.getDeathday()
                                                                                .format(DISPLAY_DATE_FORMATTER)), STANDARD_FONT);
                this.newLine(STANDARD_LINE_SPACING);
            }
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
    void addColumnRightJustifiedText (final @NotNull String line, final @NotNull PDFont font) throws IOException {
        final float fontSize = this.getColumnFittedFontSize(line, font);
        this.contents.setFont(font, fontSize);
        final float columnRightX = (this.currentColumn == 1 || this.currentColumn == MAX_COLUMNS)
                ? this.columnWidth() - HALF_LINE_SPACING
                : this.columnWidth() - STANDARD_LINE_SPACING;
        this.location.x += columnRightX - getTextWidth(font, fontSize, line);
        this.addColumnAgnosticText(line);
    }

    private
    float getColumnFittedFontSize (final @NotNull String line, final @NotNull PDFont font) throws IOException {
        float fontSize = STANDARD_FONT_SIZE;
        while (getTextWidth(font, fontSize, line) > this.columnWidth() - STANDARD_LINE_SPACING) {
            fontSize -= 0.01f;
        }
        return fontSize;
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
    void addColumnAgnosticText (final @NotNull String line) throws IOException {
        if (!line.contains(String.valueOf(DAGGER))) {
            this.contents.beginText();
            this.contents.newLineAtOffset(this.location.x, this.location.y);
            this.contents.showText(line);
            this.contents.endText();
        } else {
            final String[] splitLines = line.split(String.valueOf(DAGGER));
            this.contents.beginText();
            this.contents.newLineAtOffset(this.location.x, this.location.y);
            for (int i = 0; i < splitLines.length; ++i) {
                if (!splitLines[i].isEmpty()) {
                    this.contents.showText(splitLines[i]);
                }
                if (i < splitLines.length - 1) {
                    this.contents.setTextRise(TEXT_RISE);
                    this.contents.showText(String.valueOf(DAGGER));
                    this.contents.setTextRise(0.0f);
                }
            }
            this.contents.endText();
        }
    }

    private
    void addAddressToBodyTextBlock (final @NotNull List<String> address) throws IOException {
        float addressFontSize = STANDARD_FONT_SIZE;
        for (final String line : address) {
            final float fontSize = this.getColumnFittedFontSize(line, STANDARD_FONT);
            if (fontSize < addressFontSize) {
                addressFontSize = fontSize;
            }
        }
        this.contents.setFont(STANDARD_FONT, addressFontSize);
        for (final String line : address) {
            this.addColumnAgnosticText(line);
            this.newLine(STANDARD_LINE_SPACING);
        }
    }

    void addBodyTextBlock (final @NotNull Member member, final @Nullable Member spouse, final @Nullable List<Member> deadEndDescendants, final boolean startOfSection)
            throws NewPageException, IOException
    {
//  DETERMINE IF BLOCK FITS IN COLUMN
        float blockSizeYOffset = calculateBlockSizeYOffset(member, spouse, deadEndDescendants);
        if (startOfSection && this.location.y < this.bodyContentStartY) {
            blockSizeYOffset += THREE_HALF_LINE_SPACING;
        }
        if (this.bodyTextBlockNeedsNewColumn(blockSizeYOffset) && !this.nextColumn()) {
            throw new NewPageException();
        }

//  DRAW LINE BETWEEN SECTIONS
        if (startOfSection && this.location.y < this.bodyContentStartY) {
            this.contents.moveTo(this.location.x, this.location.y);
            final float linesEnd = (this.currentColumn == 1 || this.currentColumn == MAX_COLUMNS)
                    ? this.location.x + this.columnWidth() - HALF_LINE_SPACING
                    : this.location.x + this.columnWidth() - STANDARD_LINE_SPACING;
            this.contents.lineTo(linesEnd, this.location.y);
            this.contents.stroke();
            this.newLine(THREE_HALF_LINE_SPACING);
        }

//  PRINT BLOCK HEADER
        this.addBodyTextBlockHeader(member, spouse);

//  PRINT ADDRESS TO PDF
        Boolean memberIsLandlineHolder = null;
        if (nonNull(member.getAddress())) {
            memberIsLandlineHolder = true;
            this.addAddressToBodyTextBlock(member.getAddress());
        } else if (nonNull(spouse) && nonNull(spouse.getAddress())) {
            memberIsLandlineHolder = false;
            this.addAddressToBodyTextBlock(spouse.getAddress());
        }

//  PRINT LANDLINE PHONE TO PDF
        float phoneFontSize = STANDARD_FONT_SIZE;
        if (ofNullable(member.getPhones()).filter(m -> m.containsKey(PhoneType.MOBILE))
                                          .isPresent())
        {
            final float fontSize = this.getColumnFittedFontSize(member.getPhones()
                                                                      .get(PhoneType.MOBILE), STANDARD_FONT);
            if (fontSize < phoneFontSize) {
                phoneFontSize = fontSize;
            }
        }
        if (ofNullable(spouse).map(Member::getPhones)
                              .filter(m -> m.containsKey(PhoneType.MOBILE))
                              .isPresent())
        {
            final float fontSize = this.getColumnFittedFontSize(spouse.getPhones()
                                                                      .get(PhoneType.MOBILE), STANDARD_FONT);
            if (fontSize < phoneFontSize) {
                phoneFontSize = fontSize;
            }
        }
        if (nonNull(memberIsLandlineHolder)) {
            final Member landLineHolder = memberIsLandlineHolder
                    ? member
                    : spouse;
            if (ofNullable(landLineHolder.getPhones()).filter(m -> m.containsKey(PhoneType.LANDLINE))
                                                      .isPresent())
            {
                final float fontSize = this.getColumnFittedFontSize(landLineHolder.getPhones()
                                                                                  .get(PhoneType.LANDLINE), STANDARD_FONT);
                if (fontSize < phoneFontSize) {
                    phoneFontSize = fontSize;
                }
                this.contents.setFont(STANDARD_FONT, phoneFontSize);
                this.addColumnAgnosticText(landLineHolder.getPhones()
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

//          ADD DESCENDANT BIRTHDAY
                this.addColumnRightJustifiedText("%s%s".formatted(TAB, desc.getBirthday()
                                                                           .format(DISPLAY_DATE_FORMATTER)), STANDARD_FONT);
                this.newLine(STANDARD_LINE_SPACING);

//          ADD DESCENDANT DEATHDAY
                if (nonNull(desc.getDeathday())) {
                    this.addColumnRightJustifiedText("%s%c%s".formatted(TAB, DAGGER, desc.getDeathday()
                                                                                         .format(DISPLAY_DATE_FORMATTER)), STANDARD_FONT);
                    this.newLine(STANDARD_LINE_SPACING);
                }

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
