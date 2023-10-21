package org.familydirectory.assets.lambda.function.pdf.helper;

import java.io.Closeable;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.jetbrains.annotations.NotNull;

final
class PDPageHelper implements Closeable {
    final @NotNull Location location = new Location(0.0f, 0.0f);
    private final @NotNull PDPageContentStream contents;
    private final @NotNull PDPage page;

    PDPageHelper (final @NotNull PDDocument pdf, final @NotNull PDPage page) throws IOException {
        super();
        this.page = page;
        pdf.addPage(this.page);
        this.contents = new PDPageContentStream(pdf, this.page);
    }

    float centerY () {
        return this.height() * 0.5f;
    }

    float height () {
        return this.page.getMediaBox()
                        .getHeight();
    }

    float centerX () {
        return this.width() * 0.5f;
    }

    float width () {
        return this.page.getMediaBox()
                        .getWidth();
    }

    void addLine (final @NotNull String line) throws IOException {
        this.contents.beginText();
        this.contents.newLineAtOffset(this.location.x, this.location.y);
        this.contents.showText(line);
        this.contents.endText();
    }

    @NotNull
    PDPageContentStream contents () {
        return this.contents;
    }

    @Override
    public
    void close () throws IOException {
        this.contents.close();
    }

    static final
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
