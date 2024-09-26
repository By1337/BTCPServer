package org.by1337.btcp.common.io.zip.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.zip.DataFormatException;

import static org.junit.jupiter.api.Assertions.*;

class JavaZLibTest {
    private JavaZLibCompressor compressor = new JavaZLibCompressor(9);
    private JavaZLibDecompressor decompressor = new JavaZLibDecompressor();

    @Test
    public void test() {
        ByteBuf source = createAndFill(2000);
        ByteBuf dest = Unpooled.directBuffer();
        ByteBuf decompressed = Unpooled.directBuffer();

        try {
            compressor.compress(source, dest);

            decompressor.decompress(dest, decompressed, source.readableBytes());

            assertArrayEquals(toByteArray(source), toByteArray(decompressed));
        } catch (DataFormatException e) {
            throw new RuntimeException(e);
        } finally {
            source.release();
            dest.release();
        }
    }

    private byte[] toByteArray(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    private ByteBuf createAndFill(int size) {
        ByteBuf buf = Unpooled.directBuffer(size);
        for (int i = 0; i < size; i++) {
            if (i % 2 == 0) {
                buf.writeByte(127);
            } else {
                buf.writeByte(-128);
            }
        }
        return buf;
    }
}