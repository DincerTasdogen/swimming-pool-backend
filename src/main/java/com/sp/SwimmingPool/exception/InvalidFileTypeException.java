package com.sp.SwimmingPool.exception;

public class InvalidFileTypeException extends AuthenticationException {
    public InvalidFileTypeException(String message) {
        super(message, "REG_004");
    }
}
