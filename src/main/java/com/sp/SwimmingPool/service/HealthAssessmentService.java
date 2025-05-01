package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.AnswerRequest;
import com.sp.SwimmingPool.model.entity.HealthAnswer;
import com.sp.SwimmingPool.model.entity.HealthQuestion;
import com.sp.SwimmingPool.model.entity.MemberHealthAssessment;
import com.sp.SwimmingPool.model.entity.RiskAssessmentResult;
import com.sp.SwimmingPool.model.enums.RiskLevel;
import com.sp.SwimmingPool.repos.HealthQuestionRepository;
import com.sp.SwimmingPool.repos.MemberHealthAssessmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class HealthAssessmentService {

    private final MemberHealthAssessmentRepository healthAssessmentRepository;
    private final HealthQuestionRepository questionRepository;
    private final RiskAssessmentService riskAssessmentService;

    /**
     * Creates a health assessment for a member after registration,
     * using the answers collected during registration (from temp data).
     */
    public MemberHealthAssessment createHealthAssessmentForMember(int memberId, List<AnswerRequest> answersDto) {
        log.info("Creating health assessment for member ID: {}", memberId);

        if (answersDto == null || answersDto.isEmpty()) {
            throw new IllegalArgumentException("Answers list cannot be null or empty");
        }

        MemberHealthAssessment assessment = new MemberHealthAssessment();
        assessment.setMemberId(memberId);
        assessment.setCreatedAt(LocalDateTime.now());
        assessment.setUpdatedAt(LocalDateTime.now());

        // Convert AnswerRequest DTOs to HealthAnswer entities
        List<HealthAnswer> answers = answersDto.stream()
                .map(answerRequest -> processAnswerRequest(answerRequest, assessment))
                .collect(Collectors.toList());

        assessment.setAnswers(answers);

        // Calculate risk
        log.debug("Calculating risk for {} answers", answers.size());
        RiskAssessmentResult riskResult = riskAssessmentService.assessHealthRisk(answers);
        assessment.setRiskScore(riskResult.getTotalScore());
        log.info("Calculated risk score: {}, Level: {}, Requires Review: {}",
                riskResult.getTotalScore(), riskResult.getRiskLevel(), riskResult.isRequiresImmediateReview());

        if (riskResult.isRequiresImmediateReview() || riskResult.getRiskLevel() == RiskLevel.HIGH) {
            log.warn("Assessment for member {} requires a medical report due to high risk or critical answers.", memberId);
            assessment.setRequiresMedicalReport(true);
        } else {
            assessment.setRequiresMedicalReport(false);
        }

        MemberHealthAssessment savedAssessment = healthAssessmentRepository.save(assessment);
        log.info("Successfully saved health assessment with ID: {} for member ID: {}", savedAssessment.getId(), savedAssessment.getMemberId());

        return savedAssessment;
    }

    /**
     * Helper to convert AnswerRequest DTO to HealthAnswer entity.
     */
    private HealthAnswer processAnswerRequest(AnswerRequest answerRequest, MemberHealthAssessment assessment) {
        if (answerRequest.getQuestionId() == null) {
            throw new IllegalArgumentException("Question ID cannot be null in an answer request");
        }

        HealthQuestion question = questionRepository.findById(answerRequest.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "HealthQuestion not found with id: " + answerRequest.getQuestionId()
                ));

        HealthAnswer answer = new HealthAnswer();
        answer.setQuestion(question);
        answer.setAnswer(answerRequest.isAnswer());
        answer.setAdditionalNotes(answerRequest.getNotes());
        answer.setHealthAssessment(assessment); // Set the owning side of the relationship

        return answer;
    }
}
