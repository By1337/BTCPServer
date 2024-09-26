package org.by1337.btcp.common.io.zip.natives;

/**
 * Status codes for data decompression using libdeflate.
 */
public enum LibdeflateDecompressionStatus {
    /**
     * Decompression was successful.
     */
    LIBDEFLATE_SUCCESS(0),
    /**
     * Decompression failed because the compressed data was invalid,
     * corrupt, or otherwise unsupported.
     */
    LIBDEFLATE_BAD_DATA(1),
    /**
     * A NULL 'actual_out_nbytes_ret' was provided, but the data would have
     * * decompressed to fewer than 'out_nbytes_avail' bytes.
     */
    LIBDEFLATE_SHORT_OUTPUT(2),
    /**
     * The data would have decompressed to more than 'out_nbytes_avail'
     * bytes.
     */
    LIBDEFLATE_INSUFFICIENT_SPACE(3),
    /**
     * Libdeflate returned an unknown status
     */
    UNKNOWN_STATUS(-1);
    private final int id;

    LibdeflateDecompressionStatus(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static LibdeflateDecompressionStatus fromId(int id) {
        return switch (id) {
            case 0 -> LIBDEFLATE_SUCCESS;
            case 1 -> LIBDEFLATE_BAD_DATA;
            case 2 -> LIBDEFLATE_SHORT_OUTPUT;
            case 3 -> LIBDEFLATE_INSUFFICIENT_SPACE;
            default -> UNKNOWN_STATUS;
        };
    }
}
