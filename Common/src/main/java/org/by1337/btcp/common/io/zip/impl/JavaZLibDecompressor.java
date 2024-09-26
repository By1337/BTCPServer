package org.by1337.btcp.common.io.zip.impl;

import io.netty.buffer.ByteBuf;
import org.by1337.btcp.common.io.zip.ZLibDecompressor;

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class JavaZLibDecompressor implements ZLibDecompressor {
    private final Inflater inflater;
    private volatile boolean closed;

    public JavaZLibDecompressor() {
        this.inflater = new Inflater();
    }

    @Override
    public void decompress(ByteBuf source, ByteBuf out, int originalSize) throws DataFormatException {
        byte[] compressed = new byte[source.readableBytes()];
        int readIndex = source.readerIndex();
        source.readBytes(compressed);
        source.readerIndex(readIndex);
        inflater.setInput(compressed);
        byte[] result = new byte[originalSize];
        inflater.inflate(result);
        out.writeBytes(result);
        this.inflater.reset();
    }

    @Override
    public void release() {
        if (closed) return;
        closed = true;
        inflater.end();
    }

    @Override
    public void close() throws IOException {

    }
}
