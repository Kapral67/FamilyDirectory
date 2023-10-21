package org.familydirectory.assets.lambda.function.pdf.helper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.jetbrains.annotations.NotNull;

public final
class PdfHelper {
    private static final char BULLET = '•';
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");
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
        final String title = "%s FAMILY DIRECTORY".formatted(this.rootMemberSurname.toUpperCase());
        this.page.addTitle(title);
        this.page.addSubtitle(this.date);
        this.page.addTopLine();
        this.page.addBottomLine();
        this.page.addColumnLines();
        this.page.addPageNumber(++this.pageNumber);
        this.page.initBody();
    }

//    public
//    void addFamily (final @NotNull Member member, final @NotNull Member spouse) throws IOException {
//        // TODO: CHECK IF NEED NEW COLUMN
//        // TODO: CHECK IF NEED NEW PAGE
//
//        final float namesWidth;
//
//        { // NAMES
//            this.page.contents()
//                     .setFont(BOLD_FONT, STANDARD_FONT_SIZE);
//            final String names = "%s & %s".formatted(member.getDisplayName(), spouse.getDisplayName());
//            namesWidth = getTextWidth(BOLD_FONT, STANDARD_FONT_SIZE, names);
//        }
//    }

    /**
     * TODO: DELETE THIS
     */
    public @NotNull
    PDDocument getPdf () throws IOException {
        this.page.close();
        return this.pdf;
    }
}
