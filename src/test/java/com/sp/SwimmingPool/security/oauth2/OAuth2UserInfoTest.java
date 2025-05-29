package com.sp.SwimmingPool.security.oauth2;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class OAuth2UserInfoTest {

    @Test
    void googleOAuth2UserInfo_shouldExtractCorrectly() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google123");
        attributes.put("name", "Google User");
        attributes.put("email", "google@example.com");
        attributes.put("picture", "http://example.com/pic.jpg");

        GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);

        assertEquals("google123", userInfo.getId());
        assertEquals("Google User", userInfo.getName());
        assertEquals("google@example.com", userInfo.getEmail());
        assertSame(attributes, userInfo.getAttributes());
    }

    @Test
    void githubOAuth2UserInfo_shouldExtractCorrectly() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 98765); // GitHub uses integer id
        attributes.put("name", "GitHub User");
        attributes.put("email", "github@example.com");
        attributes.put("login", "githubuser");

        GithubOAuth2UserInfo userInfo = new GithubOAuth2UserInfo(attributes);

        assertEquals("98765", userInfo.getId()); // Note: converted to String
        assertEquals("GitHub User", userInfo.getName());
        assertEquals("github@example.com", userInfo.getEmail());
        assertSame(attributes, userInfo.getAttributes());
    }

    @Test
    void githubOAuth2UserInfo_shouldHandleNullNameAndEmail() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 112233);
        attributes.put("login", "githubNoNameNoEmail");
        // Name and email are null

        GithubOAuth2UserInfo userInfo = new GithubOAuth2UserInfo(attributes);

        assertEquals("112233", userInfo.getId());
        assertNull(userInfo.getName());
        assertNull(userInfo.getEmail());
    }
}