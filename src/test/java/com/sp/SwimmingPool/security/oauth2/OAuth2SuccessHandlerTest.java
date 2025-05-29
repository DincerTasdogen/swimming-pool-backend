package com.sp.SwimmingPool.security.oauth2;

import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Authentication authentication;
    @Mock
    private RedirectStrategy redirectStrategy;

    @InjectMocks
    private OAuth2SuccessHandler successHandler;

    @Captor
    private ArgumentCaptor<Cookie> cookieCaptor;
    @Captor
    private ArgumentCaptor<String> redirectUrlCaptor;

    private final String baseRedirectUri = "http://localhost:3000";
    private final String cookieName = "authCookie";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(successHandler, "redirectUri", baseRedirectUri);
        ReflectionTestUtils.setField(successHandler, "cookieName", cookieName);
        successHandler.setRedirectStrategy(redirectStrategy); // Important for SimpleUrlAuthenticationSuccessHandler
    }

    private UserPrincipal createUserPrincipal(Integer id, String email, String name, Map<String, Object> attributes) {
        return UserPrincipal.builder()
                .id(id)
                .email(email)
                .name(name)
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .attributes(attributes)
                .build();
    }

    @Test
    void onAuthenticationSuccess_existingUser_shouldRedirectToDashboardAndSetCookie() throws IOException, ServletException {
        UserPrincipal userPrincipal = createUserPrincipal(1, "existing@example.com", "Existing User", null);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        String mockToken = "mock.jwt.token";
        when(tokenProvider.generateToken(authentication)).thenReturn(mockToken);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).addCookie(cookieCaptor.capture());
        Cookie capturedCookie = cookieCaptor.getValue();
        assertEquals(cookieName, capturedCookie.getName());
        assertEquals(mockToken, capturedCookie.getValue());
        assertEquals("/", capturedCookie.getPath());
        assertTrue(capturedCookie.isHttpOnly());
        assertEquals(3600, capturedCookie.getMaxAge());

        verify(redirectStrategy).sendRedirect(eq(request), eq(response), redirectUrlCaptor.capture());
        assertEquals(baseRedirectUri + "/member/dashboard", redirectUrlCaptor.getValue());
    }

    @Test
    void onAuthenticationSuccess_newUserGoogle_shouldRedirectToRegisterWithParams() throws IOException, ServletException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "newuser@google.com");
        attributes.put("name", "New Google User");
        attributes.put("sub", "google-oauth-sub"); // Indicates Google

        UserPrincipal userPrincipal = createUserPrincipal(null, null, null, attributes); // ID is null for new user
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(redirectStrategy).sendRedirect(eq(request), eq(response), redirectUrlCaptor.capture());
        String targetUrl = redirectUrlCaptor.getValue();
        assertTrue(targetUrl.startsWith(baseRedirectUri + "/register?"));
        assertTrue(targetUrl.contains("email=" + URLEncoder.encode("newuser@google.com", StandardCharsets.UTF_8)));
        assertTrue(targetUrl.contains("provider=google"));
        assertTrue(targetUrl.contains("name=" + URLEncoder.encode("New", StandardCharsets.UTF_8))); // First part of name
        assertTrue(targetUrl.contains("surname=" + URLEncoder.encode("Google User", StandardCharsets.UTF_8))); // Second part
    }

    @Test
    void onAuthenticationSuccess_newUserGithub_noEmailInPrincipal_shouldRedirectToRegisterWithPlaceholderEmail() throws IOException, ServletException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", "New GithubUser"); // Single name part
        attributes.put("id", 12345L); // Indicates Github
        attributes.put("login", "githublogin");
        // No email in attributes directly

        UserPrincipal userPrincipal = createUserPrincipal(null, null, "New GithubUser", attributes);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(redirectStrategy).sendRedirect(eq(request), eq(response), redirectUrlCaptor.capture());
        String targetUrl = redirectUrlCaptor.getValue();
        assertTrue(targetUrl.startsWith(baseRedirectUri + "/register?"));
        assertTrue(targetUrl.contains("email=" + URLEncoder.encode("githublogin@github.placeholder.com", StandardCharsets.UTF_8)));
        assertTrue(targetUrl.contains("provider=github"));
        assertTrue(targetUrl.contains("name=" + URLEncoder.encode("New", StandardCharsets.UTF_8)));
        assertTrue(targetUrl.contains("surname=" + URLEncoder.encode("GithubUser", StandardCharsets.UTF_8)));
    }

    @Test
    void onAuthenticationSuccess_newUser_nameIsNull_shouldHandleGracefully() throws IOException, ServletException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "noname@example.com");
        attributes.put("sub", "google-oauth-sub"); // Google
        // Name is null in attributes

        UserPrincipal userPrincipal = createUserPrincipal(null, "noname@example.com", null, attributes);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(redirectStrategy).sendRedirect(eq(request), eq(response), redirectUrlCaptor.capture());
        String targetUrl = redirectUrlCaptor.getValue();
        assertTrue(targetUrl.startsWith(baseRedirectUri + "/register?"));
        assertTrue(targetUrl.contains("email=" + URLEncoder.encode("noname@example.com", StandardCharsets.UTF_8)));
        assertTrue(targetUrl.contains("provider=google"));
        System.out.println("Target URL for null name: " + targetUrl); // For debugging
    }


    @Test
    void onAuthenticationSuccess_exceptionDuringProcessing_shouldRedirectToLoginWithError() throws IOException, ServletException {
        UserPrincipal userPrincipal = createUserPrincipal(1, "error@example.com", "Error User", null);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        RuntimeException mockException = new RuntimeException("Token generation failed");
        when(tokenProvider.generateToken(authentication)).thenThrow(mockException);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(redirectStrategy).sendRedirect(eq(request), eq(response), redirectUrlCaptor.capture());
        String targetUrl = redirectUrlCaptor.getValue();
        assertTrue(targetUrl.startsWith(baseRedirectUri + "/login?error="));
        assertTrue(targetUrl.contains(URLEncoder.encode("Authentication failed: Token generation failed", StandardCharsets.UTF_8)));
    }
}