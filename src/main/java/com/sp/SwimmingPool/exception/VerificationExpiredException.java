package com.sp.SwimmingPool.exception;

public class VerificationExpiredException extends AuthenticationException {
    public VerificationExpiredException(String message) {
        super(message, "REG_003");
    }
}