package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.AuthResponse;
import com.sp.SwimmingPool.dto.LoginRequest;
import com.sp.SwimmingPool.dto.RegisterRequest;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.service.AuthService;
import com.sp.SwimmingPool.service.CookieService;
import com.sp.SwimmingPool.service.RegistrationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthService authService;
    private final RegistrationService registrationService;
    private final CookieService cookieService;

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

//    @PostMapping("/register")
//    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
//        return ResponseEntity.ok(registrationService.register(request));
//    }
}
