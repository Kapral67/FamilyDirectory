package org.familydirectory.assets.lambda.function.stream.IO;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;

public final
class S3ByteArrayOutputStream extends OutputStream {
    private static final int CHUNK_SIZE = 8192; // bytes
    private volatile byte[] buf;
    private volatile int count = 0;
    private volatile boolean closed = false;

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
        if (this.count >= CHUNK_SIZE) {
            this.buf = Arrays.copyOf(this.buf, this.buf.length + CHUNK_SIZE);
        }
    }

    private
    void validateStream () throws IOException {
        if (this.closed) {
            throw new IOException("Stream is closed");
        }
    }

    @Override
    public synchronized
    void close () {
        this.closed = true;
        if (this.buf.length > this.count) {
            this.buf = Arrays.copyOf(this.buf, this.count);
        }
    }

    public synchronized @NotNull
    RequestBody requestBody () throws IOException {
        this.validateStream();
        this.close();
        return RequestBody.fromContentProvider(ContentStreamProvider.fromByteArrayUnsafe(this.buf), this.buf.length, "application/octet-stream");
    }
}
