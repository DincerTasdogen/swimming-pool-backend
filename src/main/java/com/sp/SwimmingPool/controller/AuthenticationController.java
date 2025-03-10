package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.AuthResponse;
import com.sp.SwimmingPool.dto.LoginRequest;
import com.sp.SwimmingPool.dto.OAuthRegisterRequest;
import com.sp.SwimmingPool.dto.RegisterRequest;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.service.AuthService;
import com.sp.SwimmingPool.service.CookieService;
import com.sp.SwimmingPool.service.RegistrationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthService authService;
    private final RegistrationService registrationService;
    private final CookieService cookieService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(authService.authenticate(loginRequest, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        cookieService.clearAuthCookie(response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthResponse response = AuthResponse.builder()
                .id(userPrincipal.getId())
                .email(userPrincipal.getEmail())
                .role(userPrincipal.getRole())
                .userType(userPrincipal.getUserType())
                .name(userPrincipal.getName())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/oauth")
    public ResponseEntity<?> registerOAuthUser(@RequestBody Map<String, Object> oauthData,
                                               HttpServletResponse response) {
        try {
            // Validate required fields
            String email = (String) oauthData.get("email");
            String name = (String) oauthData.get("name");
            String surname = (String) oauthData.get("surname");
            String provider = (String) oauthData.get("provider");
            String identityNumber = (String) oauthData.get("identityNumber");

            if (email == null || name == null || surname == null || provider == null) {
                return ResponseEntity.badRequest().body("Missing required OAuth data");
            }

            // Check if email exists
            if (registrationService.isEmailRegistered(email)) {
                return ResponseEntity.badRequest().body("Email already registered");
            }

            // Check if identity number exists (if provided)
            if (identityNumber != null && registrationService.isIdentityNumberRegistered(identityNumber)) {
                return ResponseEntity.badRequest().body("Identity number already registered");
            }

            // Determine if this is initial data collection or final registration
            boolean isComplete = isOAuthRegistrationComplete(oauthData);

            if (!isComplete) {
                // Store the partial data for later completion
                registrationService.storeOAuthTempData(oauthData);
                return ResponseEntity.ok().build();
            } else {
                // This is the final step - convert to RegisterRequest and complete registration
                RegisterRequest registerRequest = createRegisterRequestFromOAuthData(oauthData);

                // Register the user (with generated password handled in the service)
                Member member = registrationService.register(registerRequest);

                // Generate authentication and token for the newly registered user
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        UserPrincipal.createFromMember(member), null,
                        List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = jwtTokenProvider.generateToken(authentication);

                // Set authentication cookie
                cookieService.createAuthCookie(jwt, response);

                // Return user info
                AuthResponse authResponse = AuthResponse.builder()
                        .id(member.getId())
                        .email(member.getEmail())
                        .name(member.getName())
                        .role("MEMBER")
                        .userType("MEMBER")
                        .build();

                return ResponseEntity.ok(authResponse);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Helper method to check if the OAuth registration data is complete
    private boolean isOAuthRegistrationComplete(Map<String, Object> oauthData) {
        // Check if we have all required fields to complete registration
        return oauthData.containsKey("phoneNumber") &&
                oauthData.containsKey("identityNumber") &&
                oauthData.containsKey("birthDate") &&
                oauthData.containsKey("height") &&
                oauthData.containsKey("weight") &&
                oauthData.containsKey("gender");
    }

    // Helper method to create RegisterRequest from OAuth data
    private RegisterRequest createRegisterRequestFromOAuthData(Map<String, Object> oauthData) {
        return RegisterRequest.builder()
                .email((String) oauthData.get("email"))
                .name((String) oauthData.get("name"))
                .surname((String) oauthData.get("surname"))
                .phoneNumber((String) oauthData.get("phoneNumber"))
                .identityNumber((String) oauthData.get("identityNumber"))
                .birthDate(LocalDate.parse((String) oauthData.get("birthDate")))
                .height(Double.parseDouble(oauthData.get("height").toString()))
                .weight(Double.parseDouble(oauthData.get("weight").toString()))
                .gender((String) oauthData.get("gender"))
                .canSwim(Boolean.parseBoolean(oauthData.get("canSwim").toString()))
                // Generate a random password for OAuth users
                .password(UUID.randomUUID().toString())
                .build();
    }
}
