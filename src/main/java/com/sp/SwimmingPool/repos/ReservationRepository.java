package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
}
