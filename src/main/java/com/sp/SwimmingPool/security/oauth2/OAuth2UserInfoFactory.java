package com.sp.SwimmingPool.security.oauth2;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        switch(registrationId.toLowerCase()) {
            case "google":
                return new GoogleOAuth2UserInfo(attributes);
            case "github":
                return new GithubOAuth2UserInfo(attributes);
            default:
                throw new OAuth2AuthenticationException("Login with " + registrationId + " is not supported yet.");
        }
    }
}