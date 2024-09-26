package org.by1337.btcp.common.io.zip.natives;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.zip.DataFormatException;
import java.util.zip.ZipException;

import static org.junit.jupiter.api.Assertions.*;

class LibdeflateZLibTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibdeflateZLibTest.class);
    private LibdeflateZLibCompressor compressor;
    private LibdeflateZLibDecompressor decompressor;
    private final boolean doSkip;

    public LibdeflateZLibTest() {
        if (!NativeLoader.NATIVE_IS_AVAILABLE) {
            doSkip = true;
            LOGGER.warn("Native library not set up! Skip tests");
            return;
        }
        doSkip = false;
        compressor = new LibdeflateZLibCompressor(12);
        decompressor = new LibdeflateZLibDecompressor();
    }


    @Test
    public void test() {
        if (doSkip) return;
        ByteBuf source = createAndFill(2000);
        ByteBuf dest = Unpooled.directBuffer();
        ByteBuf decompressed = Unpooled.directBuffer();

        try {
            compressor.compress(source, dest);

            decompressor.decompress(dest, decompressed, source.readableBytes());

            assertArrayEquals(toByteArray(source), toByteArray(decompressed));
        } catch (DataFormatException | ZipException e) {
            throw new RuntimeException(e);
        } finally {
            source.release();
            dest.release();
            decompressed.release();
        }
    }

    @AfterEach
    public void close() {
        if (compressor != null) compressor.release();
        if (decompressor != null) decompressor.release();
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