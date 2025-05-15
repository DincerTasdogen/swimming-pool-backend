package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.exception.EntityNotFoundException;
import com.sp.SwimmingPool.exception.InvalidOperationException;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final SessionRepository sessionRepository;
    private final ReservationRepository reservationRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final MemberService memberService;

    @Transactional(readOnly = true)
    public List<Reservation> getReservationsByMember(int memberId) {
        return reservationRepository.findByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public List<Reservation> getActiveReservationsByMember(int memberId) {
        return reservationRepository.findByMemberIdAndStatus(
                memberId,
                ReservationStatusEnum.CONFIRMED
        );
    }

    @Transactional
    public Reservation createReservation(
            int authenticatedMemberId,
            int sessionId,
            int memberPackageId
    ) {
        LocalDateTime now = LocalDateTime.now();

        Session session = sessionRepository
                .findById(sessionId)
                .orElseThrow(() ->
                        new EntityNotFoundException(sessionId + " ID'li seans bulunamadı.")
                );

        // 1. Booking window: only allow booking for sessions within the next 72 hours
        LocalDateTime sessionStartDateTime = LocalDateTime.of(
                session.getSessionDate(),
                session.getStartTime()
        );
        if (sessionStartDateTime.isAfter(now.plusHours(72))) {
            throw new InvalidOperationException("Sadece önümüzdeki 72 saat için seans rezervasyonu yapabilirsiniz.");
        }
        if (sessionStartDateTime.isBefore(now)) {
            throw new InvalidOperationException("Geçmişteki bir seans için rezervasyon yapamazsınız.");
        }

        if (session.getCurrentBookings() >= session.getCapacity()) {
            throw new InvalidOperationException("Seans dolu.");
        }

        MemberPackage memberPackage = memberPackageRepository
                .findById(memberPackageId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                memberPackageId + " ID'li pakete sahip üye bulunamadı."
                        )
                );
        if (memberPackage.getMemberId() != authenticatedMemberId) {
            throw new InvalidOperationException("Sadece kendi paketinizi kullanabilirsiniz.");
        }
        if (!memberPackage.isActive()) {
            throw new InvalidOperationException("Üye paketiniz aktif değil.");
        }
        if (memberPackage.getSessionsRemaining() <= 0) {
            throw new InvalidOperationException("Seans hakkınız kalmadı.");
        }
        if (memberPackage.getPaymentStatus() != MemberPackagePaymentStatusEnum.COMPLETED) {
            throw new InvalidOperationException("Paketinizin ödemesi henüz tamamlanmamış. Rezervasyon yapamazsınız.");
        }
        if (memberPackage.getPoolId() > 0 && memberPackage.getPoolId() != session.getPoolId()) {
            throw new InvalidOperationException("Üyeliğiniz bu havuz için geçerli değil.");
        }

        PackageType packageType = packageTypeRepository
                .findById(memberPackage.getPackageTypeId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                memberPackage.getPackageTypeId() + " ID'sine ait üyelik paketi bulunamadı."
                        )
                );

        LocalTime packageStartTime = packageType.getStartTime();
        LocalTime packageEndTime = packageType.getEndTime();
        LocalTime effectivePackageEndTime = packageEndTime.equals(LocalTime.MIDNIGHT)
                ? LocalTime.MAX
                : packageEndTime;

        if (
                session.getStartTime().isBefore(packageStartTime) ||
                        session.getEndTime().isAfter(effectivePackageEndTime)
        ) {
            throw new InvalidOperationException(
                    "Bu seans (" + session.getStartTime() + "-" + session.getEndTime() +
                            ") sizin üyelik paketinizde bulunan seans saatleri (" +
                            packageStartTime + " - " + effectivePackageEndTime + ") içerisinde değil, bu saatler için rezervasyon yapamazsınız."
            );
        }

        if (
                packageType.isEducationPackage() && !session.isEducationSession()
        ) {
            throw new InvalidOperationException(
                    "Bu seans bir eğitim seansı değil."
            );
        }

        if (packageType.isRequiresSwimmingAbility()) {
            try {
                if (!memberService.hasSwimmingAbility(authenticatedMemberId)) {
                    throw new InvalidOperationException(
                            "Bu seansa rezervasyon yapabilmeniz için yüzme bilmeniz gerekiyor."
                    );
                }
            } catch (Exception e) {
                throw new RuntimeException(
                        "Yüzme bilgisi doğrulanamadı: " + e.getMessage(),
                        e
                );
            }
        }

        if (hasConflictingReservation(authenticatedMemberId, session)) {
            throw new InvalidOperationException("Bu saatler arasında başka bir rezervasyon yapılmış.");
        }

        // 6. Check if member already has this specific session reserved
        Optional<Reservation> existingReservation =
                reservationRepository.findByMemberIdAndSessionId(authenticatedMemberId, sessionId);
        if (existingReservation.isPresent() && existingReservation.get().getStatus() == ReservationStatusEnum.CONFIRMED) {
            throw new InvalidOperationException("Bu seansa halihazırda rezervasyonunuz bulunmaktadır.");
        }

        // 7. Increment session booking count
        session.setCurrentBookings(session.getCurrentBookings() + 1);
        session.setUpdatedAt(now);
        sessionRepository.save(session);

        // 8. Decrement remaining sessions in package
        memberPackage.setSessionsRemaining(memberPackage.getSessionsRemaining() - 1);
        memberPackageRepository.save(memberPackage);

        // 9. Create and save the reservation
        Reservation reservation = new Reservation();
        reservation.setMemberId(authenticatedMemberId);
        reservation.setSessionId(sessionId);
        reservation.setMemberPackageId(memberPackageId);
        reservation.setStatus(ReservationStatusEnum.CONFIRMED);
        reservation.setCreatedAt(now);
        reservation.setUpdatedAt(now);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void cancelReservation(int reservationId, int authenticatedMemberId) {
        LocalDateTime now = LocalDateTime.now();

        Reservation reservation = reservationRepository
                .findById(reservationId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Reservation not found with ID: " + reservationId
                        )
                );

        if (reservation.getMemberId() != authenticatedMemberId) {
            throw new InvalidOperationException("Sadece kendinize ait rezervasyonu iptal edebilirsiniz.");
        }

        if (reservation.getStatus() != ReservationStatusEnum.CONFIRMED) {
            throw new InvalidOperationException(
                    "Sadece onaylanmış rezervasyonlar iptal edilebilir. Güncel statü: " +
                            reservation.getStatus()
            );
        }

        Session session = sessionRepository
                .findById(reservation.getSessionId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Rezervasyonun ait olduğu seans bulunamadı. (Seans ID: " +
                                        reservation.getSessionId() +
                                        ", Rezervasyon ID: " +
                                        reservationId + ")."
                        )
                );

        LocalDateTime sessionStart = LocalDateTime.of(
                session.getSessionDate(),
                session.getStartTime()
        );
        LocalDateTime cancellationDeadline = sessionStart.minusHours(3);
        if (now.isAfter(cancellationDeadline)) {
            throw new InvalidOperationException(
                    "Rezervasyonunuzu sadece 3 saat öncesine kadar iptal edebilirsiniz. Son iptal saati: " +
                            cancellationDeadline
            );
        }

        // Decrement session booking count
        if (session.getCurrentBookings() > 0) {
            session.setCurrentBookings(session.getCurrentBookings() - 1);
            session.setUpdatedAt(now);
            sessionRepository.save(session);
        } else {
            log.warn(
                    "Attempted to decrement bookings for session {} during cancellation of reservation {}, but current bookings were already 0.",
                    session.getId(),
                    reservationId
            );
        }

        // Return session to package
        MemberPackage memberPackage = memberPackageRepository
                .findById(reservation.getMemberPackageId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Rezervasyona ait üye paketi bulunamadı (Üye paketi ID: " +
                                        reservation.getMemberPackageId() +
                                        ", Rezervasyon ID: " +
                                        reservationId + ")."
                        )
                );
        memberPackage.setSessionsRemaining(memberPackage.getSessionsRemaining() + 1);
        memberPackageRepository.save(memberPackage);

        reservation.setStatus(ReservationStatusEnum.CANCELLED);
        reservation.setUpdatedAt(now);
        reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public boolean hasConflictingReservation(int memberId, Session newSession) {
        List<Reservation> activeReservations = getActiveReservationsByMember(memberId);

        for (Reservation reservation : activeReservations) {
            if (reservation.getSessionId() == newSession.getId()) {
                continue;
            }

            Optional<Session> existingSessionOpt = sessionRepository.findById(
                    reservation.getSessionId()
            );

            if (existingSessionOpt.isPresent()) {
                Session existingSession = existingSessionOpt.get();

                // Check if sessions are on the same date
                if (
                        existingSession.getSessionDate().equals(newSession.getSessionDate())
                ) {
                    // Check for time overlap: (StartA < EndB) and (EndA > StartB)
                    boolean startsBeforeEnds = existingSession
                            .getStartTime()
                            .isBefore(newSession.getEndTime());
                    boolean endsAfterStarts = existingSession
                            .getEndTime()
                            .isAfter(newSession.getStartTime());

                    if (startsBeforeEnds && endsAfterStarts) {
                        return true; // Conflict found
                    }
                }
            }
        }
        return false; // No conflicts found
    }

    @Transactional
    public void markReservationAsCompleted(int reservationId) {
        Reservation reservation = reservationRepository
                .findById(reservationId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Rezervasyon bulunamadı! ID: " + reservationId
                        )
                );

        if (reservation.getStatus() != ReservationStatusEnum.CONFIRMED) {
            throw new InvalidOperationException(
                    "Sadece onaylanmış statüdeki rezervasyonlar tamamlandı olarak işaretlenebilir. Güncel statü: " +
                            reservation.getStatus()
            );
        }

        reservation.setStatus(ReservationStatusEnum.COMPLETED);
        reservation.setUpdatedAt(LocalDateTime.now());
        reservationRepository.save(reservation);
    }

    @Transactional
    public void markReservationAsNoShow(int reservationId) {
        Reservation reservation = reservationRepository
                .findById(reservationId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Rezervasyon bulunamadı! ID: " + reservationId
                        )
                );

        if (reservation.getStatus() != ReservationStatusEnum.CONFIRMED) {
            throw new InvalidOperationException(
                    "Sadece onaylanmış statüdeki rezervasyonlar gelinmedi olarak işaretlenebilir. Güncel statü: " +
                            reservation.getStatus()
            );
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
        List<ReservationStatusEnum> activeStatuses = List.of(
                ReservationStatusEnum.CONFIRMED
        );

        // Find reservations where status is CONFIRMED and session end time is in the past
        List<Reservation> expiredReservations =
                reservationRepository.findExpiredReservations(
                        activeStatuses,
                        currentDate,
                        currentTime
                );

        int processedCount = 0;
        for (Reservation reservation : expiredReservations) {
            try {
                // Double check status just in case
                if (reservation.getStatus() == ReservationStatusEnum.CONFIRMED) {
                    reservation.setStatus(ReservationStatusEnum.NO_SHOW);
                    reservation.setUpdatedAt(now);
                    reservationRepository.save(reservation);
                    processedCount++;
                }
            } catch (Exception e) {
                // Log error and continue with the next one
                log.warn("Error processing missed reservation ID {}: {}", reservation.getId(), e.getMessage());
            }
        }
        if (processedCount > 0) {
            log.info("Processed {} missed reservations as NO_SHOW.", processedCount);
        }
    }
}
