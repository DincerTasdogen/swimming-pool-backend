package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.MemberPackage;
import com.sp.SwimmingPool.model.entity.PackageType;
import com.sp.SwimmingPool.model.entity.Reservation;
import com.sp.SwimmingPool.model.entity.Session;
import com.sp.SwimmingPool.model.enums.MemberPackagePaymentStatusEnum;
import com.sp.SwimmingPool.model.enums.ReservationStatusEnum;
import com.sp.SwimmingPool.repos.MemberPackageRepository;
import com.sp.SwimmingPool.repos.PackageTypeRepository;
import com.sp.SwimmingPool.repos.ReservationRepository;
import com.sp.SwimmingPool.repos.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {

    private final SessionRepository sessionRepository;
    private final ReservationRepository reservationRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final SessionService sessionService;
    private final MemberService memberService;

    @Autowired
    public ReservationService(
            SessionRepository sessionRepository,
            ReservationRepository reservationRepository,
            MemberPackageRepository memberPackageRepository,
            PackageTypeRepository packageTypeRepository,
            @Lazy SessionService sessionService,
            MemberService memberService) {
        this.sessionRepository = sessionRepository;
        this.reservationRepository = reservationRepository;
        this.memberPackageRepository = memberPackageRepository;
        this.packageTypeRepository = packageTypeRepository;
        this.sessionService = sessionService;
        this.memberService = memberService;
    }

    public List<Reservation> getReservationsByMember(int memberId) {
        return reservationRepository.findByMemberId(memberId);
    }

    public List<Reservation> getActiveReservationsByMember(int memberId) {
        return reservationRepository.findByMemberIdAndStatus(memberId, ReservationStatusEnum.CONFIRMED);
    }

    @Transactional
    public Reservation createReservation(int memberId, int sessionId, int memberPackageId) throws Exception {
        // 1. Get the session
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new Exception("Session not found");
        }

        Session session = sessionOpt.get();

        // 2. Verify session is not full
        if (session.getCurrentBookings() >= session.getCapacity()) {
            throw new Exception("Session is already full");
        }

        // 3. Verify member has a valid package
        Optional<MemberPackage> memberPackageOpt = memberPackageRepository.findById(memberPackageId);
        if (memberPackageOpt.isEmpty()) {
            throw new Exception("Member package not found");
        }

        MemberPackage memberPackage = memberPackageOpt.get();

        if (!memberPackage.isActive() || memberPackage.getSessionsRemaining() <= 0) {
            throw new Exception("No remaining sessions in package");
        }

        if (memberPackage.getPaymentStatus() != MemberPackagePaymentStatusEnum.COMPLETED) {
            throw new Exception("Package payment has not been completed");
        }

        // Check if package is restricted to a specific pool
        if (memberPackage.getPoolId() > 0 && memberPackage.getPoolId() != session.getPoolId()) {
            throw new Exception("This package can only be used at the specified pool");
        }

        // 4. Verify package type matches session type
        Optional<PackageType> packageTypeOpt = packageTypeRepository.findById(memberPackage.getPackageTypeId());
        if (packageTypeOpt.isEmpty()) {
            throw new Exception("Package type not found");
        }

        PackageType packageType = packageTypeOpt.get();

        // Check if session time is within package allowed times
        if (session.getStartTime().isBefore(packageType.getStartTime()) ||
                session.getEndTime().isAfter(packageType.getEndTime())) {
            throw new Exception("This session is outside the allowed time for your package");
        }

        // Check if education requirement matches
        if (packageType.isEducationPackage() && !session.isEducationSession()) {
            throw new Exception("This session is not an education session required by your package");
        }

        // Check if swimming ability is required but member doesn't have it
        if (packageType.isRequiresSwimmingAbility()) {
            if (!memberService.hasSwimmingAbility(memberId)) {
                throw new Exception("This package requires swimming ability verification");
            }
        }

        // 5. Verify booking time rule (72 hours before session)
        LocalDateTime sessionStart = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime bookingDeadline = sessionStart.minusHours(72);

        if (currentTime.isBefore(bookingDeadline)) {
            throw new Exception("Sessions can only be reserved within 72 hours of the session time");
        }

        // 6. Check for conflicting reservations
        if (hasConflictingReservation(memberId, session)) {
            throw new Exception("Member already has a reservation at this time");
        }

        // 7. Check if member already has this session reserved
        Optional<Reservation> existingReservation =
                reservationRepository.findByMemberIdAndSessionId(memberId, sessionId);
        if (existingReservation.isPresent()) {
            throw new Exception("Member already has a reservation for this session");
        }

        // 8. Create the reservation
        Reservation reservation = new Reservation();
        reservation.setMemberId(memberId);
        reservation.setSessionId(sessionId);
        reservation.setMemberPackageId(memberPackageId);
        reservation.setStatus(ReservationStatusEnum.CONFIRMED);
        reservation.setCreatedAt(LocalDateTime.now());
        reservation.setUpdatedAt(LocalDateTime.now());

        // 9. Update session booking count
        sessionService.incrementSessionBookings(sessionId);

        // 10. Decrement remaining sessions in package
        memberPackage.setSessionsRemaining(memberPackage.getSessionsRemaining() - 1);
        memberPackageRepository.save(memberPackage);

        // 11. Save and return the reservation
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void cancelReservation(int reservationId) throws Exception {
        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            throw new Exception("Reservation not found");
        }

        Reservation reservation = reservationOpt.get();

        // Only confirmed reservations can be canceled
        if (reservation.getStatus() != ReservationStatusEnum.CONFIRMED) {
            throw new Exception("Only confirmed reservations can be canceled");
        }

        // Check if cancellation is within allowed time (3 hours before session)
        Optional<Session> sessionOpt = sessionRepository.findById(reservation.getSessionId());
        if (sessionOpt.isEmpty()) {
            throw new Exception("Session not found");
        }

        Session session = sessionOpt.get();

        LocalDateTime sessionStart = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
        LocalDateTime cancellationDeadline = sessionStart.minusHours(3);

        if (LocalDateTime.now().isAfter(cancellationDeadline)) {
            throw new Exception("Reservations must be canceled at least 3 hours before session time");
        }

        // Update reservation status
        reservation.setStatus(ReservationStatusEnum.CANCELLED);
        reservation.setUpdatedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        // Update session booking count
        sessionService.decrementSessionBookings(reservation.getSessionId());

        // Return session to package
        Optional<MemberPackage> memberPackageOpt = memberPackageRepository.findById(reservation.getMemberPackageId());
        if (memberPackageOpt.isEmpty()) {
            throw new Exception("Member package not found");
        }

        MemberPackage memberPackage = memberPackageOpt.get();

        memberPackage.setSessionsRemaining(memberPackage.getSessionsRemaining() + 1);
        memberPackageRepository.save(memberPackage);
    }

    public boolean hasConflictingReservation(int memberId, Session newSession) throws Exception {
        List<Reservation> activeReservations = reservationRepository
                .findByMemberIdAndStatus(memberId, ReservationStatusEnum.CONFIRMED);

        for (Reservation reservation : activeReservations) {
            Optional<Session> existingSessionOpt = sessionRepository.findById(reservation.getSessionId());

            if (existingSessionOpt.isPresent()) {
                Session existingSession = existingSessionOpt.get();

                // Check if sessions are on the same date
                if (existingSession.getSessionDate().equals(newSession.getSessionDate())) {
                    // Check for time overlap
                    boolean startsBeforeEnds = existingSession.getStartTime().isBefore(newSession.getEndTime());
                    boolean endsAfterStarts = existingSession.getEndTime().isAfter(newSession.getStartTime());

                    if (startsBeforeEnds && endsAfterStarts) {
                        return true; // Conflict found
                    }
                }
            }
        }

        return false;
    }

    @Transactional
    public void markReservationAsCompleted(int reservationId) throws Exception {
        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            throw new Exception("Reservation not found");
        }

        Reservation reservation = reservationOpt.get();

        if (reservation.getStatus() != ReservationStatusEnum.CONFIRMED) {
            throw new Exception("Only confirmed reservations can be marked as completed");
        }

        reservation.setStatus(ReservationStatusEnum.COMPLETED);
        reservation.setUpdatedAt(LocalDateTime.now());
        reservationRepository.save(reservation);
    }

    @Transactional
    public void markReservationAsNoShow(int reservationId) throws Exception {
        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            throw new Exception("Reservation not found");
        }

        Reservation reservation = reservationOpt.get();

        if (reservation.getStatus() != ReservationStatusEnum.CONFIRMED) {
            throw new Exception("Only confirmed reservations can be marked as no-show");
        }

        reservation.setStatus(ReservationStatusEnum.NO_SHOW);
        reservation.setUpdatedAt(LocalDateTime.now());
        reservationRepository.save(reservation);
    }

    @Transactional
    public void processMissedReservations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentDate = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();
        List<ReservationStatusEnum> activeStatuses = Arrays.asList(ReservationStatusEnum.CONFIRMED);

        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations(
                activeStatuses, currentDate, currentTime);

        for (Reservation reservation : expiredReservations) {
            try {
                reservation.setStatus(ReservationStatusEnum.NO_SHOW);
                reservation.setUpdatedAt(now);
                reservationRepository.save(reservation);
            } catch (Exception e) {
                System.err.println("Error processing reservation ID " + reservation.getId() + ": " + e.getMessage());
            }
        }
    }
}