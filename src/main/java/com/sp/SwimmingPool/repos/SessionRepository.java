package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Repository interface for managing Session entities
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, Integer> {

    List<Session> findBySessionDate(LocalDate sessionDate);

    List<Session> findByPoolIdAndSessionDate(int poolId, LocalDate sessionDate);

    boolean existsByPoolIdAndSessionDateAndStartTime(int poolId, LocalDate sessionDate, LocalTime startTime);

    // Fix for the incorrect method name that was causing the error
    @Query("SELECT s FROM Session s WHERE s.sessionDate = :sessionDate AND s.currentBookings < s.capacity")
    List<Session> findBySessionDateAndAvailableCapacity(@Param("sessionDate") LocalDate sessionDate);

    // Or use the correct naming convention for derived query methods
    List<Session> findBySessionDateAndCurrentBookingsLessThan(LocalDate sessionDate, int capacity);

    List<Session> findBySessionDateAndIsEducationSession(LocalDate sessionDate, boolean isEducationSession);
}