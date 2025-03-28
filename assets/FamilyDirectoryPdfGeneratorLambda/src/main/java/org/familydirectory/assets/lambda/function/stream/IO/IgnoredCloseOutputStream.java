package org.familydirectory.assets.lambda.function.stream.IO;

import java.io.IOException;
import java.io.OutputStream;
import org.jetbrains.annotations.NotNull;

public final
class IgnoredCloseOutputStream extends OutputStream {
    public final OutputStream delegate;

    private
    IgnoredCloseOutputStream () {
        this(OutputStream.nullOutputStream());
    }

    public
    IgnoredCloseOutputStream (final OutputStream delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public
    void write (final int b) throws IOException {
        this.delegate.write(b);
    }

    @Override
    public
    void write (final byte @NotNull [] b) throws IOException {
        this.delegate.write(b);
    }

    @Override
    public
    void write (final byte @NotNull [] b, final int off, final int len) throws IOException {
        this.delegate.write(b, off, len);
    }

    @Override
    public
    void flush () throws IOException {
        this.delegate.flush();
    }

    @Override
    public
    void close () {
    }
}
