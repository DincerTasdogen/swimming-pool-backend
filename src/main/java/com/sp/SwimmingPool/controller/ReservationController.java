package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.AvailableSessionsRequest;
import com.sp.SwimmingPool.dto.CreateReservationRequest;
import com.sp.SwimmingPool.dto.ReservationResponse;
import com.sp.SwimmingPool.dto.SessionResponse;
import com.sp.SwimmingPool.model.entity.Pool;
import com.sp.SwimmingPool.model.entity.Reservation;
import com.sp.SwimmingPool.model.entity.Session;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.service.PoolService;
import com.sp.SwimmingPool.service.ReservationService;
import com.sp.SwimmingPool.service.SessionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final SessionService sessionService;
    private final PoolService poolService;

    private ReservationResponse mapToReservationResponse(Reservation reservation) {
        Session session = sessionService.getSession(reservation.getSessionId());

        Optional<Pool> poolOpt = poolService.findById(session.getPoolId());
        String poolName = poolOpt.map(Pool::getName).orElse("N/A");

        int remainingCapacity = session.getCapacity() - session.getCurrentBookings();

        return new ReservationResponse(
                reservation.getId(),
                reservation.getMemberId(),
                reservation.getSessionId(),
                reservation.getMemberPackageId(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                poolName,
                session.isEducationSession(),
                remainingCapacity
        );
    }

    @PostMapping("/available-sessions")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<List<SessionResponse>> getAvailableSessionsWithBody(
            @Valid @RequestBody AvailableSessionsRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<SessionResponse> availableSessions =
                sessionService.getAvailableSessionsForMemberPackage(
                        userPrincipal.getId(),
                        request.getMemberPackageId(),
                        request.getPoolId(),
                        request.getDate()
                );
        return ResponseEntity.ok(availableSessions);
    }

    @GetMapping("/available-sessions")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<List<SessionResponse>> getAvailableSessionsForMember(
            @RequestParam int memberPackageId,
            @RequestParam int poolId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<SessionResponse> availableSessions =
                sessionService.getAvailableSessionsForMemberPackage(
                        userPrincipal.getId(),
                        memberPackageId,
                        poolId,
                        date
                );
        return ResponseEntity.ok(availableSessions);
    }

    @PostMapping
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Reservation reservation = reservationService.createReservation(
                userPrincipal.getId(),
                request.getSessionId(),
                request.getMemberPackageId()
        );
        ReservationResponse response = mapToReservationResponse(reservation); // your mapping method
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Page<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ReservationResponse> reservations =
                reservationService.getReservationsByMember(userPrincipal.getId(), page, size);
        return ResponseEntity.ok(reservations);
    }

    @PutMapping("/{reservationId}/cancel")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable int reservationId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        reservationService.cancelReservation(reservationId, userPrincipal.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{reservationId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markReservationAsCompleted(
            @PathVariable int reservationId
    ) {
        reservationService.markReservationAsCompleted(reservationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{reservationId}/no-show")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markReservationAsNoShow(
            @PathVariable int reservationId
    ) {
        reservationService.markReservationAsNoShow(reservationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/complete-by-qr")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH', 'DOCTOR')")
    public ResponseEntity<?> completeReservationByQr(@RequestBody Map<String, String> body) {
        String qrToken = body.get("qrToken");
        if (qrToken == null || qrToken.isEmpty()) {
            return ResponseEntity.badRequest().body("QR kodu eksik.");
        }
        try {
            reservationService.completeReservationByQrToken(qrToken);
            return ResponseEntity.ok("Giriş başarılı, rezervasyon tamamlandı.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{reservationId}/qr-token")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<?> getReservationQrToken(
            @PathVariable int reservationId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String qrToken = reservationService.generateReservationQrTokenForMember(reservationId, userPrincipal.getId());
        return ResponseEntity.ok(Map.of("qrToken", qrToken));
    }
}