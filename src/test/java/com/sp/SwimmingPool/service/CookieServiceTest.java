package com.sp.SwimmingPool.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CookieServiceTest {

    @Mock
    private HttpServletResponse httpServletResponse;

    private final String COOKIE_NAME = "testAuthCookie";
    private final int MAX_AGE_SECONDS = 3600; // 1 hour

    private CookieService cookieService;

    @BeforeEach
    void setUp() {
        cookieService = new CookieService(COOKIE_NAME, MAX_AGE_SECONDS);
    }

    @Test
    void createAuthCookie_addsCorrectlyConfiguredCookieToResponse() {
        String tokenValue = "sample-jwt-token";

        cookieService.createAuthCookie(tokenValue, httpServletResponse);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(httpServletResponse).addCookie(cookieCaptor.capture());

        Cookie capturedCookie = cookieCaptor.getValue();
        assertNotNull(capturedCookie);
        assertEquals(COOKIE_NAME, capturedCookie.getName());
        assertEquals(tokenValue, capturedCookie.getValue());
        assertTrue(capturedCookie.isHttpOnly());
        assertFalse(capturedCookie.getSecure());
        assertEquals("/", capturedCookie.getPath());
        assertEquals(MAX_AGE_SECONDS, capturedCookie.getMaxAge());
    }

    @Test
    void clearAuthCookie_addsCookieWithMaxAgeZeroToResponse() {
        cookieService.clearAuthCookie(httpServletResponse);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(httpServletResponse).addCookie(cookieCaptor.capture());

        Cookie capturedCookie = cookieCaptor.getValue();
        assertNotNull(capturedCookie);
        assertEquals(COOKIE_NAME, capturedCookie.getName());
        assertEquals("", capturedCookie.getValue());
        assertTrue(capturedCookie.isHttpOnly());
        assertTrue(capturedCookie.getSecure());
        assertEquals("/", capturedCookie.getPath());
        assertEquals(0, capturedCookie.getMaxAge());
    }
}