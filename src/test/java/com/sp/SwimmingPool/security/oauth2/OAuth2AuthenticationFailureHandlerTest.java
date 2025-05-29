package com.sp.SwimmingPool.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationFailureHandlerTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private AuthenticationException exception;
    @Mock
    private RedirectStrategy redirectStrategy;

    @InjectMocks
    private OAuth2AuthenticationFailureHandler failureHandler;

    private final String baseRedirectUri = "http://localhost:3000/login";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(failureHandler, "redirectUri", baseRedirectUri);
        failureHandler.setRedirectStrategy(redirectStrategy);
    }

    @Test
    void onAuthenticationFailure_shouldRedirectWithErrorMessage() throws IOException, ServletException {
        String errorMessage = "OAuth2 provider error";
        when(exception.getLocalizedMessage()).thenReturn(errorMessage);

        String actualExpectedUrlBasedOnLog = baseRedirectUri + "?error=" + errorMessage;
        failureHandler.onAuthenticationFailure(request, response, exception);
        verify(redirectStrategy).sendRedirect(request, response, actualExpectedUrlBasedOnLog);
    }

    @Test
    void onAuthenticationFailure_shouldHandleNullErrorMessage() throws IOException, ServletException {
        when(exception.getLocalizedMessage()).thenReturn(null);

        failureHandler.onAuthenticationFailure(request, response, exception);
        String expectedTargetUrl = baseRedirectUri + "?error=null";
        when(exception.getLocalizedMessage()).thenReturn("null");
        failureHandler.onAuthenticationFailure(request, response, exception);
        verify(redirectStrategy).sendRedirect(request, response, baseRedirectUri + "?error=null");
    }
}