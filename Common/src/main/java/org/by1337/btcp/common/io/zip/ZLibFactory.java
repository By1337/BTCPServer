package org.by1337.btcp.common.io.zip;

import org.by1337.btcp.common.io.zip.impl.JavaZLibCompressor;
import org.by1337.btcp.common.io.zip.impl.JavaZLibDecompressor;
import org.by1337.btcp.common.io.zip.natives.LibdeflateZLibCompressor;
import org.by1337.btcp.common.io.zip.natives.LibdeflateZLibDecompressor;
import org.by1337.btcp.common.io.zip.natives.NativeLoader;

/**
 * A factory class for creating instances of {@link ZLibCompressor} and {@link ZLibDecompressor}.
 * <p>
 * This class provides methods to create compressors and decompressors that either utilize the native
 * {@link LibdeflateZLibCompressor} and {@link LibdeflateZLibDecompressor} for potentially better performance,
 * or fall back to Java implementations ({@link JavaZLibCompressor} and {@link JavaZLibDecompressor})
 * if native support is not available or desired.
 * </p>
 */
public class ZLibFactory {

    /**
     * Creates a new {@link ZLibCompressor} with the specified compression level, using the native implementation if allowed.
     *
     * @param lvl the compression level, which should be between 1 and 12 for the native version and between 1 and 9 for the regular version.
     * @return a {@link ZLibCompressor} instance.
     */
    public static ZLibCompressor createCompressor(int lvl) {
        return createCompressor(lvl, true);
    }

    /**
     * Creates a new {@link ZLibCompressor} with the specified compression level.
     *
     * @param lvl the compression level, which should be between 1 and 12 for the native version and between 1 and 9 for the regular version.
     * @param allowNative if true, allows the use of the native compressor if available.
     * @return a {@link ZLibCompressor} instance.
     */
    public static ZLibCompressor createCompressor(int lvl, boolean allowNative) {
        if (!allowNative || !NativeLoader.NATIVE_IS_AVAILABLE) {
            return new JavaZLibCompressor(lvl);
        }
        return new LibdeflateZLibCompressor(lvl);
    }

    /**
     * Creates a new {@link ZLibDecompressor}, using the native implementation if allowed.
     *
     * @return a {@link ZLibDecompressor} instance.
     */
    public static ZLibDecompressor createDecompressor() {
        return createDecompressor(true);
    }

    /**
     * Creates a new {@link ZLibDecompressor}.
     *
     * @param allowNative if true, allows the use of the native decompressor if available.
     * @return a {@link ZLibDecompressor} instance.
     */
    public static ZLibDecompressor createDecompressor(boolean allowNative) {
        if (!allowNative || !NativeLoader.NATIVE_IS_AVAILABLE) {
            return new JavaZLibDecompressor();
        }
        return new LibdeflateZLibDecompressor();
    }
}
