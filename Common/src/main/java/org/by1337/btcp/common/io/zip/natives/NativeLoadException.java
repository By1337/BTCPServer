package org.by1337.btcp.common.io.zip.natives;

public class NativeLoadException extends RuntimeException{
    public NativeLoadException() {
    }

    public NativeLoadException(String message) {
        super(message);
    }

    public NativeLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
