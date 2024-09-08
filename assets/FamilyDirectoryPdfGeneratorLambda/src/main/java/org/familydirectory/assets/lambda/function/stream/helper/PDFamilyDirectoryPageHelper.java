package org.familydirectory.assets.lambda.function.stream.helper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.familydirectory.assets.ddb.enums.PhoneType;
import org.familydirectory.assets.ddb.member.Member;
import org.familydirectory.assets.lambda.function.stream.helper.models.PDPageHelperModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.familydirectory.assets.ddb.models.member.MemberModel.DAGGER;

final
class PDFamilyDirectoryPageHelper extends PDPageHelperModel {
    PDFamilyDirectoryPageHelper (final @NotNull PDDocument pdf, final @NotNull PDPage page, final @NotNull String title, final @NotNull LocalDate subtitle, final int pageNumber) throws IOException {
        super(pdf, page, title, subtitle, pageNumber);
    }

    void addBodyTextBlock (final @NotNull Member member, final @Nullable Member spouse, final @Nullable List<Member> deadEndDescendants, final boolean startOfSection)
            throws NewPageException, IOException
    {
//  DETERMINE IF BLOCK FITS IN COLUMN
        float blockSizeYOffset = calculateBlockSizeYOffset(member, spouse, deadEndDescendants);
        if (startOfSection && this.location.y < this.bodyContentStartY) {
            blockSizeYOffset += THREE_HALF_LINE_SPACING;
        }
        if (this.isNewColumnNeeded(blockSizeYOffset) && !this.nextColumn()) {
            throw new NewPageException();
        }

//  DRAW LINE BETWEEN SECTIONS
        if (startOfSection && this.location.y < this.bodyContentStartY) {
            this.contents.moveTo(this.location.x, this.location.y);
            final float linesEnd = (this.currentColumn == 1 || this.currentColumn == this.maxColumns())
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
        List<String> address = member.getAddress();
        if (nonNull(address)) {
            memberIsLandlineHolder = true;
            this.addAddressToBodyTextBlock(address);
        }
        address = ofNullable(spouse).map(Member::getAddress)
                                    .orElse(null);
        if (isNull(memberIsLandlineHolder) && nonNull(address)) {
            memberIsLandlineHolder = false;
            this.addAddressToBodyTextBlock(address);
        }

//  PRINT LANDLINE PHONE TO PDF
        float phoneFontSize = STANDARD_FONT_SIZE;
        final String memberMobilePhone = ofNullable(member.getPhones()).map(m -> m.get(PhoneType.MOBILE))
                                                                       .orElse(null);
        if (nonNull(memberMobilePhone)) {
            final float fontSize = this.getColumnFittedFontSize(memberMobilePhone, STANDARD_FONT, STANDARD_FONT_SIZE);
            if (fontSize < phoneFontSize) {
                phoneFontSize = fontSize;
            }
        }
        final String spouseMobilePhone = ofNullable(spouse).map(Member::getPhones)
                                                           .map(m -> m.get(PhoneType.MOBILE))
                                                           .orElse(null);
        if (nonNull(spouseMobilePhone)) {
            final float fontSize = this.getColumnFittedFontSize(spouseMobilePhone, STANDARD_FONT, STANDARD_FONT_SIZE);
            if (fontSize < phoneFontSize) {
                phoneFontSize = fontSize;
            }
        }
        if (nonNull(memberIsLandlineHolder)) {
            final Member landLineHolder = memberIsLandlineHolder
                    ? member
                    : spouse;
            final String landlinePhone = ofNullable(landLineHolder.getPhones()).map(m -> m.get(PhoneType.LANDLINE))
                                                                               .orElse(null);
            if (nonNull(landlinePhone)) {
                final float fontSize = this.getColumnFittedFontSize(landlinePhone, STANDARD_FONT, STANDARD_FONT_SIZE);
                if (fontSize < phoneFontSize) {
                    phoneFontSize = fontSize;
                }
                this.contents.setFont(STANDARD_FONT, phoneFontSize);
                this.addColumnAgnosticText(landlinePhone);
                this.newLine(STANDARD_LINE_SPACING);
            }
        }

//  PRINT MEMBER AND SPOUSE EMAILS (MEMBER FIRST)
        float emailFontSize = STANDARD_FONT_SIZE;
        final String memberEmail = member.getEmail();
        if (nonNull(memberEmail)) {
            final float fontSize = this.getColumnFittedFontSize(memberEmail, STANDARD_FONT, STANDARD_FONT_SIZE);
            if (fontSize < emailFontSize) {
                emailFontSize = fontSize;
            }
        }
        final String spouseEmail = ofNullable(spouse).map(Member::getEmail)
                                                     .orElse(null);
        if (nonNull(spouseEmail)) {
            final float fontSize = this.getColumnFittedFontSize(spouseEmail, STANDARD_FONT, STANDARD_FONT_SIZE);
            if (fontSize < emailFontSize) {
                emailFontSize = fontSize;
            }
        }
        if (nonNull(memberEmail)) {
            this.addEmailToBodyTextBlock(STANDARD_FONT, emailFontSize, memberEmail, null);
        }
        if (nonNull(spouseEmail)) {
            this.addEmailToBodyTextBlock(STANDARD_FONT, emailFontSize, spouseEmail, null);
        }

//  PRINT MEMBER AND SPOUSE MOBILE PHONES (MEMBER FIRST)
        if (nonNull(memberMobilePhone)) {
            this.contents.setFont(STANDARD_FONT, phoneFontSize);
            this.addColumnAgnosticText(memberMobilePhone);
            this.newLine(STANDARD_LINE_SPACING);
        }
        if (nonNull(spouseMobilePhone)) {
            this.contents.setFont(STANDARD_FONT, phoneFontSize);
            this.addColumnAgnosticText(spouseMobilePhone);
            this.newLine(STANDARD_LINE_SPACING);
        }

//  PRINT DEAD-END DESCENDANTS TO PDF
        if (nonNull(deadEndDescendants)) {
            for (final Member desc : deadEndDescendants) {
//          ADD DESCENDANT NAME
                final String header = "%c %s".formatted(BULLET, (nonNull(desc.getDeathday()))
                        ? "%c%s".formatted(DAGGER, desc.getDisplayName())
                        : desc.getDisplayName());
                final float headerFontSize = this.getColumnFittedFontSize(header, TITLE_FONT, STANDARD_FONT_SIZE);
                this.contents.setFont(TITLE_FONT, headerFontSize);
                this.addColumnAgnosticText(header);
                this.newLine(STANDARD_LINE_SPACING);

                this.addMemberDatesToBodyTextBlock(desc, TAB);

//          ADD DESCENDANT EMAIL
                final String email = desc.getEmail();
                if (nonNull(email)) {
                    final String displayEmail = "%s%s".formatted(TAB, email);
                    final float fontSize = this.getColumnFittedFontSize(displayEmail, STANDARD_FONT, STANDARD_FONT_SIZE);
                    this.addEmailToBodyTextBlock(STANDARD_FONT, fontSize, email, displayEmail);
                }

//          ADD DESCENDANT MOBILE PHONE
                final String descMobilePhone = ofNullable(desc.getPhones()).map(m -> m.get(PhoneType.MOBILE))
                                                                           .map(phone -> TAB + phone)
                                                                           .orElse(null);
                if (nonNull(descMobilePhone)) {
                    final float fontSize = this.getColumnFittedFontSize(descMobilePhone, STANDARD_FONT, STANDARD_FONT_SIZE);
                    this.contents.setFont(STANDARD_FONT, fontSize);
                    this.addColumnAgnosticText(descMobilePhone);
                    this.newLine(STANDARD_LINE_SPACING);
                }
            }
        }

        this.newLine(HALF_LINE_SPACING);
    }

    @Override
    protected
    int maxColumns () {
        return 4;
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
        final List<String> address = member.getAddress();
        if (nonNull(address)) {
            blockSizeYOffset += STANDARD_LINE_SPACING * address.size();
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
            this.addColumnCenteredText(header, TITLE_FONT, STANDARD_FONT_SIZE);
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
            this.addColumnCenteredText(header, TITLE_FONT, STANDARD_FONT_SIZE);
            this.newLine(STANDARD_LINE_SPACING);
        } else {
            final String memberName = (nonNull(member.getDeathday()))
                    ? "%c%s".formatted(DAGGER, member.getDisplayName())
                    : member.getDisplayName();
            final String spouseName = (nonNull(spouse.getDeathday()))
                    ? "%c%s".formatted(DAGGER, spouse.getDisplayName())
                    : spouse.getDisplayName();
            final Map.Entry<String, String> header = (PDPageHelperModel.getTextWidth(TITLE_FONT, STANDARD_FONT_SIZE, spouseName) >
                                                      PDPageHelperModel.getTextWidth(TITLE_FONT, STANDARD_FONT_SIZE, memberName))
                    ? Map.entry("%s &".formatted(memberName), spouseName)
                    : Map.entry(memberName, "& %s".formatted(spouseName));
            this.addColumnCenteredText(header.getKey(), TITLE_FONT, STANDARD_FONT_SIZE);
            this.newLine(STANDARD_LINE_SPACING);
            this.addColumnCenteredText(header.getValue(), TITLE_FONT, STANDARD_FONT_SIZE);
            this.newLine(STANDARD_LINE_SPACING);
        }

        this.addMemberDatesToBodyTextBlock(member, "");

        if (nonNull(spouse)) {
            this.addMemberDatesToBodyTextBlock(spouse, "");
        }
    }

    private
    void addMemberDatesToBodyTextBlock (final @NotNull Member member, final @NotNull String prefix) throws IOException {
//  ADD MEMBER BIRTHDAY
        this.addColumnRightJustifiedText("%s%s".formatted(prefix, member.getBirthday()
                                                                        .format(DISPLAY_DATE_FORMATTER)), STANDARD_FONT, STANDARD_FONT_SIZE);
        this.newLine(STANDARD_LINE_SPACING);

//  ADD MEMBER DEATHDAY
        final LocalDate memberDeathday = member.getDeathday();
        if (nonNull(memberDeathday)) {
            this.addColumnRightJustifiedText("%s%c%s".formatted(prefix, DAGGER, memberDeathday.format(DISPLAY_DATE_FORMATTER)), STANDARD_FONT, STANDARD_FONT_SIZE);
            this.newLine(STANDARD_LINE_SPACING);
        }
    }

    private
    void addAddressToBodyTextBlock (final @NotNull List<String> address) throws IOException {
        float addressFontSize = STANDARD_FONT_SIZE;
        for (final String line : address) {
            final float fontSize = this.getColumnFittedFontSize(line, STANDARD_FONT, STANDARD_FONT_SIZE);
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
    void addEmailToBodyTextBlock (final PDFont font, final float fontSize, final @NotNull String email, @Nullable String display) throws IOException {
        if (isNull(display)) {
            display = requireNonNull(email);
        }

        // append display text to pdf
        this.contents.setFont(font, fontSize);
        this.addColumnAgnosticText(display);

        // create annotation object for mailto link
        final PDAnnotationLink mailToLink = new PDAnnotationLink();
        mailToLink.setBorderStyle(INVISIBLE_BORDER);

        // create clickable area on pdf
        final PDRectangle mailToLinkBox = new PDRectangle();
        mailToLinkBox.setLowerLeftX(this.location.x);
        mailToLinkBox.setLowerLeftY(this.location.y - getAbsoluteFontDescentFromBaseline(font, fontSize));
        mailToLinkBox.setUpperRightX(this.location.x + getTextWidth(font, fontSize, display));
        mailToLinkBox.setUpperRightY(this.location.y + getAbsoluteFontAscentFromBaseline(font, fontSize));

        // clickable area attached to annotation object
        mailToLink.setRectangle(mailToLinkBox);

        // create action for when clicked
        final PDActionURI mailToLinkHref = new PDActionURI();
        mailToLinkHref.setURI("mailto:%s".formatted(email));

        // attach action to annotation object
        mailToLink.setAction(mailToLinkHref);

        // add annotations to page
        final List<PDAnnotation> pageAnnotations = new ArrayList<>(this.page.getAnnotations());
        pageAnnotations.add(mailToLink);
        this.page.setAnnotations(pageAnnotations);

        this.newLine(STANDARD_LINE_SPACING);
    }
}
