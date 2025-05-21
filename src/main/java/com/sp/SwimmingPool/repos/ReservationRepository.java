package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.dto.ReservationResponse;
import com.sp.SwimmingPool.model.entity.Reservation;
import com.sp.SwimmingPool.model.enums.ReservationStatusEnum;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    @Query("""
        SELECT new com.sp.SwimmingPool.dto.ReservationResponse(
            r.id, r.memberId, r.sessionId, r.memberPackageId, r.status, r.createdAt, r.updatedAt,
            s.sessionDate, s.startTime, s.endTime, p.name, s.isEducationSession, (s.capacity - s.currentBookings)
        )
        FROM Reservation r
        JOIN Session s ON r.sessionId = s.id
        JOIN Pool p ON s.poolId = p.id
        WHERE r.memberId = :memberId
        ORDER BY s.sessionDate DESC, s.startTime DESC
    """)
    Page<ReservationResponse> findReservationResponsesByMemberId(
            @Param("memberId") int memberId,
            Pageable pageable
    );

    @Query("""
        SELECT r FROM Reservation r
        JOIN Session s ON r.sessionId = s.id
        WHERE r.memberId = :memberId
          AND r.status = :status
          AND s.sessionDate = :sessionDate
    """)
    List<Reservation> findByMemberIdAndStatusAndSessionDate(
            @Param("memberId") int memberId,
            @Param("status") ReservationStatusEnum status,
            @Param("sessionDate") LocalDate sessionDate
    );

    Optional<Reservation> findByMemberIdAndSessionId(int memberId, int sessionId);

    @Query("""
        SELECT r FROM Reservation r
        JOIN Session s ON r.sessionId = s.id
        WHERE r.status IN :activeStatuses
          AND (s.sessionDate < :currentDate OR (s.sessionDate = :currentDate AND s.endTime < :currentTime))
    """)
    List<Reservation> findExpiredReservations(
            @Param("activeStatuses") List<ReservationStatusEnum> activeStatuses,
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") java.time.LocalTime currentTime
    );
}