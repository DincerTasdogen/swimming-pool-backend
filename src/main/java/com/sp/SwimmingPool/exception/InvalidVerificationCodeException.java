package com.sp.SwimmingPool.exception;

public class InvalidVerificationCodeException extends AuthenticationException {
    public InvalidVerificationCodeException(String message) {
        super(message, "REG_002");
    }
}
