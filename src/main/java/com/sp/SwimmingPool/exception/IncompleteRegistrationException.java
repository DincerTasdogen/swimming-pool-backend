package com.sp.SwimmingPool.exception;

public class IncompleteRegistrationException extends AuthenticationException {
    public IncompleteRegistrationException(String message) {
        super(message, "REG_006");
    }
}
