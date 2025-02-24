package com.sp.SwimmingPool.exception;

public class FileSizeLimitExceededException extends AuthenticationException {
    public FileSizeLimitExceededException(String message) {
        super(message, "REG_005");
    }
}
