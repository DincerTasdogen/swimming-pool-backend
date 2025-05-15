package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.AvailableSessionsRequest;
import com.sp.SwimmingPool.dto.CreateReservationRequest;
import com.sp.SwimmingPool.dto.ReservationResponse;
import com.sp.SwimmingPool.dto.SessionResponse;
import com.sp.SwimmingPool.exception.EntityNotFoundException;
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
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/available-sessions")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<List<SessionResponse>> getAvailableSessionsWithBody(
            @Valid @RequestBody AvailableSessionsRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<Session> availableSessions =
                sessionService.getAvailableSessionsForMemberPackage(
                        userPrincipal.getId(),
                        request.getMemberPackageId(),
                        request.getPoolId(),
                        request.getDate()
                );

        List<SessionResponse> responseList = availableSessions
                .stream()
                .map(this::mapToSessionResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/available-sessions")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<List<SessionResponse>> getAvailableSessionsForMember(
            @RequestParam int memberPackageId,
            @RequestParam int poolId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<Session> availableSessions =
                sessionService.getAvailableSessionsForMemberPackage(
                        userPrincipal.getId(),
                        memberPackageId,
                        poolId,
                        date
                );

        List<SessionResponse> responseList = availableSessions
                .stream()
                .map(this::mapToSessionResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
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
        ReservationResponse response = mapToReservationResponse(reservation);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<Reservation> reservations =
                reservationService.getReservationsByMember(userPrincipal.getId());
        List<ReservationResponse> responseList = reservations
                .stream()
                .map(this::mapToReservationResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/me/active")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<List<ReservationResponse>> getMyActiveReservations(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<Reservation> reservations =
                reservationService.getActiveReservationsByMember(userPrincipal.getId());
        List<ReservationResponse> responseList = reservations
                .stream()
                .map(this::mapToReservationResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
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
    @PreAuthorize("hasRole('ADMIN')") // Example security
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

    private ReservationResponse mapToReservationResponse(
            Reservation reservation
    ) {
        ReservationResponse response = new ReservationResponse();
        response.setId(reservation.getId());
        response.setMemberId(reservation.getMemberId());
        response.setSessionId(reservation.getSessionId());
        response.setMemberPackageId(reservation.getMemberPackageId());
        response.setStatus(reservation.getStatus());
        response.setCreatedAt(reservation.getCreatedAt());
        response.setUpdatedAt(reservation.getUpdatedAt());

        try {
            Session session = sessionService.getSession(reservation.getSessionId());
            response.setSessionDate(session.getSessionDate());
            response.setStartTime(session.getStartTime());
            response.setEndTime(session.getEndTime());
            response.setEducationSession(session.isEducationSession());
            response.setRemainingCapacity(
                    session.getCapacity() - session.getCurrentBookings()
            );

            Optional<Pool> poolOpt = poolService.findById(session.getPoolId());
            response.setPoolName(poolOpt.map(Pool::getName).orElse("N/A"));
        } catch (EntityNotFoundException e) {
            System.err.println(
                    "Error enriching reservation response: " + e.getMessage()
            );
            response.setPoolName("Error: Pool not found");
        } catch (Exception e) {
            System.err.println(
                    "Unexpected error enriching reservation response: " + e.getMessage()
            );
            response.setPoolName("Error");
        }
        return response;
    }

    private SessionResponse mapToSessionResponse(Session session) {
        SessionResponse response = new SessionResponse();
        response.setId(session.getId());
        response.setPoolId(session.getPoolId());
        response.setSessionDate(session.getSessionDate());
        response.setStartTime(session.getStartTime());
        response.setEndTime(session.getEndTime());
        response.setCapacity(session.getCapacity());
        response.setCurrentBookings(session.getCurrentBookings());
        response.setAvailableSpots(
                Math.max(0, session.getCapacity() - session.getCurrentBookings())
        );
        response.setEducationSession(session.isEducationSession());
        response.setBookable(true); // Assumes only bookable sessions are passed here

        Optional<Pool> poolOpt = poolService.findById(session.getPoolId());
        response.setPoolName(poolOpt.map(Pool::getName).orElse("N/A"));

        return response;
    }
}
