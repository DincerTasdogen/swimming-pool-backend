package com.sp.SwimmingPool.exception;

public class OAuth2RegistrationException extends AuthenticationException {
    public OAuth2RegistrationException(String message) {
        super(message, "OAUTH_002");
    }
}
