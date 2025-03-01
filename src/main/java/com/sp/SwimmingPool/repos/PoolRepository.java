package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.Pool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PoolRepository extends JpaRepository<Pool, Integer> {
    List<Pool> findByCity(String city);
    List<Pool> findByIsActive(boolean isActive);
    List<Pool> findByCityAndIsActive(String city, boolean isActive);

    @Query(value = "SELECT * FROM pool ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Pool> findRandomPools(int count);
}
