package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.AuthResponse;
import com.sp.SwimmingPool.dto.LoginRequest;
import com.sp.SwimmingPool.dto.RegisterRequest;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.User;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.repos.UserRepository;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.service.*;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final VerificationService verificationService;
    private final EmailService emailService;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final UserRepository userRepository;

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

        AuthResponse.AuthResponseBuilder responseBuilder = AuthResponse.builder()
                .id(userPrincipal.getId())
                .email(userPrincipal.getEmail())
                .role(userPrincipal.getRole())
                .userType(userPrincipal.getUserType());

        // Fetch full details based on userType to get status and surname
        if ("MEMBER".equals(userPrincipal.getUserType())) {
            Member member = memberRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member details not found for authenticated user"));
            responseBuilder
                    .name(member.getName())
                    .surname(member.getSurname())
                    .status(member.getStatus());
        } else if ("STAFF".equals(userPrincipal.getUserType())) {
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User details not found for authenticated user"));
            responseBuilder
                    .name(user.getName())
                    .surname(user.getSurname())
                    .status(null);
        } else {
            // Should not happen if UserPrincipal is created correctly
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // Or handle appropriately
        }

        return ResponseEntity.ok(responseBuilder.build());
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

            if (registrationService.isEmailRegistered(email)) {
                return ResponseEntity.badRequest().body("Email already registered");
            }

            // Check if identity number exists (if provided)
            if (identityNumber != null && registrationService.isIdentityNumberRegistered(identityNumber)) {
                return ResponseEntity.badRequest().body("Identity number already registered");
            }

            boolean isComplete = isOAuthRegistrationComplete(oauthData);

            if (!isComplete) {
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

    private boolean isOAuthRegistrationComplete(Map<String, Object> oauthData) {
        return oauthData.containsKey("phoneNumber") &&
                oauthData.containsKey("identityNumber") &&
                oauthData.containsKey("birthDate") &&
                oauthData.containsKey("height") &&
                oauthData.containsKey("weight") &&
                oauthData.containsKey("gender");
    }

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
                // Document paths
                .photo((String) oauthData.get("photo"))
                .idPhotoFront((String) oauthData.get("idPhotoFront"))
                .idPhotoBack((String) oauthData.get("idPhotoBack"))
                // Generate a secure random password
                .password(UUID.randomUUID().toString())
                .build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Email gerekli.");
        }

        Member member = memberRepository.findByEmail(email).orElse(null);

        if (member == null) {
            return ResponseEntity.ok().body("Doğrulama E-Postası gönderildi!");
        }
        Map<String, Object> tempData = Map.of(
                "resetRequested", LocalDateTime.now().toString()
        );

        String verificationCode = verificationService.generateAndStoreCode(email, tempData);

        try {
            emailService.sendPasswordResetEmail(email, verificationCode);
            return ResponseEntity.ok().body("Doğrulama E-Postası gönderildi!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("E-Posta gönderilirken bir hata oluştu.");
        }
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Email gerekli.");
        }

        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().body("Doğrulama kodu gerekli.");
        }

        boolean isValid = verificationService.verifyCode(email, code);
        if (!isValid) {
            return ResponseEntity.badRequest().body("Doğrulama kodu yanlış ya da süresi geçmiş.");
        }

        verificationService.extendVerificationExpiry(email, 5);
        return ResponseEntity.ok().body("E-posta adresinizi doğruladınız! Şifrenizi şimdi yenileyebilirsiniz.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        String newPassword = request.get("newPassword");

        if (email == null || email.isEmpty() || code == null || code.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body("Parametrelerden en az biri eksik.");
        }

        boolean isValid = verificationService.verifyCode(email, code);
        if (!isValid) {
            return ResponseEntity.badRequest().body("Doğrulama kodu yanlış ya da süresi geçmiş.");
        }

        try {
            memberRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Üye bulunamadı"));
            memberService.updatePassword(email, newPassword);
            verificationService.removeVerificationData(email);

            return ResponseEntity.ok().body("Şifreniz başarıyla güncellendi.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Şifre sıfırlanırken bir hata oluştu: " + e.getMessage());
        }
    }

}
