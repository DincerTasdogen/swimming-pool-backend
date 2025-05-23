package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.dto.SessionResponse;
import com.sp.SwimmingPool.model.entity.Session;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, Integer> {

    @Query("""
    SELECT s FROM Session s
    WHERE s.poolId = :poolId
      AND s.sessionDate = :date
      AND s.currentBookings < s.capacity
      AND s.startTime >= :packageStartTime
      AND s.endTime <= :packageEndTime
      AND (:educationOnly = false OR s.isEducationSession = true)
      AND (
            s.sessionDate > :nowDate
         OR (s.sessionDate = :nowDate AND s.startTime > :nowTime)
      )
      AND (
            s.sessionDate < :maxBookingDate
         OR (s.sessionDate = :maxBookingDate AND s.startTime <= :maxBookingTime)
      )
    ORDER BY s.startTime
""")
    List<Session> findAvailableSessionsForPackage(
            @Param("poolId") int poolId,
            @Param("date") LocalDate date,
            @Param("packageStartTime") LocalTime packageStartTime,
            @Param("packageEndTime") LocalTime packageEndTime,
            @Param("educationOnly") boolean educationOnly,
            @Param("nowDate") LocalDate nowDate,
            @Param("nowTime") LocalTime nowTime,
            @Param("maxBookingDate") LocalDate maxBookingDate,
            @Param("maxBookingTime") LocalTime maxBookingTime
    );

    boolean existsByPoolIdAndSessionDateAndStartTime(int poolId, LocalDate sessionDate, LocalTime startTime);

    List<Session> findBySessionDate(LocalDate date);

    long countByPoolIdAndSessionDate(int id, LocalDate date);

    List<Session> findByPoolIdAndSessionDate(int poolId, LocalDate date);

    List<Session> findByPoolIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            int poolId, LocalDate start, LocalDate end
    );
}