package com.example.Graduation.Project.repos;

import com.example.Graduation.Project.entity.HealthInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthInfoRepository extends JpaRepository<HealthInfo, Integer> {
    HealthInfo findByMemberId(Integer memberId);
}
