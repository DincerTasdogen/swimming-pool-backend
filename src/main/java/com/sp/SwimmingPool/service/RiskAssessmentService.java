    package com.sp.SwimmingPool.service;

    import com.sp.SwimmingPool.model.entity.HealthAnswer;
    import com.sp.SwimmingPool.model.entity.HealthQuestion;
    import com.sp.SwimmingPool.model.entity.RiskAssessmentResult;
    import com.sp.SwimmingPool.model.enums.HealthCategory;
    import com.sp.SwimmingPool.model.enums.RiskLevel;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Service;

    import java.util.EnumMap;
    import java.util.List;
    import java.util.Map;

    @Service
    @Slf4j
    public class RiskAssessmentService {

        public RiskAssessmentResult assessHealthRisk(List<HealthAnswer> answers) {
            double totalScore = 0;
            boolean requiresImmediateReview = false;
            Map<HealthCategory, Integer> categoryScores = new EnumMap<>(HealthCategory.class);

            for (HealthAnswer answer : answers) {
                if (answer.isAnswer()) {
                    HealthQuestion question = answer.getQuestion();
                    totalScore += question.getWeight();

                    HealthCategory category = HealthCategory.valueOf(question.getCategory());
                    categoryScores.merge(category, question.getWeight(), Integer::sum);

                    if (question.isCriticalQuestion()) {
                        requiresImmediateReview = true;
                    }
                }
            }

            return RiskAssessmentResult.builder()
                    .totalScore(totalScore)
                    .riskLevel(determineRiskLevel(totalScore))
                    .requiresImmediateReview(requiresImmediateReview)
                    .categoryScores(categoryScores)
                    .build();
        }

        private RiskLevel determineRiskLevel(double score) {
            double highRiskThreshold = 15.0;
            if (score >= highRiskThreshold) return RiskLevel.HIGH;
            double mediumRiskThreshold = 8.0;
            if (score >= mediumRiskThreshold) return RiskLevel.MEDIUM;
            return RiskLevel.LOW;
        }
    }
