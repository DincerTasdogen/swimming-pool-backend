package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.HealthAnswer;
import com.sp.SwimmingPool.model.entity.HealthQuestion;
import com.sp.SwimmingPool.model.entity.RiskAssessmentResult;
import com.sp.SwimmingPool.model.enums.HealthCategory;
import com.sp.SwimmingPool.model.enums.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceTest {

    @InjectMocks
    private RiskAssessmentService riskAssessmentService;

    private HealthQuestion questionCardiacCritical;
    private HealthQuestion questionMuscularNonCritical;
    private HealthQuestion questionNeuroNonCritical;

    @BeforeEach
    void setUp() {
        questionCardiacCritical = new HealthQuestion();
        questionCardiacCritical.setId(1L);
        questionCardiacCritical.setQuestionText("Do you have heart problems?");
        questionCardiacCritical.setWeight(5);
        questionCardiacCritical.setCriticalQuestion(true);
        questionCardiacCritical.setCategory(HealthCategory.CARDIAC.name());

        questionMuscularNonCritical = new HealthQuestion();
        questionMuscularNonCritical.setId(2L);
        questionMuscularNonCritical.setQuestionText("Do you have muscle pain?");
        questionMuscularNonCritical.setWeight(3);
        questionMuscularNonCritical.setCriticalQuestion(false);
        questionMuscularNonCritical.setCategory(HealthCategory.MUSCULAR.name());

        questionNeuroNonCritical = new HealthQuestion();
        questionNeuroNonCritical.setId(3L);
        questionNeuroNonCritical.setQuestionText("Do you have headaches?");
        questionNeuroNonCritical.setWeight(2);
        questionNeuroNonCritical.setCriticalQuestion(false);
        questionNeuroNonCritical.setCategory(HealthCategory.NEUROLOGICAL.name());
    }

    private HealthAnswer createHealthAnswer(HealthQuestion question, boolean answerValue) {
        HealthAnswer answer = new HealthAnswer();
        answer.setQuestion(question);
        answer.setAnswer(answerValue);
        return answer;
    }

    @Test
    void assessHealthRisk_noAnswers_shouldReturnLowRiskNoReview() {
        RiskAssessmentResult result = riskAssessmentService.assessHealthRisk(Collections.emptyList());

        assertEquals(0.0, result.getTotalScore());
        assertEquals(RiskLevel.LOW, result.getRiskLevel());
        assertFalse(result.isRequiresImmediateReview());
        assertTrue(result.getCategoryScores().isEmpty());
    }

    @Test
    void assessHealthRisk_allNoAnswers_shouldReturnLowRiskNoReview() {
        List<HealthAnswer> answers = List.of(
                createHealthAnswer(questionCardiacCritical, false),
                createHealthAnswer(questionMuscularNonCritical, false)
        );

        RiskAssessmentResult result = riskAssessmentService.assessHealthRisk(answers);

        assertEquals(0.0, result.getTotalScore());
        assertEquals(RiskLevel.LOW, result.getRiskLevel());
        assertFalse(result.isRequiresImmediateReview());
        assertTrue(result.getCategoryScores().isEmpty());
    }

    @Test
    void assessHealthRisk_oneNonCriticalYesAnswer_shouldCalculateScoreAndCategory() {
        List<HealthAnswer> answers = List.of(
                createHealthAnswer(questionMuscularNonCritical, true)
        );

        RiskAssessmentResult result = riskAssessmentService.assessHealthRisk(answers);

        assertEquals(3.0, result.getTotalScore());
        assertEquals(RiskLevel.LOW, result.getRiskLevel()); // 3.0 is LOW
        assertFalse(result.isRequiresImmediateReview());
        assertEquals(1, result.getCategoryScores().size());
        assertEquals(3, result.getCategoryScores().get(HealthCategory.MUSCULAR));
    }

    @Test
    void assessHealthRisk_oneCriticalYesAnswer_shouldRequireReviewAndCalculateScore() {
        List<HealthAnswer> answers = List.of(
                createHealthAnswer(questionCardiacCritical, true)
        );

        RiskAssessmentResult result = riskAssessmentService.assessHealthRisk(answers);

        assertEquals(5.0, result.getTotalScore());
        assertEquals(RiskLevel.LOW, result.getRiskLevel());
        assertTrue(result.isRequiresImmediateReview());
        assertEquals(1, result.getCategoryScores().size());
        assertEquals(5, result.getCategoryScores().get(HealthCategory.CARDIAC));
    }

    @Test
    void assessHealthRisk_multipleYesAnswers_shouldSumScoresAndAggregateCategories() {
        List<HealthAnswer> answers = List.of(
                createHealthAnswer(questionCardiacCritical, true),    // 5, critical
                createHealthAnswer(questionMuscularNonCritical, true), // 3
                createHealthAnswer(questionNeuroNonCritical, false)    // 0
        );

        RiskAssessmentResult result = riskAssessmentService.assessHealthRisk(answers);

        assertEquals(8.0, result.getTotalScore()); // 5 + 3
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel()); // 8.0 is MEDIUM
        assertTrue(result.isRequiresImmediateReview());
        assertEquals(2, result.getCategoryScores().size());
        assertEquals(5, result.getCategoryScores().get(HealthCategory.CARDIAC));
        assertEquals(3, result.getCategoryScores().get(HealthCategory.MUSCULAR));
        assertNull(result.getCategoryScores().get(HealthCategory.NEUROLOGICAL));
    }

    @Test
    void assessHealthRisk_multipleYesAnswersSameCategory_shouldSumCategoryScore() {
        HealthQuestion anotherCardiacQuestion = new HealthQuestion();
        anotherCardiacQuestion.setId(4L);
        anotherCardiacQuestion.setQuestionText("Do you have chest pain?");
        anotherCardiacQuestion.setWeight(4);
        anotherCardiacQuestion.setCriticalQuestion(false);
        anotherCardiacQuestion.setCategory(HealthCategory.CARDIAC.name());

        List<HealthAnswer> answers = List.of(
                createHealthAnswer(questionCardiacCritical, true),    // 5, critical
                createHealthAnswer(anotherCardiacQuestion, true)      // 4
        );

        RiskAssessmentResult result = riskAssessmentService.assessHealthRisk(answers);

        assertEquals(9.0, result.getTotalScore()); // 5 + 4
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());
        assertTrue(result.isRequiresImmediateReview());
        assertEquals(1, result.getCategoryScores().size());
        assertEquals(9, result.getCategoryScores().get(HealthCategory.CARDIAC));
    }


    @Test
    void determineRiskLevel_lowScore() {
        assertEquals(RiskLevel.LOW, riskAssessmentService.determineRiskLevel(0.0));
        assertEquals(RiskLevel.LOW, riskAssessmentService.determineRiskLevel(7.9));
    }

    @Test
    void determineRiskLevel_mediumScore() {
        assertEquals(RiskLevel.MEDIUM, riskAssessmentService.determineRiskLevel(8.0));
        assertEquals(RiskLevel.MEDIUM, riskAssessmentService.determineRiskLevel(14.9));
    }

    @Test
    void determineRiskLevel_highScore() {
        assertEquals(RiskLevel.HIGH, riskAssessmentService.determineRiskLevel(15.0));
        assertEquals(RiskLevel.HIGH, riskAssessmentService.determineRiskLevel(100.0));
    }
}