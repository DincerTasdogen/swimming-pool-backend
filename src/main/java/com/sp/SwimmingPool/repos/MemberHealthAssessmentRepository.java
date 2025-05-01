package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.MemberHealthAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberHealthAssessmentRepository extends JpaRepository<MemberHealthAssessment, Long> {
    Optional<MemberHealthAssessment> findTopByMemberIdOrderByCreatedAtDesc(int memberId);
}
