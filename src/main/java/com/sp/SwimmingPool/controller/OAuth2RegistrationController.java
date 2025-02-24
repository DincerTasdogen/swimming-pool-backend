package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.RegisterRequest;
import com.sp.SwimmingPool.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
public class OAuth2RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/complete-registration")
    public ResponseEntity<?> completeOAuthRegistration(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(request);
        return ResponseEntity.ok().build();
    }
}