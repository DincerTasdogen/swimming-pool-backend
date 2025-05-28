package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.AnswerRequest; // Your DTO
import com.sp.SwimmingPool.model.entity.HealthAnswer;
import com.sp.SwimmingPool.model.entity.HealthQuestion;
import com.sp.SwimmingPool.model.entity.MemberHealthAssessment;
import com.sp.SwimmingPool.model.entity.RiskAssessmentResult;
import com.sp.SwimmingPool.model.enums.HealthCategory;
import com.sp.SwimmingPool.model.enums.RiskLevel;
import com.sp.SwimmingPool.repos.HealthQuestionRepository;
import com.sp.SwimmingPool.repos.MemberHealthAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthAssessmentServiceTest {

    @Mock
    private MemberHealthAssessmentRepository healthAssessmentRepository;
    @Mock
    private HealthQuestionRepository healthQuestionRepository;
    @Mock
    private RiskAssessmentService riskAssessmentService;

    @InjectMocks
    private HealthAssessmentService healthAssessmentService;

    private HealthQuestion question1;
    private HealthQuestion question2_critical;
    private AnswerRequest answerRequest1_yes;
    private AnswerRequest answerRequest2_no_critical;
    private RiskAssessmentResult lowRiskResult;
    private RiskAssessmentResult highRiskResult_criticalAnswer;

    private final int memberId = 101;

    @BeforeEach
    void setUp() {
        question1 = new HealthQuestion();
        question1.setId(1L);
        question1.setQuestionText("Do you exercise regularly?");
        question1.setWeight(2);
        question1.setCriticalQuestion(false);
        question1.setCategory(HealthCategory.GENERAL.name());

        question2_critical = new HealthQuestion();
        question2_critical.setId(2L);
        question2_critical.setQuestionText("Do you have chest pain?");
        question2_critical.setWeight(5);
        question2_critical.setCriticalQuestion(true);
        question2_critical.setCategory(HealthCategory.CARDIAC.name());

        // Correctly instantiate AnswerRequest using setters
        answerRequest1_yes = new AnswerRequest();
        answerRequest1_yes.setQuestionId(question1.getId());
        answerRequest1_yes.setAnswer(true);
        answerRequest1_yes.setNotes("Daily walks");

        answerRequest2_no_critical = new AnswerRequest();
        answerRequest2_no_critical.setQuestionId(question2_critical.getId());
        answerRequest2_no_critical.setAnswer(false);
        answerRequest2_no_critical.setNotes(null); // Or "" if appropriate

        lowRiskResult = RiskAssessmentResult.builder()
                .totalScore(2.0)
                .riskLevel(RiskLevel.LOW)
                .requiresImmediateReview(false)
                .categoryScores(Map.of(HealthCategory.GENERAL, 2))
                .build();

        highRiskResult_criticalAnswer = RiskAssessmentResult.builder()
                .totalScore(5.0)
                .riskLevel(RiskLevel.LOW)
                .requiresImmediateReview(true)
                .categoryScores(Map.of(HealthCategory.CARDIAC, 5))
                .build();
    }

    @Test
    void createHealthAssessmentForMember_success_lowRisk_noReportNeeded() {
        List<AnswerRequest> answersDto = List.of(answerRequest1_yes, answerRequest2_no_critical);

        when(healthQuestionRepository.findById(question1.getId())).thenReturn(Optional.of(question1));
        when(healthQuestionRepository.findById(question2_critical.getId())).thenReturn(Optional.of(question2_critical));
        when(riskAssessmentService.assessHealthRisk(anyList())).thenReturn(lowRiskResult);
        when(healthAssessmentRepository.save(any(MemberHealthAssessment.class)))
                .thenAnswer(invocation -> {
                    MemberHealthAssessment mha = invocation.getArgument(0);
                    mha.setId(1L);
                    return mha;
                });

        MemberHealthAssessment result = healthAssessmentService.createHealthAssessmentForMember(memberId, answersDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(memberId, result.getMemberId());
        assertEquals(2, result.getAnswers().size());
        assertEquals(lowRiskResult.getTotalScore(), result.getRiskScore());
        assertFalse(result.isRequiresMedicalReport());
        assertFalse(result.isDoctorApproved());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        ArgumentCaptor<MemberHealthAssessment> captor = ArgumentCaptor.forClass(MemberHealthAssessment.class);
        verify(healthAssessmentRepository).save(captor.capture());
        MemberHealthAssessment savedAssessment = captor.getValue();
        assertEquals(memberId, savedAssessment.getMemberId());
        assertEquals(2, savedAssessment.getAnswers().size());
        assertTrue(savedAssessment.getAnswers().stream().anyMatch(a -> a.getQuestion().getId().equals(question1.getId()) && a.isAnswer()));
        assertTrue(savedAssessment.getAnswers().stream().anyMatch(a -> a.getQuestion().getId().equals(question2_critical.getId()) && !a.isAnswer()));

        ArgumentCaptor<List<HealthAnswer>> healthAnswerListCaptor = ArgumentCaptor.forClass(List.class);
        verify(riskAssessmentService).assessHealthRisk(healthAnswerListCaptor.capture());
        assertEquals(2, healthAnswerListCaptor.getValue().size());
    }

    @Test
    void createHealthAssessmentForMember_success_criticalAnswerYes_requiresReport() {
        AnswerRequest answerRequestCriticalYes = new AnswerRequest();
        answerRequestCriticalYes.setQuestionId(question2_critical.getId());
        answerRequestCriticalYes.setAnswer(true);
        answerRequestCriticalYes.setNotes("Occasional pain");
        List<AnswerRequest> answersDto = List.of(answerRequest1_yes, answerRequestCriticalYes);

        when(healthQuestionRepository.findById(question1.getId())).thenReturn(Optional.of(question1));
        when(healthQuestionRepository.findById(question2_critical.getId())).thenReturn(Optional.of(question2_critical));
        when(riskAssessmentService.assessHealthRisk(anyList())).thenReturn(highRiskResult_criticalAnswer);
        when(healthAssessmentRepository.save(any(MemberHealthAssessment.class)))
                .thenAnswer(invocation -> {
                    MemberHealthAssessment mha = invocation.getArgument(0);
                    mha.setId(2L);
                    return mha;
                });

        MemberHealthAssessment result = healthAssessmentService.createHealthAssessmentForMember(memberId, answersDto);

        assertNotNull(result);
        assertEquals(highRiskResult_criticalAnswer.getTotalScore(), result.getRiskScore());
        assertTrue(result.isRequiresMedicalReport());
    }

    @Test
    void createHealthAssessmentForMember_success_highRiskScore_requiresReport() {
        List<AnswerRequest> answersDto = List.of(answerRequest1_yes);
        RiskAssessmentResult highRiskScoreResult = RiskAssessmentResult.builder()
                .totalScore(20.0)
                .riskLevel(RiskLevel.HIGH)
                .requiresImmediateReview(false)
                .categoryScores(Map.of(HealthCategory.GENERAL, 20))
                .build();

        when(healthQuestionRepository.findById(question1.getId())).thenReturn(Optional.of(question1));
        when(riskAssessmentService.assessHealthRisk(anyList())).thenReturn(highRiskScoreResult);
        when(healthAssessmentRepository.save(any(MemberHealthAssessment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MemberHealthAssessment result = healthAssessmentService.createHealthAssessmentForMember(memberId, answersDto);

        assertNotNull(result);
        assertTrue(result.isRequiresMedicalReport());
    }

    @Test
    void createHealthAssessmentForMember_nullAnswersDto_throwsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> healthAssessmentService.createHealthAssessmentForMember(memberId, null));
        assertEquals("Answers list cannot be null or empty", exception.getMessage());
    }

    @Test
    void createHealthAssessmentForMember_emptyAnswersDto_throwsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> healthAssessmentService.createHealthAssessmentForMember(memberId, Collections.emptyList()));
        assertEquals("Answers list cannot be null or empty", exception.getMessage());
    }

    @Test
    void createHealthAssessmentForMember_answerWithNullQuestionId_throwsIllegalArgumentException() {
        AnswerRequest answerWithNullQId = new AnswerRequest();
        // answerWithNullQId.setQuestionId(null); // This is the default for Long
        answerWithNullQId.setAnswer(true);
        answerWithNullQId.setNotes("notes");
        List<AnswerRequest> answersDto = List.of(answerWithNullQId);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> healthAssessmentService.createHealthAssessmentForMember(memberId, answersDto));
        assertEquals("Question ID cannot be null in an answer request", exception.getMessage());
    }

    @Test
    void createHealthAssessmentForMember_questionNotFoundForAnswer_throwsIllegalArgumentException() {
        Long nonExistentQuestionId = 99L;
        AnswerRequest answerForNonExistentQ = new AnswerRequest();
        answerForNonExistentQ.setQuestionId(nonExistentQuestionId);
        answerForNonExistentQ.setAnswer(true);
        answerForNonExistentQ.setNotes("notes");
        List<AnswerRequest> answersDto = List.of(answerForNonExistentQ);

        when(healthQuestionRepository.findById(nonExistentQuestionId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> healthAssessmentService.createHealthAssessmentForMember(memberId, answersDto));
        assertEquals("HealthQuestion not found with id: " + nonExistentQuestionId, exception.getMessage());
    }
}