package org.familydirectory.assets.lambda.function.pdf.helper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.familydirectory.assets.ddb.member.Member;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.nonNull;

public final
class PdfHelper {
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
                                                             .map(String::toUpperCase)
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
    void addFamily (final @NotNull Member member, final @Nullable Member spouse, final @Nullable List<Member> descendants, final boolean endOfSection) throws IOException {
        try {
            this.page.addBodyTextBlock(member, spouse, descendants, endOfSection);
        } catch (final PDPageHelper.NewPageException e) {
            this.newPage();
            try {
                this.page.addBodyTextBlock(member, spouse, descendants, endOfSection);
            } catch (final PDPageHelper.NewPageException x) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * TODO: DELETE THIS
     */
    public @NotNull
    PDDocument getPdf () throws IOException {
        this.page.close();
        return this.pdf;
    }
}
