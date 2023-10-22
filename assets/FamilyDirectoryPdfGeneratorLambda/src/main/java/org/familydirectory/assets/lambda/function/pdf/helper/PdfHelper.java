package org.familydirectory.assets.lambda.function.pdf.helper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;
import static java.util.Objects.nonNull;

public final
class PdfHelper {
    private static final char BULLET = '•';
    private static final String TAB = "    ";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    @NotNull
    private final PDDocument pdf = new PDDocument();
    @NotNull
    private final String date = LocalDate.now()
                                         .format(DATE_FORMATTER);
    @NotNull
    private final String title;
    private int pageNumber = 0;
    private PDPageHelper page = null;

    public
    PdfHelper (final @NotNull String rootMemberSurname) throws IOException {
        super();
        this.title = "%s FAMILY DIRECTORY".formatted(Optional.of(rootMemberSurname)
                                                             .filter(Predicate.not(String::isBlank))
                                                             .orElseThrow());
        this.newPage();
    }

    private
    void newPage () throws IOException {
        if (nonNull(this.page)) {
            this.page.close();
        }
        this.page = new PDPageHelper(this.pdf, new PDPage(), this.title, this.date, ++this.pageNumber);
    }

    public
    void addMember (final @NotNull Member member) throws IOException {
//        final List<String> text = new ArrayList<>();
//        text.add(member.getDisplayName());
//        ofNullable(member.getAddress()).ifPresent(text::addAll);
//        ofNullable(member.getPhones()).ifPresent(m -> m.forEach((k, v) -> text.add("%s".formatted(v))));
//        ofNullable(member.getEmail()).ifPresent(text::add);
//        this.page.addBodyTextBlock(text);
//        this.page.addMember(member);
    }

    public
    void addTestText () throws IOException {
//        final List<String> testList = new ArrayList<>();
//        final StringBuilder stringBuilder = new StringBuilder();
//        for (int i = 0; i < 100; ++i) {
//            stringBuilder.append('a');
//            testList.add(stringBuilder.toString());
//        }
//        for (int i = 1; i <= 4; ++i) {
//            this.page.addName("Wmmmmmmmmmmmmmmm Sr.", i);
//        }
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
