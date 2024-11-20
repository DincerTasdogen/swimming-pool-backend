package com.swimmingpool.repos;

import com.swimmingpool.model.entity.HealthInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthInfoRepository extends JpaRepository<HealthInfo, Integer> {
    HealthInfo findByMemberId(Integer memberId);
}
