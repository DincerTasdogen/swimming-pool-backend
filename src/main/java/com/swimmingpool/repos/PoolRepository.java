package com.swimmingpool.repos;

import com.swimmingpool.model.entity.Pool;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoolRepository extends JpaRepository<Pool, Integer> {
}
