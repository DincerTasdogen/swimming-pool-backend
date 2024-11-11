package com.example.Graduation.Project.repos;

import com.example.Graduation.Project.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
}
