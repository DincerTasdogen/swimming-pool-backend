package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.AvailableSessionsRequest;
import com.sp.SwimmingPool.dto.CreateReservationRequest;
import com.sp.SwimmingPool.dto.ReservationResponse;
import com.sp.SwimmingPool.dto.SessionResponse;
import com.sp.SwimmingPool.model.entity.Pool;
import com.sp.SwimmingPool.model.entity.Reservation;
import com.sp.SwimmingPool.model.entity.Session;
import com.sp.SwimmingPool.service.PoolService;
import com.sp.SwimmingPool.service.ReservationService;
import com.sp.SwimmingPool.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.RequestEntity.post;

/**
 * REST Controller for reservation operations
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final SessionService sessionService;
    private final PoolService poolService;

    @Autowired
    public ReservationController(
            ReservationService reservationService,
            SessionService sessionService,
            PoolService poolService) {
        this.reservationService = reservationService;
        this.sessionService = sessionService;
        this.poolService = poolService;
    }

    @GetMapping("/available-sessions")
    public ResponseEntity<List<SessionResponse>> getAvailableSessionsForMember(
            @RequestParam int memberId,
            @RequestParam int memberPackageId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            List<Session> availableSessions = sessionService.getAvailableSessionsForMemberPackage(
                    memberId, memberPackageId, date);

            List<SessionResponse> responseList = availableSessions.stream()
                    .map(this::mapToSessionResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/available-sessions")
    public ResponseEntity<List<SessionResponse>> getAvailableSessionsWithBody(
            @Valid @RequestBody AvailableSessionsRequest request) {

        try {
            List<Session> availableSessions = sessionService.getAvailableSessionsForMemberPackage(
                    request.getMemberId(), request.getMemberPackageId(), request.getDate());

            List<SessionResponse> responseList = availableSessions.stream()
                    .map(this::mapToSessionResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request) {

        try {
            Reservation reservation = reservationService.createReservation(
                    request.getMemberId(),
                    request.getSessionId(),
                    request.getMemberPackageId());

            ReservationResponse response = mapToReservationResponse(reservation);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<ReservationResponse>> getMemberReservations(@PathVariable int memberId) {
        List<Reservation> reservations = reservationService.getReservationsByMember(memberId);
        List<ReservationResponse> responseList = reservations.stream()
                .map(this::mapToReservationResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/member/{memberId}/active")
    public ResponseEntity<List<ReservationResponse>> getActiveMemberReservations(@PathVariable int memberId) {
        List<Reservation> reservations = reservationService.getActiveReservationsByMember(memberId);
        List<ReservationResponse> responseList = reservations.stream()
                .map(this::mapToReservationResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    @PutMapping("/{reservationId}/cancel")
    public ResponseEntity<Void> cancelReservation(@PathVariable int reservationId) {
        try {
            reservationService.cancelReservation(reservationId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{reservationId}/complete")
    public ResponseEntity<Void> markReservationAsCompleted(@PathVariable int reservationId) {
        try {
            reservationService.markReservationAsCompleted(reservationId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{reservationId}/no-show")
    public ResponseEntity<Void> markReservationAsNoShow(@PathVariable int reservationId) {
        try {
            reservationService.markReservationAsNoShow(reservationId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private ReservationResponse mapToReservationResponse(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.setId(reservation.getId());
        response.setMemberId(reservation.getMemberId());
        response.setSessionId(reservation.getSessionId());
        response.setMemberPackageId(reservation.getMemberPackageId());
        response.setStatus(reservation.getStatus());
        response.setCreatedAt(reservation.getCreatedAt());
        response.setUpdatedAt(reservation.getUpdatedAt());

        // Populate session details by fetching from sessionService
        try {
            Session session = sessionService.getSession(reservation.getSessionId());
            response.setSessionDate(session.getSessionDate());
            response.setStartTime(session.getStartTime());
            response.setEndTime(session.getEndTime());
            response.setEducationSession(session.isEducationSession());
            response.setRemainingCapacity(session.getCapacity() - session.getCurrentBookings());

            // Get pool name from the poolService
            Optional<Pool> poolOpt = poolService.findById(session.getPoolId());
            poolOpt.ifPresent(pool -> response.setPoolName(pool.getName()));
        } catch (Exception e) {
            // Log error but continue
            System.err.println("Error fetching session details: " + e.getMessage());
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
        response.setAvailableSpots(session.getCapacity() - session.getCurrentBookings());
        response.setEducationSession(session.isEducationSession());
        response.setBookable(true); // By default, sessions returned are bookable

        // Get pool name from pool service
        Optional<Pool> poolOpt = poolService.findById(session.getPoolId());
        poolOpt.ifPresent(pool -> response.setPoolName(pool.getName()));

        return response;
    }

}