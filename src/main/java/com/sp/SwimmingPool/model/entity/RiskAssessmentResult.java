package com.sp.SwimmingPool.model.entity;

import com.sp.SwimmingPool.model.enums.HealthCategory;
import com.sp.SwimmingPool.model.enums.RiskLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Builder
@Getter
public class RiskAssessmentResult {
    private final double totalScore;
    private final RiskLevel riskLevel;
    private final boolean requiresImmediateReview;
    private final Map<HealthCategory, Integer> categoryScores;
}
