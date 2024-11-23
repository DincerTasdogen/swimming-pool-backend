package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Integer> {
}
