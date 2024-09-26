package org.by1337.btcp.common.io.zip.impl;

import io.netty.buffer.ByteBuf;
import org.by1337.btcp.common.io.zip.ZLibCompressor;

import java.util.zip.Deflater;

public class JavaZLibCompressor implements ZLibCompressor {
    private final byte[] encodeBuf = new byte[8192];
    private volatile boolean closed;
    private final Deflater deflater;

    public JavaZLibCompressor(int lvl) {
        deflater = new Deflater(lvl);
    }

    @Override
    public void compress(ByteBuf source, ByteBuf out) {
        byte[] input = new byte[source.readableBytes()];
        int readIndex = source.readerIndex();
        source.readBytes(input);
        source.readerIndex(readIndex);

        deflater.setInput(input);
        deflater.finish();

        while (!deflater.finished()) {
            int read = this.deflater.deflate(this.encodeBuf);
            out.writeBytes(this.encodeBuf, 0, read);
        }
        deflater.reset();
    }

    @Override
    public void release() {
        if (closed) return;
        closed = true;
        deflater.end();
    }

    @Override
    public void close() throws Exception {

    }
}
