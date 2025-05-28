package com.sp.SwimmingPool.security.oauth2;

import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;

    @Value("${app.redirect-uri}")
    private String redirectUri;
    @Value("${app.jwt.cookie.name}")
    private String cookieName;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Map<String, Object> attributes = userPrincipal.getAttributes();

            String email = userPrincipal.getEmail();
            String name = userPrincipal.getName();

            // Get values from attributes if not present in principal
            if (email == null && attributes != null) {
                email = (String) attributes.get("email");
            }

            if (name == null || "null".equals(name)) {
                name = attributes != null ? (String) attributes.get("name") : null;
            }

            log.debug("OAuth2 login success. Email: {}, Name: {}", email, name);

            String targetUrl;

            if (userPrincipal.getId() != null) {
                // User already exists - generate token and redirect to dashboard
                String token = tokenProvider.generateToken(authentication);

                // Set the token as a cookie
                Cookie authCookie = new Cookie(cookieName, token);
                authCookie.setPath("/");
                authCookie.setHttpOnly(true);
                authCookie.setMaxAge(3600); // 1 hour
                response.addCookie(authCookie);

                // Redirect directly to the dashboard
                targetUrl = redirectUri + "/member/dashboard";
                log.debug("Existing user - redirecting to dashboard: {}", targetUrl);
            } else {
                // New user - need to complete registration
                // Extract basic details
                String surname = "";
                String provider = "";

                // Process name parts if available
                if (name != null && name.contains(" ")) {
                    String[] nameParts = name.split(" ", 2);
                    name = nameParts[0];
                    surname = nameParts[1];
                }

                // Determine provider
                if (attributes.containsKey("sub")) {
                    provider = "google";
                } else if (attributes.containsKey("id")) {
                    provider = "github";
                    // GitHub might not provide email
                    if (email == null) {
                        String login = (String) attributes.get("login");
                        if (login != null) {
                            email = login + "@github.placeholder.com";
                        }
                    }
                }

                // Build the redirect URL with hardcoded frontend URL to avoid path issues
                StringBuilder urlBuilder = new StringBuilder(redirectUri + "/register?");

                // Manually build query parameters - avoid potential URI encoding issues
                if (email != null) {
                    urlBuilder.append("email=").append(URLEncoder.encode(email, StandardCharsets.UTF_8));
                }

                if (provider != null && !provider.isEmpty()) {
                    urlBuilder.append("&provider=").append(provider);
                }

                if (name != null && !name.equals("null")) {
                    urlBuilder.append("&name=").append(URLEncoder.encode(name, StandardCharsets.UTF_8));
                }

                if (surname != null && !surname.isEmpty()) {
                    urlBuilder.append("&surname=").append(URLEncoder.encode(surname, StandardCharsets.UTF_8));
                }

                targetUrl = urlBuilder.toString();
                log.debug("New user - redirecting to registration: {}", targetUrl);
            }

            log.info("Redirecting to: {}", targetUrl);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception ex) {
            log.error("OAuth2 authentication success handling failed", ex);
            getRedirectStrategy().sendRedirect(request, response,
                    redirectUri + "/login?error=" +
                            URLEncoder.encode("Authentication failed: " + ex.getMessage(), StandardCharsets.UTF_8));
        }
    }
}