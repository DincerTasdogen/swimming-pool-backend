package com.sp.SwimmingPool.security;

import com.sp.SwimmingPool.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private CustomUserDetailsService customUserDetailsService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final String cookieName = "jwtTestCookie";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "cookieName", cookieName);
        SecurityContextHolder.clearContext(); // Ensure clean context for each test
    }

    @Test
    void doFilterInternal_withValidJwtInHeader_shouldSetAuthentication() throws ServletException, IOException {
        String jwt = "valid.jwt.token";
        String username = "testuser";
        UserDetails userDetails = User.withUsername(username)
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(jwtTokenProvider.validateToken(jwt)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(jwt)).thenReturn(username);
        when(customUserDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withValidJwtInCookie_shouldSetAuthentication() throws ServletException, IOException {
        String jwt = "valid.cookie.jwt.token";
        String username = "cookieuser";
        UserDetails userDetails = User.withUsername(username)
                .password("password")
                .authorities(Collections.emptyList())
                .build();
        Cookie authCookie = new Cookie(cookieName, jwt);

        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{authCookie});
        when(jwtTokenProvider.validateToken(jwt)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(jwt)).thenReturn(username);
        when(customUserDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_headerTokenTakesPrecedenceOverCookie() throws ServletException, IOException {
        String headerJwt = "header.jwt.token";
        String cookieJwt = "cookie.jwt.token";
        String headerUsername = "headeruser";

        UserDetails userDetails = User.withUsername(headerUsername)
                .password("password")
                .authorities(Collections.emptyList())
                .build();
        Cookie authCookie = new Cookie(cookieName, cookieJwt);


        when(request.getHeader("Authorization")).thenReturn("Bearer " + headerJwt);

        when(jwtTokenProvider.validateToken(headerJwt)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(headerJwt)).thenReturn(headerUsername);
        when(customUserDetailsService.loadUserByUsername(headerUsername)).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(headerUsername, SecurityContextHolder.getContext().getAuthentication().getName());
        verify(jwtTokenProvider, never()).validateToken(cookieJwt);
        verify(filterChain).doFilter(request, response);
    }


    @Test
    void doFilterInternal_withNoJwt_shouldNotSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withInvalidJwt_shouldNotSetAuthentication() throws ServletException, IOException {
        String jwt = "invalid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(jwtTokenProvider.validateToken(jwt)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_exceptionDuringProcessing_shouldNotSetAuthenticationAndContinueChain() throws ServletException, IOException {
        String jwt = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(jwtTokenProvider.validateToken(jwt)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(jwt)).thenReturn("testuser");
        when(customUserDetailsService.loadUserByUsername("testuser")).thenThrow(new RuntimeException("DB error"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response); // Crucial: filter chain must proceed
    }
}