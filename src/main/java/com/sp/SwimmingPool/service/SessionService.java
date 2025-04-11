package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.MemberPackage;
import com.sp.SwimmingPool.model.entity.PackageType;
import com.sp.SwimmingPool.model.entity.Session;
import com.sp.SwimmingPool.model.enums.MemberPackagePaymentStatusEnum;
import com.sp.SwimmingPool.repos.MemberPackageRepository;
import com.sp.SwimmingPool.repos.PackageTypeRepository;
import com.sp.SwimmingPool.repos.SessionRepository;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for handling session-related operations
 */
@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final PackageTypeRepository packageTypeRepository;

    @Setter
    private ReservationService reservationService;

    public SessionService(
            SessionRepository sessionRepository,
            MemberPackageRepository memberPackageRepository,
            PackageTypeRepository packageTypeRepository,
            @Lazy ReservationService reservationService) {
        this.sessionRepository = sessionRepository;
        this.memberPackageRepository = memberPackageRepository;
        this.packageTypeRepository = packageTypeRepository;
        this.reservationService = reservationService;
    }

    public Session getSession(int sessionId) throws Exception {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new Exception("Session not found"));
    }

    public List<Session> getSessionsByDate(LocalDate date) {
        return sessionRepository.findBySessionDate(date);
    }

    public List<Session> getSessionsByPoolAndDate(int poolId, LocalDate date) {
        return sessionRepository.findByPoolIdAndSessionDate(poolId, date);
    }

    public List<Session> getAvailableSessionsForMemberPackage(int memberId, int memberPackageId, LocalDate date)
            throws Exception {

        // 1. Verify member package exists and is active
        Optional<MemberPackage> memberPackageOpt = memberPackageRepository.findById(memberPackageId);
        if (memberPackageOpt.isEmpty()) {
            throw new Exception("Member package not found");
        }

        MemberPackage memberPackage = memberPackageOpt.get();

        if (!memberPackage.isActive()) {
            throw new Exception("Member package is not active");
        }

        if (memberPackage.getSessionsRemaining() <= 0) {
            throw new Exception("No sessions remaining in package");
        }

        if (memberPackage.getPaymentStatus() != MemberPackagePaymentStatusEnum.COMPLETED) {
            throw new Exception("Package payment has not been completed");
        }

        // 2. Get the package type details
        Optional<PackageType> packageTypeOpt = packageTypeRepository.findById(memberPackage.getPackageTypeId());
        if (packageTypeOpt.isEmpty()) {
            throw new Exception("Package type not found");
        }

        PackageType packageType = packageTypeOpt.get();

        // 3. Get sessions for the date
        List<Session> sessions = sessionRepository.findBySessionDate(date);

        // 4. Filter sessions based on package restrictions
        return sessions.stream()
                .filter(session -> {
                    // Filter by pool ID if package is restricted to specific pool
                    if (memberPackage.getPoolId() > 0 && session.getPoolId() != memberPackage.getPoolId()) {
                        return false;
                    }

                    // Filter by education session flag
                    if (packageType.isEducationPackage() && !session.isEducationSession()) {
                        return false;
                    }

                    // Filter by time window
                    if (session.getStartTime().isBefore(packageType.getStartTime()) ||
                            session.getEndTime().isAfter(packageType.getEndTime())) {
                        return false;
                    }

                    // Filter by capacity
                    if (session.getCurrentBookings() >= session.getCapacity()) {
                        return false;
                    }

                    // Check if session can be reserved (72-hour rule)
                    LocalDateTime sessionDateTime = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
                    LocalDateTime now = LocalDateTime.now();

                    // Must be within 72 hours (not too early to book)
                    if (sessionDateTime.minusHours(72).isAfter(now)) {
                        return false;
                    }

                    // Check if member already has conflicting reservation
                    try {
                        return !reservationService.hasConflictingReservation(memberId, session);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Session createSession(Session session) throws Exception {
        // Validate session data
        if (session.getStartTime().isAfter(session.getEndTime())) {
            throw new Exception("Start time must be before end time");
        }

        if (session.getCapacity() <= 0) {
            throw new Exception("Capacity must be greater than zero");
        }

        // Check for duplicate session
        boolean exists = sessionRepository.existsByPoolIdAndSessionDateAndStartTime(
                session.getPoolId(), session.getSessionDate(), session.getStartTime());

        if (exists) {
            throw new Exception("A session already exists for this pool, date and time");
        }

        // Set default values
        session.setCurrentBookings(0);
        session.setCreatedAt(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    @Transactional
    public Session updateSession(int sessionId, Session updatedSession) throws Exception {
        Optional<Session> existingSessionOpt = sessionRepository.findById(sessionId);
        if (existingSessionOpt.isEmpty()) {
            throw new Exception("Session not found");
        }

        Session existingSession = existingSessionOpt.get();

        // Don't allow reducing capacity below current bookings
        if (updatedSession.getCapacity() < existingSession.getCurrentBookings()) {
            throw new Exception("Cannot reduce capacity below current bookings");
        }

        // Update fields
        existingSession.setCapacity(updatedSession.getCapacity());
        existingSession.setEducationSession(updatedSession.isEducationSession());

        // Save and return
        return sessionRepository.save(existingSession);
    }

    @Transactional
    public void deleteSession(int sessionId) throws Exception {
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new Exception("Session not found");
        }

        Session session = sessionOpt.get();

        // Don't allow deleting session with active bookings
        if (session.getCurrentBookings() > 0) {
            throw new Exception("Cannot delete session with active bookings");
        }

        sessionRepository.delete(session);
    }


    @Transactional
    public void incrementSessionBookings(int sessionId) throws Exception {
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new Exception("Session not found");
        }

        Session session = sessionOpt.get();

        if (session.getCurrentBookings() >= session.getCapacity()) {
            throw new Exception("Session is already at full capacity");
        }

        session.setCurrentBookings(session.getCurrentBookings() + 1);
        sessionRepository.save(session);
    }

    @Transactional
    public void decrementSessionBookings(int sessionId) throws Exception {
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new Exception("Session not found");
        }

        Session session = sessionOpt.get();

        if (session.getCurrentBookings() <= 0) {
            throw new Exception("Session has no bookings to decrement");
        }

        session.setCurrentBookings(session.getCurrentBookings() - 1);
        sessionRepository.save(session);
    }
}