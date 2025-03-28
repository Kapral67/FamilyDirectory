package org.familydirectory.assets.lambda.function.stream.IO;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import static java.util.Objects.isNull;

public final
class S3ByteArrayOutputStream extends OutputStream {
    private static final int CHUNK_SIZE = 8192; // bytes
    private byte[] buf;
    private int count = 0;
    private boolean closed = false;
    private RequestBody requestBody = null;

    public
    S3ByteArrayOutputStream () {
        super();
        this.buf = new byte[CHUNK_SIZE];
    }

    @Override
    public synchronized
    void write (final int b) throws IOException {
        this.validateStream();
        this.buf[this.count++] = (byte) b;
        if (this.count >= this.buf.length) {
            this.buf = Arrays.copyOf(this.buf, this.buf.length + CHUNK_SIZE);
        }
    }

    @Override
    public synchronized
    void write (final byte @NotNull [] b, final int off, final int len) throws IOException {
        this.validateStream();
        Objects.checkFromIndexSize(off, len, b.length);
        if (this.count + len >= this.buf.length) {
            final int multiplier = (len / CHUNK_SIZE) + 1;
            this.buf = Arrays.copyOf(this.buf, this.buf.length + (CHUNK_SIZE * multiplier));
        }
        System.arraycopy(b, off, this.buf, this.count, len);
        this.count += len;
    }

    @Override
    public synchronized
    void close () {
        this.closed = true;
        if (this.buf.length > this.count) {
            this.buf = Arrays.copyOf(this.buf, this.count);
        }
    }

    private
    void validateStream () throws IOException {
        if (this.closed) {
            throw new IOException("Stream is closed");
        }
    }

    public synchronized @NotNull
    RequestBody requestBody () {
        this.close();
        if (isNull(this.requestBody)) {
            this.requestBody = RequestBody.fromContentProvider(ContentStreamProvider.fromByteArrayUnsafe(this.buf), this.buf.length, "application/octet-stream");
        }
        return this.requestBody;
    }
}
