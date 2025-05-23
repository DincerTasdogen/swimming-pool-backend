package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.SessionResponse;
import com.sp.SwimmingPool.exception.EntityNotFoundException;
import com.sp.SwimmingPool.exception.InvalidOperationException;
import com.sp.SwimmingPool.model.entity.MemberPackage;
import com.sp.SwimmingPool.model.entity.PackageType;
import com.sp.SwimmingPool.model.entity.Pool;
import com.sp.SwimmingPool.model.entity.Session;
import com.sp.SwimmingPool.model.enums.MemberPackagePaymentStatusEnum;
import com.sp.SwimmingPool.repos.MemberPackageRepository;
import com.sp.SwimmingPool.repos.PackageTypeRepository;
import com.sp.SwimmingPool.repos.PoolRepository;
import com.sp.SwimmingPool.repos.SessionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final ReservationService reservationService;
    private final PoolRepository poolRepository;

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
    public List<SessionResponse> getAvailableSessionsForMemberPackage(
            int authenticatedMemberId,
            int memberPackageId,
            int poolId,
            LocalDate date
    ) {
        // 1. Validate the member package
        MemberPackage memberPackage = memberPackageRepository
                .findById(memberPackageId)
                .orElseThrow(() -> new EntityNotFoundException("Üye paketi bulunamadı! ID: " + memberPackageId));

        if (memberPackage.getMemberId() != authenticatedMemberId) {
            throw new InvalidOperationException("Sadece kendi paketinizi kullanabilirsiniz.");
        }
        if (!memberPackage.isActive()) {
            throw new InvalidOperationException("Üye paketiniz henüz aktif durumda değil.");
        }
        if (memberPackage.getSessionsRemaining() <= 0) {
            throw new InvalidOperationException("Paketinizle rezervasyon yapabileceğiniz hakkınız kalmadı.");
        }
        if (memberPackage.getPaymentStatus() != MemberPackagePaymentStatusEnum.COMPLETED) {
            throw new InvalidOperationException("Paketinize ait ödeme henüz tamamlanmadı.");
        }
        if (memberPackage.getPoolId() > 0 && memberPackage.getPoolId() != poolId) {
            throw new InvalidOperationException("Bu üye paketi bu havuza tanımlı değil!");
        }

        // 2. Get package type details
        PackageType packageType = packageTypeRepository
                .findById(memberPackage.getPackageTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Üyelik paketi bulunamadı! ID: " + memberPackage.getPackageTypeId()));

        LocalTime packageStartTime = packageType.getStartTime();
        LocalTime packageEndTime = packageType.getEndTime().equals(LocalTime.MIDNIGHT)
                ? LocalTime.of(23, 59, 59)
                : packageType.getEndTime();

        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        LocalDate currentDate = now.toLocalDate();
        LocalDateTime maxBookingDateTime = now.plusHours(72);
        LocalDate maxBookingDate = maxBookingDateTime.toLocalDate();
        LocalTime maxBookingTime = maxBookingDateTime.toLocalTime();
        String poolName = getPoolNameById(poolId);
        List<Session> sessions = sessionRepository.findAvailableSessionsForPackage(
                poolId,
                date,
                packageStartTime,
                packageEndTime,
                packageType.isEducationPackage(),
                currentDate,
                currentTime,
                maxBookingDate,
                maxBookingTime
        );

        List<SessionResponse> sessionResponses = sessions.stream()
                .map(session -> new SessionResponse(
                        session.getId(),
                        session.getPoolId(),
                        poolName,
                        session.getSessionDate(),
                        session.getStartTime(),
                        session.getEndTime(),
                        session.getCapacity(),
                        session.getCurrentBookings(),
                        session.getCapacity() - session.getCurrentBookings(),
                        session.isEducationSession(),
                        true,
                        null
                ))
                .toList();

        log.info("Converted to {} SessionResponse objects", sessionResponses.size());

        // 8. Apply time conflict filtering
        var memberReservations = reservationService.getMemberConfirmedReservationsOnDate(
                authenticatedMemberId, date
        );

        List<SessionResponse> result = sessionResponses.stream()
                .filter(session -> !reservationService.hasTimeConflict(session, memberReservations))
                .collect(Collectors.toList());

        log.info("Final result after conflict filtering: {} sessions", result.size());

        return result;
    }

    private String getPoolNameById(int poolId) {
        return poolRepository.findById(poolId)
                .map(Pool::getName)
                .orElse("Bulunamadı");
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
        session.setUpdatedAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    @Transactional
    public Session updateSession(int sessionId, Session updatedSession) {
        Session existingSession = getSession(sessionId);

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
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Transactional
    public void decrementSessionBookings(int sessionId) {
        Session session = getSession(sessionId);
        if (session.getCurrentBookings() <= 0) {
            log.warn(
                    "Warning: Attempted to decrement bookings for session {} which already has 0 bookings.",
                    sessionId
            );
            return;
        }
        session.setCurrentBookings(session.getCurrentBookings() - 1);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<Session> getSessionsForPoolInRange(int poolId, LocalDate start, LocalDate end) {
        return sessionRepository.findByPoolIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(poolId, start, end);
    }
}