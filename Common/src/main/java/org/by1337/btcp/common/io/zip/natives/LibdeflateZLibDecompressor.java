package org.by1337.btcp.common.io.zip.natives;

import io.netty.buffer.ByteBuf;
import org.by1337.btcp.common.io.zip.ZLibDecompressor;

import java.util.zip.DataFormatException;

/**
 * Decompressor for extracting data using the libdeflate and zlib libraries.
 * <p>
 * This class provides a decompression mechanism via JNI calls to the native library.
 * It is necessary to manually release resources by calling {@link #close()},
 * or use the object in a try-with-resources block.
 * @see <a href="https://github.com/ebiggers/libdeflate">libdeflate</a>
 */
public class LibdeflateZLibDecompressor implements ZLibDecompressor {

    /**
     * Pointer to the native decompressor object.
     */
    private final long deompressorPtr;
    /**
     * Indicates whether the decompressor has been closed.
     */
    private volatile boolean closed;

    /**
     * Constructor that creates a new decompressor.
     */
    public LibdeflateZLibDecompressor() {
        deompressorPtr = allocDecompressor();
    }

    /**
     * Allocates resources for the decompressor on the native side.
     *
     * @return a pointer to the native decompressor object.
     */
    private native long allocDecompressor();

    /**
     * Releases resources associated with the decompressor on the native side.
     *
     * @param compressorPtr pointer to the native decompressor object.
     */
    private native void freeDecompressor(long compressorPtr);

    /**
     * Native method for decompressing data.
     *
     * @param compressorPtr pointer to the decompressor.
     * @param sourceAddress address of the source data.
     * @param sourceLength length of the source data.
     * @param destAddress address of the buffer for decompressed data.
     * @param originalSize the original size of the data before compression.
     * @return the decompression status as an int.
     */
    public native int decompress0(long compressorPtr, long sourceAddress, int sourceLength,
                                  long destAddress, int originalSize);

    /**
     * Decompresses data from one buffer to another.
     *
     * @param source the source data buffer.
     * @param out the buffer for decompressed data.
     * @param originalSize the original size of the data before compression.
     * @throws DataFormatException if there is an error during decompression.
     * @throws IllegalStateException if the decompressor is already closed.
     * @throws IllegalArgumentException if the buffers do not support direct memory access.
     */
    @Override
    public void decompress(ByteBuf source, ByteBuf out, int originalSize) throws DataFormatException {
        if (closed) throw new IllegalStateException("Decompressor closed");
        if (!source.hasMemoryAddress()) {
            throw new IllegalArgumentException("The source buffer does not support direct memory access");
        }
        if (!out.hasMemoryAddress()) {
            throw new IllegalArgumentException("The output buffer does not support direct memory access");
        }
        if (out.writableBytes() < originalSize) {
            out.ensureWritable(out.writerIndex() + originalSize);
        }
        int statusInt = decompress0(
                deompressorPtr,
                source.memoryAddress() + source.readerIndex(),
                source.readableBytes(),
                out.memoryAddress() + out.writerIndex(),
                originalSize
        );
        LibdeflateDecompressionStatus status = LibdeflateDecompressionStatus.fromId(statusInt);
        switch (status) {
            case LIBDEFLATE_BAD_DATA -> throw new DataFormatException("The data is corrupted or unsupported");
            case LIBDEFLATE_SHORT_OUTPUT -> throw new DataFormatException("The size of the decompressed data is less than expected");
            case LIBDEFLATE_INSUFFICIENT_SPACE -> throw new DataFormatException("Insufficient space in the output buffer for decompressed data");
            case UNKNOWN_STATUS -> throw new IllegalStateException("Unknown decompression status: " + statusInt);
        }
        out.writerIndex(out.writerIndex() + originalSize);
    }

    /**
     * Releases resources associated with the decompressor.
     */
    public void release() {
        if (closed) return;
        freeDecompressor(deompressorPtr);
        closed = true;
    }

    /**
     * Closes the decompressor and releases resources.
     */
    @Override
    public void close() {
        release();
    }
}
