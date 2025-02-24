package com.sp.SwimmingPool.exception;

public class OAuth2AuthenticationException extends AuthenticationException {
    public OAuth2AuthenticationException(String message) {
        super(message, "OAUTH_001");
    }
}