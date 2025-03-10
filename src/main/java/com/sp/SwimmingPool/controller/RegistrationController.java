package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.InitialRegisterRequest;
import com.sp.SwimmingPool.dto.RegisterRequest;
import com.sp.SwimmingPool.service.EmailService;
import com.sp.SwimmingPool.service.RegistrationService;
import com.sp.SwimmingPool.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;
    private final VerificationService verificationService;
    private final EmailService emailService;

    @PostMapping("/register/init")
    public ResponseEntity<?> initiateRegistration(@Valid @RequestBody InitialRegisterRequest request) {
        // Check if email is already registered
        if (registrationService.isEmailRegistered(request.getEmail())) {
            return ResponseEntity.badRequest().body("Bu e-posta adresi zaten kayıtlıdır");
        }

        // Check if identity number is already registered
        if (registrationService.isIdentityNumberRegistered(request.getIdentityNumber())) {
            return ResponseEntity.badRequest().body("Bu T.C. Kimlik numarası zaten kayıtlıdır");
        }

        // Store initial registration data
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", request.getName());
        userData.put("surname", request.getSurname());
        userData.put("email", request.getEmail());
        userData.put("phoneNumber", request.getPhoneNumber());
        userData.put("password", request.getPassword());
        userData.put("identityNumber", request.getIdentityNumber());

        // Generate and send verification code
        String code = verificationService.generateAndStoreCode(request.getEmail(), userData);
        emailService.sendVerificationEmail(request.getEmail(), code);

        return ResponseEntity.ok().build();
    }

    // New endpoint for updating registration data without sending verification emails
    @PostMapping("/register/update-data")
    public ResponseEntity<?> updateRegistrationData(@RequestParam String email, @RequestBody Map<String, Object> updateData) {
        Map<String, Object> userData = verificationService.getTempUserData(email);
        if (userData == null) {
            return ResponseEntity.badRequest().body("No registration in progress");
        }

        // Update the user data with the new values
        verificationService.updateTempUserData(email, updateData);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/register/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String email, @RequestParam String code) {
        boolean isValid = verificationService.verifyCode(email, code);
        if (!isValid) {
            return ResponseEntity.badRequest().body("Invalid verification code");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register/complete")
    public ResponseEntity<?> completeRegistration(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> userData = verificationService.getTempUserData(request.getEmail());
        if (userData == null) {
            return ResponseEntity.badRequest().body("No verification data found");
        }

        // Complete registration
        registrationService.register(request);

        // Clear verification data
        verificationService.removeVerificationData(request.getEmail());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/register/resend-code")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        Map<String, Object> userData = verificationService.getTempUserData(email);
        if (userData == null) {
            return ResponseEntity.badRequest().body("No registration in progress");
        }

        String newCode = verificationService.generateAndStoreCode(email, userData);
        emailService.sendVerificationEmail(email, newCode);

        return ResponseEntity.ok().build();
    }
}