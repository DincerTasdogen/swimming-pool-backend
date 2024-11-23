package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.AuthResponse;
import com.sp.SwimmingPool.dto.LoginRequest;
import com.sp.SwimmingPool.dto.RegisterRequest;
import com.sp.SwimmingPool.service.AuthService;
import com.sp.SwimmingPool.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthService authService;
    private final RegistrationService registrationService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.authenticate(loginRequest));
    }

//    @PostMapping("/register")
//    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
//        return ResponseEntity.ok(registrationService.register(request));
//    }
}
