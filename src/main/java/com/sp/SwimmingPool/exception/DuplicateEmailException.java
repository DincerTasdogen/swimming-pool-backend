package com.sp.SwimmingPool.exception;

public class DuplicateEmailException extends AuthenticationException {
    public DuplicateEmailException(String message) {
        super(message, "REG_001");
    }
}
