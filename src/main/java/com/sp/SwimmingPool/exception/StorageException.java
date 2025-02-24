package com.sp.SwimmingPool.exception;

public class StorageException extends RuntimeException {
    private final String errorCode;

    public StorageException(String message) {
        super(message);
        this.errorCode = "STORAGE_001";
    }

    public StorageException(String message, Throwable cause) {
        super(message + ": " + cause.getMessage(), cause);
        this.errorCode = "STORAGE_001";
    }

    public String getErrorCode() {
        return errorCode;
    }
}