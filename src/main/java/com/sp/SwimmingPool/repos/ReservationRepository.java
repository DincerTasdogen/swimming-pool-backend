package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.Reservation;
import com.sp.SwimmingPool.model.enums.ReservationStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Reservation entities
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    List<Reservation> findByMemberId(int memberId);
    List<Reservation> findByMemberIdAndStatus(int memberId, ReservationStatusEnum status);
    List<Reservation> findByStatus(ReservationStatusEnum status);
    int countByMemberPackageIdAndStatus(int memberPackageId, ReservationStatusEnum status);
    Optional<Reservation> findByMemberIdAndSessionId(int memberId, int sessionId);

    @Query("SELECT r FROM Reservation r JOIN Session s ON r.sessionId = s.id " +
            "WHERE r.status IN :activeStatuses AND " +
            "CONCAT(s.sessionDate, ' ', s.endTime) < :currentDateTime")
    List<Reservation> findExpiredReservations(
            @Param("activeStatuses") List<ReservationStatusEnum> activeStatuses,
            @Param("currentDateTime") LocalDateTime currentDateTime);

    @Query("SELECT r FROM Reservation r JOIN Session s ON r.sessionId = s.id " +
            "WHERE r.memberId = :memberId AND s.sessionDate = :sessionDate AND r.status IN :activeStatuses")
    List<Reservation> findActiveMemberReservationsForDate(
            @Param("memberId") int memberId,
            @Param("sessionDate") LocalDateTime sessionDate,
            @Param("activeStatuses") List<ReservationStatusEnum> activeStatuses);
}