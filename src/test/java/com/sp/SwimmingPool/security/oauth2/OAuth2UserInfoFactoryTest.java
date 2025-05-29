package com.sp.SwimmingPool.security.oauth2;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OAuth2UserInfoFactoryTest {

    private final Map<String, Object> dummyAttributes = Collections.singletonMap("key", "value");

    @Test
    void getOAuth2UserInfo_shouldReturnGoogleUserInfo_forGoogleRegistrationId() {
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo("google", dummyAttributes);
        assertTrue(userInfo instanceof GoogleOAuth2UserInfo);
    }

    @Test
    void getOAuth2UserInfo_shouldReturnGoogleUserInfo_forGoogleRegistrationIdCaseInsensitive() {
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo("GoOgLe", dummyAttributes);
        assertTrue(userInfo instanceof GoogleOAuth2UserInfo);
    }

    @Test
    void getOAuth2UserInfo_shouldReturnGithubUserInfo_forGithubRegistrationId() {
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo("github", dummyAttributes);
        assertTrue(userInfo instanceof GithubOAuth2UserInfo);
    }

    @Test
    void getOAuth2UserInfo_shouldReturnGithubUserInfo_forGithubRegistrationIdCaseInsensitive() {
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo("GiThUb", dummyAttributes);
        assertTrue(userInfo instanceof GithubOAuth2UserInfo);
    }

    @Test
    void getOAuth2UserInfo_shouldThrowException_forUnsupportedRegistrationId() {
        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class, () -> {
            OAuth2UserInfoFactory.getOAuth2UserInfo("facebook", dummyAttributes);
        });
        assertEquals(null, exception.getMessage());
    }
}