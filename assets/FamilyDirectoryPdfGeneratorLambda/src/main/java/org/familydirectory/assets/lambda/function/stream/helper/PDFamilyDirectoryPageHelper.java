package org.familydirectory.assets.lambda.function.stream.helper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.familydirectory.assets.ddb.enums.PhoneType;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.lambda.function.stream.helper.models.IPDPageHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.familydirectory.assets.ddb.models.member.MemberModel.DAGGER;

final
class PDFamilyDirectoryPageHelper extends IPDPageHelper {

    PDFamilyDirectoryPageHelper (final @NotNull PDDocument pdf, final @NotNull PDPage page, final @NotNull String title, final @NotNull LocalDate subtitle, final int pageNumber) throws IOException {
        super(pdf, page, title, subtitle, pageNumber);
        this.maxColumns = 4;
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
            final float linesEnd = (this.currentColumn == 1 || this.currentColumn == this.maxColumns)
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
                final float headerFontSize = this.getColumnFittedFontSize(header, TITLE_FONT);
                this.contents.setFont(TITLE_FONT, headerFontSize);
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
            final Map.Entry<String, String> header = (IPDPageHelper.getTextWidth(TITLE_FONT, STANDARD_FONT_SIZE, spouseName) > IPDPageHelper.getTextWidth(TITLE_FONT, STANDARD_FONT_SIZE, memberName))
                    ? Map.entry("%s &".formatted(memberName), spouseName)
                    : Map.entry(memberName, "& %s".formatted(spouseName));
            this.addColumnCenteredText(header.getKey(), TITLE_FONT);
            this.newLine(STANDARD_LINE_SPACING);
            this.addColumnCenteredText(header.getValue(), TITLE_FONT);
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

    private
    boolean bodyTextBlockNeedsNewColumn (final float blockSizeYOffset) {
        return this.location.y < this.bodyContentStartY && (this.location.y - blockSizeYOffset) < BODY_CONTENT_END_Y;
    }
}
