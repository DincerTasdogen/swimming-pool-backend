package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.exception.EntityNotFoundException;
import com.sp.SwimmingPool.exception.InvalidOperationException;
import com.sp.SwimmingPool.model.entity.MemberPackage;
import com.sp.SwimmingPool.model.entity.PackageType;
import com.sp.SwimmingPool.model.entity.Session;
import com.sp.SwimmingPool.model.enums.MemberPackagePaymentStatusEnum;
import com.sp.SwimmingPool.repos.MemberPackageRepository;
import com.sp.SwimmingPool.repos.PackageTypeRepository;
import com.sp.SwimmingPool.repos.SessionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling session-related operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final ReservationService reservationService;

    @Transactional(readOnly = true)
    public Session getSession(int sessionId) {
        return sessionRepository
                .findById(sessionId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Seans bulunamadı! ID: " + sessionId)
                );
    }

    @Transactional(readOnly = true)
    public List<Session> getSessionsByDate(LocalDate date) {
        return sessionRepository.findBySessionDate(date);
    }

    @Transactional(readOnly = true)
    public List<Session> getSessionsByPoolAndDate(int poolId, LocalDate date) {
        return sessionRepository.findByPoolIdAndSessionDate(poolId, date);
    }

    @Transactional(readOnly = true)
    public List<Session> getAvailableSessionsForMemberPackage(
            int authenticatedMemberId,
            int memberPackageId,
            int poolId,
            LocalDate date
    ) {
        MemberPackage memberPackage = memberPackageRepository
                .findById(memberPackageId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Üye paketi bulunamadı! ID: " + memberPackageId
                        )
                );

        if (memberPackage.getMemberId() != authenticatedMemberId) {
            throw new InvalidOperationException("Sadece kendi paketinizi kullanabilirsiniz.");
        }
        if (!memberPackage.isActive()) {
            throw new InvalidOperationException("Üye paketiniz henüz aktif durumda değil.");
        }
        if (memberPackage.getSessionsRemaining() <= 0) {
            throw new InvalidOperationException(
                    "Paketinizle rezervasyon yapabileceğiniz hakkınız kalmadı."
            );
        }
        if (
                memberPackage.getPaymentStatus() !=
                        MemberPackagePaymentStatusEnum.COMPLETED
        ) {
            throw new InvalidOperationException(
                    "Paketinize ait ödeme henüz tamamlanmadı."
            );
        }

        // 2. Get the package type details
        PackageType packageType = packageTypeRepository
                .findById(memberPackage.getPackageTypeId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Üyelik paketi bulunamadı! ID: " +
                                        memberPackage.getPackageTypeId()
                        )
                );

        if (memberPackage.getPoolId() > 0) { // Package is for a specific pool
            if (memberPackage.getPoolId() != poolId) {
                throw new InvalidOperationException(
                        "Bu üye paketi bu havuza tanımlı değil! Geçerli olduğu havuz: (ID: " +
                                memberPackage.getPoolId() +
                                ")."
                );
            }
        }

        // 4. Get sessions for the *specified pool* and date
        List<Session> sessionsForPoolAndDate =
                sessionRepository.findByPoolIdAndSessionDate(poolId, date);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxBookingDateTime = now.plusHours(72);

        LocalTime packageStartTime = packageType.getStartTime();
        LocalTime packageEndTime = packageType.getEndTime();

        LocalTime effectivePackageEndTime = packageEndTime.equals(LocalTime.MIDNIGHT)
                ? LocalTime.MAX
                : packageEndTime;

        return sessionsForPoolAndDate
                .stream()
                .filter(session -> {
                    if (
                            packageType.isEducationPackage() &&
                                    !session.isEducationSession()
                    ) {
                        return false;
                    }
                    if (
                            session.getStartTime().isBefore(packageStartTime) ||
                                    session.getEndTime().isAfter(effectivePackageEndTime)
                    ) {
                        return false;
                    }
                    if (session.getCurrentBookings() >= session.getCapacity()) {
                        return false;
                    }

                    LocalDateTime sessionStartDateTime = LocalDateTime.of(
                            session.getSessionDate(),
                            session.getStartTime()
                    );
                    LocalDateTime bookingOpenDateTime =
                            sessionStartDateTime.minusHours(72);

                    // Only sessions within the next 72 hours and not in the past
                    boolean withinBookingWindow =
                            !now.isBefore(bookingOpenDateTime) &&
                                    !sessionStartDateTime.isAfter(maxBookingDateTime) &&
                                    sessionStartDateTime.isAfter(now);

                    if (!withinBookingWindow) {
                        log.debug("Session {} filtered out by booking window: Start={}, Now={}, Open={}, Max={}",
                                session.getId(), sessionStartDateTime, now, bookingOpenDateTime, maxBookingDateTime);
                        return false;
                    }

                    try {
                        return !reservationService.hasConflictingReservation(
                                authenticatedMemberId,
                                session
                        );
                    } catch (Exception e) {
                        log.error(
                                "Error checking conflicting reservation for session {}: {}",
                                session.getId(),
                                e.getMessage()
                        );
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }


    @Transactional
    public Session createSession(Session session) {
        if (session.getStartTime().isAfter(session.getEndTime())) {
            throw new InvalidOperationException(
                    "Başlangıç zamanı bitiş zamanından önce olmalı!"
            );
        }
        if (session.getCapacity() <= 0) {
            throw new InvalidOperationException(
                    "Kapasite 0'dan büyük olmalı!"
            );
        }
        boolean exists = sessionRepository.existsByPoolIdAndSessionDateAndStartTime(
                session.getPoolId(),
                session.getSessionDate(),
                session.getStartTime()
        );
        if (exists) {
            throw new InvalidOperationException(
                    "Bu zaman aralığına, bu güne ve bu havuza ait bir seans zaten tanımlı!"
            );
        }
        session.setCurrentBookings(0);
        session.setCreatedAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    @Transactional
    public Session updateSession(int sessionId, Session updatedSession) {
        Session existingSession = getSession(sessionId); // Uses method with exception

        if (updatedSession.getCapacity() < existingSession.getCurrentBookings()) {
            throw new InvalidOperationException(
                    "Seans kapasitesi mevcut rezervasyonun altına düşürülemez! Güncel rezervasyon sayısı: (" +
                            existingSession.getCurrentBookings() +
                            ")"
            );
        }
        existingSession.setCapacity(updatedSession.getCapacity());
        existingSession.setEducationSession(updatedSession.isEducationSession());
        existingSession.setUpdatedAt(LocalDateTime.now());
        return sessionRepository.save(existingSession);
    }

    @Transactional
    public void deleteSession(int sessionId) {
        Session session = getSession(sessionId);
        if (session.getCurrentBookings() > 0) {
            throw new InvalidOperationException(
                    "Aktif rezervasyonu bulunan seans iptal edilemez!"
            );
        }
        sessionRepository.delete(session);
    }

    @Transactional
    public Session updateSessionEducationStatus(
            int sessionId,
            boolean isEducationSession
    ) {
        Session session = getSession(sessionId);
        session.setEducationSession(isEducationSession);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
        return session;
    }

    @Transactional
    public void incrementSessionBookings(int sessionId) {
        Session session = getSession(sessionId);
        if (session.getCurrentBookings() >= session.getCapacity()) {
            throw new InvalidOperationException(
                    "Seans zaten dolu!"
            );
        }
        session.setCurrentBookings(session.getCurrentBookings() + 1);
        session.setUpdatedAt(LocalDateTime.now()); // Set update time
        sessionRepository.save(session);
    }

    @Transactional
    public void decrementSessionBookings(int sessionId) {
        Session session = getSession(sessionId); // Uses method with exception
        if (session.getCurrentBookings() <= 0) {
            // This might indicate a data inconsistency, log a warning
            System.err.println(
                    "Warning: Attempted to decrement bookings for session " +
                            sessionId +
                            " which already has 0 bookings."
            );
            // Optionally throw, or just return without changing
            return;
            // throw new InvalidOperationException("Session has no bookings to decrement");
        }
        session.setCurrentBookings(session.getCurrentBookings() - 1);
        session.setUpdatedAt(LocalDateTime.now()); // Set update time
        sessionRepository.save(session);
    }
}
