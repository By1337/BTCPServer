package org.by1337.btcp.server.service;

public class InvalidServiceException extends Exception {
    public InvalidServiceException() {
    }

    public InvalidServiceException(String message) {
        super(message);
    }

    public InvalidServiceException(String message, Object... objects) {
        super(String.format(message, objects));
    }

    public InvalidServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidServiceException(String message, Throwable cause, Object... objects) {
        super(String.format(message, objects), cause);
    }


    public InvalidServiceException(Throwable cause) {
        super(cause);
    }

}
