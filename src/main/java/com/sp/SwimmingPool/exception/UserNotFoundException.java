package com.sp.SwimmingPool.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends AuthenticationException {

    public UserNotFoundException(String email) {
        super("No account found with email: " + email, "AUTH_001");
    }

}
