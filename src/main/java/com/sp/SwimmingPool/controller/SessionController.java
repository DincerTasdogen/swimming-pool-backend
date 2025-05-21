package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.ApiResponse;
import com.sp.SwimmingPool.model.entity.Session;
import com.sp.SwimmingPool.service.ScheduledSessionCreationService;
import com.sp.SwimmingPool.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final ScheduledSessionCreationService scheduledSessionCreationService;
    private final SessionService sessionService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse> generateSessions() {
        try {
            log.info("Manual session generation triggered.");
            scheduledSessionCreationService.generateScheduledSessions();
            return ResponseEntity.ok(
                    new ApiResponse(true, "Otomatik seans oluşturma işlemi başarıyla tamamlandı.")
            );
        } catch (Exception e) {
            log.error("Error during manual session generation: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                    new ApiResponse(false, "Error during session generation: " + e.getMessage())
            );
        }
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<Session>> getSessionsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Retrieving sessions for date: {}", date);
        List<Session> sessions = sessionService.getSessionsByDate(date);

        if (sessions.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(sessions);
    }

    @PatchMapping("/{id}/education-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public ResponseEntity<?> updateSessionEducationStatus(
            @PathVariable int id,
            @Valid @RequestBody boolean sessionEducationStatus) {

        log.info("Updating session {} education status to {}",
                id, sessionEducationStatus );

        try {
            Session updatedSession = sessionService.updateSessionEducationStatus(
                    id, sessionEducationStatus);

            if (updatedSession != null) {
                return ResponseEntity.ok(updatedSession);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ApiResponse(false, "Session not found")
                );
            }
        } catch (Exception e) {
            log.error("Error updating session education status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ApiResponse(false, "Error updating session: " + e.getMessage())
            );
        }
    }

    @PostMapping("/ensure-availability")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> ensureSessionAvailability() {
        try {
            scheduledSessionCreationService.ensureMinimumSessionAvailability();
            return ResponseEntity.ok(
                    new ApiResponse(true, "Seans uygunluğu başarıyla alındı.")
            );
        } catch (Exception e) {
            log.error("Error during session availability check: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                    new ApiResponse(false, "Error during session availability check: " + e.getMessage())
            );
        }
    }
}