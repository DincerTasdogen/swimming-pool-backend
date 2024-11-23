package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.HealthInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthInfoRepository extends JpaRepository<HealthInfo, Integer> {
    HealthInfo findByMemberId(Integer memberId);
}
