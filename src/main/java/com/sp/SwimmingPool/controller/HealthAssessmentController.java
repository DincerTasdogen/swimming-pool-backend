package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.HealthAssessmentRequest;
import com.sp.SwimmingPool.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/registration/health-assessment")
@RequiredArgsConstructor
public class HealthAssessmentController {

    private final VerificationService verificationService;

    @PostMapping
    public ResponseEntity<?> saveHealthAssessment(@Valid @RequestBody HealthAssessmentRequest request) {
        boolean updated = verificationService.updateTempUserData(
                request.getEmail(),
                Map.of("healthAnswers", request.getAnswers())
        );
        if (!updated) {
            return ResponseEntity.badRequest().body("No registration in progress for this email");
        }
        return ResponseEntity.ok().build();
    }
}
