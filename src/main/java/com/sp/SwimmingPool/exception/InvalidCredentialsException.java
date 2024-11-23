package com.sp.SwimmingPool.exception;

public class InvalidCredentialsException extends AuthenticationException {
    public InvalidCredentialsException() {
        super("Invalid email or password", "AUTH_002");
    }
}
