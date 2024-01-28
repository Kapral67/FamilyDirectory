package org.familydirectory.assets.lambda.function.stream.helper;

import java.io.IOException;
import java.time.LocalDate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.familydirectory.assets.lambda.function.stream.helper.models.IPDPageHelper;
import org.jetbrains.annotations.NotNull;
import static java.util.Objects.requireNonNull;

final
class PDBirthdayPageHelper implements IPDPageHelper {
    private final @NotNull Location location = new Location(0.0f, 0.0f);
    private final @NotNull PDPageContentStream contents;
    private final @NotNull PDPage page;
    private final float bodyContentStartY;
    private int currentColumn = 1;

    PDBirthdayPageHelper (final @NotNull PDDocument pdf, final @NotNull PDPage page, final @NotNull String title, final @NotNull LocalDate subtitle, final int pageNumber) throws IOException {
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

    @Override
    @NotNull
    public
    PDPage getPage () {
        return this.page;
    }

    @Override
    public
    int getMaxColumns () {
        return 3;
    }

    @Override
    @NotNull
    public
    Location getLocation () {
        return this.location;
    }

    @Override
    @NotNull
    public
    PDPageContentStream getContents () {
        return this.contents;
    }

    @Override
    public
    float getBodyContentStartY () {
        return this.bodyContentStartY;
    }

    @Override
    public
    int getCurrentColumn () {
        return this.currentColumn;
    }

    @Override
    public
    void setCurrentColumn (final int col) {
        if (col < 1 || col > this.getMaxColumns()) {
            throw new IllegalArgumentException("%d is must be in [1, %d]".formatted(col, this.getMaxColumns()));
        }
        this.currentColumn = col;
    }
}
