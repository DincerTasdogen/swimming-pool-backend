package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.HealthQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthQuestionRepository extends JpaRepository<HealthQuestion, Long> {
}
