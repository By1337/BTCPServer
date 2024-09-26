package org.by1337.btcp.common.io.zip.natives;

import io.netty.buffer.ByteBuf;
import org.by1337.btcp.common.io.zip.ZLibCompressor;

import java.util.zip.ZipException;

/**
 * Compressor for data compression using the libdeflate and zlib libraries.
 * <p>
 * This class provides a compression mechanism via JNI calls to the native library.
 * When using this compressor, remember to release resources by calling {@link #close()},
 * or use it within a try-with-resources block.
 * @see <a href="https://github.com/ebiggers/libdeflate">libdeflate</a>
 */
public class LibdeflateZLibCompressor implements ZLibCompressor {
    /**
     * The minimum zlib overhead is 8 bytes.
     * <p>
     * If there is not enough space in the output buffer, compression will fail,
     * so 50 extra bytes are added in case the size of the data increases after compression.
     * <p>
     * The value of 50 was chosen arbitrarily and can be increased if necessary.
     */
    private static final int ZLIB_OVERHEAD = 50;
    /**
     * Pointer to the native compressor object created via JNI.
     */
    private final long compressorPtr;
    /**
     * Indicates whether the compressor has been closed.
     */
    private volatile boolean closed;

    /**
     * Constructor that creates a new compressor with the specified compression level.
     *
     * @param lvl the compression level (from 1 to 12).
     * @throws IllegalArgumentException if the compression level is out of the valid range.
     */
    public LibdeflateZLibCompressor(int lvl) {
        if (lvl < 1 || lvl > 12) throw new IllegalArgumentException("Unsupported level: " + lvl);
        compressorPtr = allocCompressor(lvl);
    }

    /**
     * Allocates resources for the compressor on the native side.
     *
     * @param lvl the compression level.
     * @return a pointer to the native compressor object.
     */
    private native long allocCompressor(int lvl);

    /**
     * Releases resources associated with the compressor on the native side.
     *
     * @param compressorPtr pointer to the native compressor object.
     */
    private native void freeCompressor(long compressorPtr);

    /**
     * Native method for compressing data.
     *
     * @param compressorPtr pointer to the compressor.
     * @param sourceAddress address of the source data.
     * @param sourceLength length of the source data.
     * @param destAddress address of the buffer for compressed data.
     * @param destLength length of the buffer for compressed data.
     * @return the number of compressed bytes or 0 if the destination buffer was too small.
     */
    private native long compress0(long compressorPtr, long sourceAddress, int sourceLength, long destAddress, int destLength);


    /**
     * Compresses data from one buffer to another.
     *
     * @param source the source data buffer.
     * @param out the buffer for compressed data.
     * @throws IllegalStateException if the compressor is already closed.
     * @throws IllegalArgumentException if the buffers do not support direct memory access.
     * @throws ZipException if there is insufficient space in the output buffer for compressed data.
     */
    @Override
    public void compress(ByteBuf source, ByteBuf out) throws ZipException {
        if (closed) throw new IllegalStateException("Compressor closed");
        if (!source.hasMemoryAddress()) {
            throw new IllegalArgumentException("The source buffer does not support direct memory access");
        }
        if (!out.hasMemoryAddress()) {
            throw new IllegalArgumentException("The output buffer does not support direct memory access");
        }

        if (out.writableBytes() < source.readableBytes() + ZLIB_OVERHEAD) {
            out.ensureWritable(source.readableBytes() + ZLIB_OVERHEAD);
        }
        long produced = compress0(
                compressorPtr,
                source.memoryAddress() + source.readerIndex(),
                source.readableBytes(),
                out.memoryAddress() + out.writerIndex(),
                out.writableBytes()
        );
        if (produced == 0) {
            throw new ZipException("Insufficient space in the output buffer for compressed data!");
        }
        out.writerIndex((int) (out.writerIndex() + produced));
    }


    /**
     * Releases resources associated with the compressor.
     */
    public void release() {
        if (closed) return;
        closed = true;
        freeCompressor(compressorPtr);
    }

    /**
     * Closes the compressor and releases resources.
     */
    @Override
    public void close() {
        release();
    }
}
