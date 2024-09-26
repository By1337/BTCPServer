package org.by1337.btcp.common.io.zip;

import io.netty.buffer.ByteBuf;

import java.util.zip.ZipException;

public interface ZLibCompressor extends Cloneable, AutoCloseable {
    /**
     * Compresses data from one buffer to another.
     *
     * @param source the source data buffer.
     * @param out    the buffer for compressed data.
     */
    void compress(ByteBuf source, ByteBuf out) throws ZipException;

    /**
     * Releases resources associated with the compressor.
     * It does not matter whether to call {@link ZLibCompressor#close()} or {@link ZLibCompressor#release()}.
     */
    void release();

}
