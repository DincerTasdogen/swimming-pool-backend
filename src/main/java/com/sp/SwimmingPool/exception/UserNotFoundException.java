package com.sp.SwimmingPool.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {
    private final String errorCode;

    public UserNotFoundException(String email) {
        super("User not found with email: " + email);
        this.errorCode = "AUTH_001";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
