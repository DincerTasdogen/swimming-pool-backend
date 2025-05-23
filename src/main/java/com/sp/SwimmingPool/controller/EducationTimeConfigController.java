package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.EducationTimeConfigRequest;
import com.sp.SwimmingPool.model.entity.EducationTimeConfig;
import com.sp.SwimmingPool.service.EducationTimeConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/education-time-configs")
@RequiredArgsConstructor
public class EducationTimeConfigController {

    private final EducationTimeConfigService educationTimeConfigService;

    // List all configs (optionally only active ones)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public ResponseEntity<List<EducationTimeConfig>> getAllConfigs(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        List<EducationTimeConfig> configs = (active != null && active)
                ? educationTimeConfigService.getAllActiveConfigs()
                : educationTimeConfigService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    // Create new config
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public ResponseEntity<?> createConfig(@Valid @RequestBody EducationTimeConfigRequest request) {
        EducationTimeConfig created = educationTimeConfigService.createEducationTimeConfig(
                request.getStartTime(),
                request.getEndTime(),
                request.getApplicableDays(),
                request.getDescription()
        );
        if (created == null) {
            return ResponseEntity.badRequest().body("Invalid config data.");
        }
        return ResponseEntity.ok(created);
    }

    // Update config
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public ResponseEntity<?> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody EducationTimeConfigRequest request
    ) {
        EducationTimeConfig updated = educationTimeConfigService.updateEducationTimeConfig(
                id,
                request.getStartTime(),
                request.getEndTime(),
                request.getApplicableDays(),
                request.getDescription(),
                request.getActive()
        );
        if (updated == null) {
            return ResponseEntity.badRequest().body("Invalid or non-existent config.");
        }
        return ResponseEntity.ok(updated);
    }

    // Delete config
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public ResponseEntity<?> deleteConfig(@PathVariable Long id) {
        boolean deleted = educationTimeConfigService.deleteEducationTimeConfig(id);
        if (deleted) {
            return ResponseEntity.ok().body("Config deleted.");
        } else {
            return ResponseEntity.status(404).body("Config not found.");
        }
    }
}