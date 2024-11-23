package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.Pool;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoolRepository extends JpaRepository<Pool, Integer> {
}
