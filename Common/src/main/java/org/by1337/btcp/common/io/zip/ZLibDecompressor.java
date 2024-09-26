package org.by1337.btcp.common.io.zip;

import io.netty.buffer.ByteBuf;

import java.io.Closeable;
import java.util.zip.DataFormatException;

public interface ZLibDecompressor extends Closeable, AutoCloseable {
    /**
     * Decompresses data from one buffer to another.
     *
     * @param source the source data buffer.
     * @param out the buffer for decompressed data.
     * @param originalSize the original size of the data before compression.
     */
    void decompress(ByteBuf source, ByteBuf out, int originalSize) throws DataFormatException;
    /**
     * Releases resources associated with the decompressor.
     * It does not matter whether to call {@link ZLibCompressor#close()} or {@link ZLibCompressor#release()}.
     */
    void release();
}
