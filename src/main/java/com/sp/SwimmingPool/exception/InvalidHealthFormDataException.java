package com.sp.SwimmingPool.exception;

public class InvalidHealthFormDataException extends AuthenticationException {
    public InvalidHealthFormDataException(String message) {
        super(message, "REG_007");
    }
}
