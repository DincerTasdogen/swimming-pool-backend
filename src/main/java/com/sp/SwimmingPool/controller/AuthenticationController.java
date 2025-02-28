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

import java.util.List;
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
    public ResponseEntity<?> registerOAuthUser(@Valid @RequestBody OAuthRegisterRequest request,
                                               HttpServletResponse response) {
        try {
            // Check if email exists
            if (registrationService.isEmailRegistered(request.getEmail())) {
                return ResponseEntity
                        .badRequest()
                        .body("Email already registered");
            }

            // Check if identity number exists
            if (registrationService.isIdentityNumberRegistered(request.getIdentityNumber())) {
                return ResponseEntity
                        .badRequest()
                        .body("Identity number already registered");
            }

            // Convert OAuth request to RegisterRequest
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .email(request.getEmail())
                    .name(request.getName())
                    .surname(request.getSurname())
                    .phoneNumber(request.getPhoneNumber())
                    .identityNumber(request.getIdentityNumber())
                    .birthDate(request.getBirthDate())
                    .height(request.getHeight())
                    .weight(request.getWeight())
                    .gender(request.getGender())
                    .canSwim(request.isCanSwim())
                    // Generate a random password for OAuth users
                    // (they will authenticate through OAuth provider, not password)
                    .password(UUID.randomUUID().toString())
                    .build();

            // Register the user
            Member member = registrationService.register(registerRequest);

            // Generate authentication and token for the newly registered user
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    UserPrincipal.createFromMember(member), null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenProvider.generateToken(authentication);

            // Set authentication cookie using cookieService
            cookieService.createAuthCookie(jwt, response);

            // Return user info without the token (since it's in the cookie)
            AuthResponse authResponse = AuthResponse.builder()
                    .id(member.getId())
                    .email(member.getEmail())
                    .name(member.getName())
                    .role("MEMBER")
                    .userType("MEMBER")
                    .build();

            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

//    @PostMapping("/register")
//    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
//        return ResponseEntity.ok(registrationService.register(request));
//    }
}
