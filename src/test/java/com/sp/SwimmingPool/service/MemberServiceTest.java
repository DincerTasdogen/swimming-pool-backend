package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.HealthAnswerDTO;
import com.sp.SwimmingPool.dto.MemberDTO;
import com.sp.SwimmingPool.dto.MemberHealthAssessmentDTO;
import com.sp.SwimmingPool.dto.MemberReportStatusDTO;
import com.sp.SwimmingPool.model.entity.HealthAnswer;
import com.sp.SwimmingPool.model.entity.HealthQuestion;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.MemberHealthAssessment;
import com.sp.SwimmingPool.model.enums.*;
import com.sp.SwimmingPool.repos.MemberHealthAssessmentRepository;
import com.sp.SwimmingPool.repos.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MemberHealthAssessmentRepository assessmentRepository;
    @Mock
    private MemberFileService memberFileService;
    @Mock
    private EmailService emailService;
    @Mock
    private RiskAssessmentService riskAssessmentService;
    @Mock
    private StorageService storageService;
    @Mock
    private CoachAssignmentService coachAssignmentService;

    @InjectMocks
    private MemberService memberService;

    private Member member1_active;
    private Member member2_pending;
    private MemberDTO memberDTO1;
    private MemberHealthAssessment assessment1;
    private LocalDateTime fixedTime;


    @BeforeEach
    void setUp() {
        fixedTime = LocalDateTime.now();

        member1_active = new Member();
        member1_active.setId(1);
        member1_active.setName("John");
        member1_active.setSurname("Doe");
        member1_active.setEmail("john.doe@example.com");
        member1_active.setIdentityNumber("11122233344");
        member1_active.setGender(MemberGenderEnum.MALE);
        member1_active.setWeight(75.5);
        member1_active.setHeight(180.0);
        member1_active.setBirthDate(LocalDate.of(1990, 5, 15));
        member1_active.setPhoneNumber("555-0101");
        member1_active.setCanSwim(true);
        member1_active.setSwimmingLevel(SwimmingLevelEnum.INTERMEDIATE);
        member1_active.setStatus(StatusEnum.ACTIVE);
        member1_active.setCoachId(101);
        member1_active.setRegistrationDate(fixedTime.minusMonths(1));
        member1_active.setUpdatedAt(fixedTime.minusDays(1));
        member1_active.setPassword("encodedPassword");


        member2_pending = new Member();
        member2_pending.setId(2);
        member2_pending.setName("Jane");
        member2_pending.setSurname("Roe");
        member2_pending.setEmail("jane.roe@example.com");
        member2_pending.setIdentityNumber("55566677788");
        member2_pending.setGender(MemberGenderEnum.FEMALE);
        member2_pending.setStatus(StatusEnum.PENDING_ID_CARD_VERIFICATION);
        member2_pending.setCanSwim(false);
        member2_pending.setSwimmingLevel(SwimmingLevelEnum.NONE);
        member2_pending.setRegistrationDate(fixedTime.minusDays(5));
        member2_pending.setUpdatedAt(fixedTime.minusDays(2));
        member2_pending.setPassword("encodedPassword2");


        memberDTO1 = new MemberDTO();
        memberDTO1.setId(member1_active.getId());
        memberDTO1.setName(member1_active.getName());
        memberDTO1.setSurname(member1_active.getSurname());
        memberDTO1.setEmail(member1_active.getEmail());
        memberDTO1.setIdentityNumber(member1_active.getIdentityNumber());
        memberDTO1.setGender(member1_active.getGender().name());
        memberDTO1.setWeight(member1_active.getWeight());
        memberDTO1.setHeight(member1_active.getHeight());
        memberDTO1.setBirthDate(member1_active.getBirthDate());
        memberDTO1.setPhoneNumber(member1_active.getPhoneNumber());
        memberDTO1.setCanSwim(member1_active.isCanSwim());
        memberDTO1.setSwimmingLevel(member1_active.getSwimmingLevel().getDisplayName()); // Use display name
        memberDTO1.setStatus(member1_active.getStatus().name());
        memberDTO1.setCoachId(member1_active.getCoachId());
        memberDTO1.setRegistrationDate(member1_active.getRegistrationDate());
        memberDTO1.setUpdatedDate(member1_active.getUpdatedAt());


        assessment1 = new MemberHealthAssessment();
        assessment1.setId(1L);
        assessment1.setMemberId(member1_active.getId());
        assessment1.setMedicalReportPath("s3key/report.pdf");
        assessment1.setRequiresMedicalReport(false);
        assessment1.setDoctorApproved(true);
        assessment1.setDoctorNotes("Looks good.");
        assessment1.setUpdatedAt(fixedTime.minusHours(12));
        assessment1.setRiskScore(5.0); // Example score

        HealthQuestion q1 = new HealthQuestion(); q1.setId(1L); q1.setQuestionText("Q1 Text");
        HealthQuestion q2 = new HealthQuestion(); q2.setId(2L); q2.setQuestionText("Q2 Text");

        HealthAnswer ans1 = new HealthAnswer();
        ans1.setQuestion(q1);
        ans1.setAnswer(true);
        ans1.setAdditionalNotes("Notes for Q1");
        ans1.setHealthAssessment(assessment1); // Link back

        HealthAnswer ans2 = new HealthAnswer();
        ans2.setQuestion(q2);
        ans2.setAnswer(false);
        ans2.setHealthAssessment(assessment1); // Link back

        assessment1.setAnswers(List.of(ans1, ans2));
    }

    @Test
    void updateMember_memberExists_updatesAndReturnsDTO() {
        int memberIdToUpdate = member1_active.getId();
        MemberDTO updatedDetailsDTO = new MemberDTO();
        updatedDetailsDTO.setName("Johnny");
        updatedDetailsDTO.setSurname("Doer");
        updatedDetailsDTO.setEmail(member1_active.getEmail()); // Email usually not changed here
        updatedDetailsDTO.setIdentityNumber(member1_active.getIdentityNumber());
        updatedDetailsDTO.setGender(MemberGenderEnum.MALE.name());
        updatedDetailsDTO.setWeight(78.0);
        updatedDetailsDTO.setHeight(181.0);
        updatedDetailsDTO.setBirthDate(member1_active.getBirthDate());
        updatedDetailsDTO.setPhoneNumber("555-0199");
        updatedDetailsDTO.setCanSwim(true);
        updatedDetailsDTO.setSwimmingLevel(SwimmingLevelEnum.ADVANCED.getDisplayName());
        updatedDetailsDTO.setStatus(StatusEnum.ACTIVE.name());
        updatedDetailsDTO.setCoachId(102);
        // Timestamps are usually handled by service/JPA

        LocalDateTime originalUpdatedAt = member1_active.getUpdatedAt();

        when(memberRepository.findById(memberIdToUpdate)).thenReturn(Optional.of(member1_active));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // Mock file service calls for DTO conversion
        when(memberFileService.getBiometricPhotoPath(memberIdToUpdate)).thenReturn(Optional.of("photo.jpg"));


        MemberDTO resultDTO = memberService.updateMember(memberIdToUpdate, updatedDetailsDTO);

        assertNotNull(resultDTO);
        assertEquals("Johnny", resultDTO.getName());
        assertEquals(78.0, resultDTO.getWeight());
        assertEquals(SwimmingLevelEnum.ADVANCED.getDisplayName(), resultDTO.getSwimmingLevel());
        assertEquals(102, resultDTO.getCoachId());
        assertNotNull(resultDTO.getUpdatedDate());
        assertTrue(resultDTO.getUpdatedDate().isAfter(originalUpdatedAt) || resultDTO.getUpdatedDate().isEqual(originalUpdatedAt));


        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();
        assertEquals("Johnny", savedMember.getName());
        assertEquals(SwimmingLevelEnum.ADVANCED, savedMember.getSwimmingLevel());
        assertTrue(savedMember.isCanSwim()); // Should be updated based on ADVANCED level
    }

    @Test
    void updateMember_memberNotFound_throwsIllegalArgumentException() {
        when(memberRepository.findById(999)).thenReturn(Optional.empty());
        MemberDTO dto = new MemberDTO(); // Dummy DTO
        assertThrows(IllegalArgumentException.class, () -> memberService.updateMember(999, dto));
    }

    @Test
    void updatePassword_memberExists_updatesEncodedPassword() {
        String newPassword = "newPasswordStrong";
        String encodedNewPassword = "encodedNewPasswordStrong";
        when(memberRepository.findByEmail(member1_active.getEmail())).thenReturn(Optional.of(member1_active));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);

        memberService.updatePassword(member1_active.getEmail(), newPassword);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertEquals(encodedNewPassword, memberCaptor.getValue().getPassword());
        assertNotNull(memberCaptor.getValue().getUpdatedAt());
    }

    @Test
    void findByEmail_memberExists_returnsDTO() {
        when(memberRepository.findByEmail(member1_active.getEmail())).thenReturn(Optional.of(member1_active));
        when(memberFileService.getBiometricPhotoPath(member1_active.getId())).thenReturn(Optional.empty());
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(member1_active.getId())).thenReturn(Optional.empty());


        MemberDTO result = memberService.findByEmail(member1_active.getEmail());
        assertNotNull(result);
        assertEquals(member1_active.getName(), result.getName());
    }

    @Test
    void updateSwimmingLevel_memberExists_updatesLevelAndCanSwim() {
        when(memberRepository.findById(member1_active.getId())).thenReturn(Optional.of(member1_active));
        // member1_active is INTERMEDIATE, canSwim=true
        assertNotEquals(SwimmingLevelEnum.NONE, member1_active.getSwimmingLevel());
        assertTrue(member1_active.isCanSwim());

        memberService.updateSwimmingLevel(member1_active.getId(), SwimmingLevelEnum.NONE.getDisplayName()); // Update to "Yüzme Bilmiyor"

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();
        assertEquals(SwimmingLevelEnum.NONE, savedMember.getSwimmingLevel());
        assertFalse(savedMember.isCanSwim()); // Should be false now
        assertNotNull(savedMember.getLastLessonDate()); // Should be updated
    }

    @Test
    void updateSwimmingLevel_fromNoneToBeginner_updatesCanSwim() {
        member2_pending.setSwimmingLevel(SwimmingLevelEnum.NONE);
        member2_pending.setCanSwim(false);
        when(memberRepository.findById(member2_pending.getId())).thenReturn(Optional.of(member2_pending));

        memberService.updateSwimmingLevel(member2_pending.getId(), SwimmingLevelEnum.BEGINNER.name()); // Using enum name

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();
        assertEquals(SwimmingLevelEnum.BEGINNER, savedMember.getSwimmingLevel());
        assertTrue(savedMember.isCanSwim());
    }


    @Test
    void deleteMember_memberExists_deletesMember() {
        when(memberRepository.existsById(member1_active.getId())).thenReturn(true);
        doNothing().when(memberRepository).deleteById(member1_active.getId());
        memberService.deleteMember(member1_active.getId());
        verify(memberRepository).deleteById(member1_active.getId());
    }

    @Test
    void getMemberDetails_memberExists_returnsDTOWithFilesAndAssessment() {
        when(memberRepository.findById(member1_active.getId())).thenReturn(Optional.of(member1_active));
        when(memberFileService.getBiometricPhotoPath(member1_active.getId())).thenReturn(Optional.of("s3/bio.jpg"));
        when(memberFileService.getIdPhotoFrontPath(member1_active.getId())).thenReturn(Optional.of("s3/id_front.jpg"));
        when(memberFileService.getIdPhotoBackPath(member1_active.getId())).thenReturn(Optional.of("s3/id_back.jpg"));
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(member1_active.getId())).thenReturn(Optional.of(assessment1));

        MemberDTO result = memberService.getMemberDetails(member1_active.getId());

        assertNotNull(result);
        assertEquals("s3/bio.jpg", result.getPhoto());
        assertEquals("s3/id_front.jpg", result.getIdPhotoFront());
        assertEquals("s3/id_back.jpg", result.getIdPhotoBack());
        assertEquals(assessment1.getMedicalReportPath(), result.getLatestMedicalReportPath());
        assertEquals(assessment1.isRequiresMedicalReport(), result.isRequiresMedicalReport());
    }

    @Test
    void listAllMembers_returnsListOfDTOs() {
        when(memberRepository.findAll()).thenReturn(List.of(member1_active, member2_pending));
        // Mock dependencies for convertToDTO for each member
        when(memberFileService.getBiometricPhotoPath(anyInt())).thenReturn(Optional.empty());
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(anyInt())).thenReturn(Optional.empty());


        List<MemberDTO> result = memberService.listAllMembers();
        assertEquals(2, result.size());
    }

    @Test
    void hasSwimmingAbility_canSwimAndLevelNotNone_returnsTrue() throws Exception {
        member1_active.setCanSwim(true);
        member1_active.setSwimmingLevel(SwimmingLevelEnum.BEGINNER);
        when(memberRepository.findById(member1_active.getId())).thenReturn(Optional.of(member1_active));
        assertTrue(memberService.hasSwimmingAbility(member1_active.getId()));
    }

    @Test
    void hasSwimmingAbility_cannotSwim_returnsFalse() throws Exception {
        member1_active.setCanSwim(false);
        member1_active.setSwimmingLevel(SwimmingLevelEnum.NONE);
        when(memberRepository.findById(member1_active.getId())).thenReturn(Optional.of(member1_active));
        assertFalse(memberService.hasSwimmingAbility(member1_active.getId()));
    }

    @Test
    void hasSwimmingAbility_levelIsNone_returnsFalse() throws Exception {
        member1_active.setCanSwim(true); // Contradictory, but level is NONE
        member1_active.setSwimmingLevel(SwimmingLevelEnum.NONE);
        when(memberRepository.findById(member1_active.getId())).thenReturn(Optional.of(member1_active));
        assertFalse(memberService.hasSwimmingAbility(member1_active.getId()));
    }


    @Test
    void updateMemberStatus_updatesStatusAndReturnsDTO() {
        when(memberRepository.findById(member2_pending.getId())).thenReturn(Optional.of(member2_pending));
        // Mocks for convertToDTO
        when(memberFileService.getBiometricPhotoPath(anyInt())).thenReturn(Optional.empty());
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(anyInt())).thenReturn(Optional.empty());


        MemberDTO result = memberService.updateMemberStatus(member2_pending.getId(), StatusEnum.ACTIVE.name());
        assertEquals(StatusEnum.ACTIVE.name(), result.getStatus());

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        assertEquals(StatusEnum.ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void reviewHealthForm_requiresReport_setsStatusPendingHealthReport() {
        when(memberRepository.findById(member2_pending.getId())).thenReturn(Optional.of(member2_pending));
        // Mocks for convertToDTO
        when(memberFileService.getBiometricPhotoPath(anyInt())).thenReturn(Optional.empty());
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(anyInt())).thenReturn(Optional.empty());


        MemberDTO result = memberService.reviewHealthForm(member2_pending.getId(), true); // requiresMedicalReport = true
        assertEquals(StatusEnum.PENDING_HEALTH_REPORT.name(), result.getStatus());

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        assertEquals(StatusEnum.PENDING_HEALTH_REPORT, captor.getValue().getStatus());
        assertNull(captor.getValue().getCoachId()); // Coach not assigned yet
    }

    @Test
    void reviewHealthForm_noReportNeeded_setsStatusActiveAndAssignsCoach() {
        int assignedCoachId = 202;
        member2_pending.setCoachId(null); // Ensure no coach initially
        when(memberRepository.findById(member2_pending.getId())).thenReturn(Optional.of(member2_pending));
        when(coachAssignmentService.findLeastLoadedCoachId()).thenReturn(assignedCoachId);
        // Mocks for convertToDTO
        when(memberFileService.getBiometricPhotoPath(anyInt())).thenReturn(Optional.empty());
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(anyInt())).thenReturn(Optional.empty());


        MemberDTO result = memberService.reviewHealthForm(member2_pending.getId(), false); // requiresMedicalReport = false
        assertEquals(StatusEnum.ACTIVE.name(), result.getStatus());
        assertEquals(assignedCoachId, result.getCoachId());


        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        assertEquals(StatusEnum.ACTIVE, captor.getValue().getStatus());
        assertEquals(assignedCoachId, captor.getValue().getCoachId());
    }

    @Test
    void reviewHealthForm_noReportNeeded_coachAlreadyAssigned_keepsCoach() {
        int existingCoachId = 303;
        member1_active.setCoachId(existingCoachId); // Coach already assigned
        when(memberRepository.findById(member1_active.getId())).thenReturn(Optional.of(member1_active));
        // Mocks for convertToDTO
        when(memberFileService.getBiometricPhotoPath(anyInt())).thenReturn(Optional.empty());
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(anyInt())).thenReturn(Optional.empty());


        MemberDTO result = memberService.reviewHealthForm(member1_active.getId(), false);
        assertEquals(StatusEnum.ACTIVE.name(), result.getStatus());
        assertEquals(existingCoachId, result.getCoachId()); // Coach should remain the same

        verify(coachAssignmentService, never()).findLeastLoadedCoachId(); // Should not try to assign new coach
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        assertEquals(existingCoachId, captor.getValue().getCoachId());
    }


    @Test
    void getMemberReportStatus_assessmentExists_returnsCorrectStatusDTO() {
        when(memberRepository.findById(member1_active.getId())).thenReturn(Optional.of(member1_active));
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(member1_active.getId())).thenReturn(Optional.of(assessment1));

        MemberReportStatusDTO result = memberService.getMemberReportStatus(member1_active.getId());
        assertEquals(member1_active.getStatus(), result.getCurrentMemberStatus());
        assertEquals(assessment1.getDoctorNotes(), result.getDoctorNotes());
        assertEquals(assessment1.isRequiresMedicalReport(), result.isRequiresMedicalReport());
        assertEquals(assessment1.getMedicalReportPath(), result.getMedicalReportPath());
        assertEquals(assessment1.getUpdatedAt(), result.getReportUpdatedAt());
        assertEquals(assessment1.isDoctorApproved(), result.isDoctorApproved());
    }

    @Test
    void getMemberReportStatus_noAssessment_returnsStatusDTOBasedOnMemberStatus() {
        member2_pending.setStatus(StatusEnum.PENDING_HEALTH_REPORT); // Explicitly set for test clarity
        when(memberRepository.findById(member2_pending.getId())).thenReturn(Optional.of(member2_pending));
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(member2_pending.getId())).thenReturn(Optional.empty());

        MemberReportStatusDTO result = memberService.getMemberReportStatus(member2_pending.getId());
        assertEquals(StatusEnum.PENDING_HEALTH_REPORT, result.getCurrentMemberStatus());
        assertNull(result.getDoctorNotes());
        assertTrue(result.isRequiresMedicalReport());
        assertNull(result.getMedicalReportPath());
        assertNull(result.getReportUpdatedAt());
        assertFalse(result.isDoctorApproved());
    }

    @Nested
    class ProcessDoctorMedicalReportReviewTests {
        private final int doctorId = 901;
        private final String doctorNotes = "Reviewed by doctor.";

        @BeforeEach
        void setupAssessment() {
            // Ensure member1_active has an assessment for these tests
            when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(member1_active.getId()))
                    .thenReturn(Optional.of(assessment1));
            when(memberRepository.findById(member1_active.getId())).thenReturn(Optional.of(member1_active));
            // Mocks for convertToDTO
            when(memberFileService.getBiometricPhotoPath(anyInt())).thenReturn(Optional.empty());
            // assessmentRepository will be called again inside convertToDTO, ensure it returns the same
            // This can be tricky if the assessment object is modified.
            // It's better if convertToDTO doesn't re-fetch if data is already available.
            // For simplicity, we'll assume convertToDTO's internal assessment fetch is fine or also mocked.
        }

        @Test
        void processDoctorMedicalReportReview_eligible_documentValid_setsActiveAndAssignsCoach() throws IOException {
            int assignedCoachId = 404;
            member1_active.setCoachId(null); // Ensure no coach initially
            assessment1.setMedicalReportPath("s3/valid_report.pdf"); // Has a report

            when(coachAssignmentService.findLeastLoadedCoachId()).thenReturn(assignedCoachId);
            doNothing().when(emailService).sendRegistrationApproval(member1_active.getEmail());

            MemberDTO result = memberService.processDoctorMedicalReportReview(
                    member1_active.getId(), doctorId, true, false, doctorNotes);

            assertEquals(StatusEnum.ACTIVE.name(), result.getStatus());
            assertEquals(assignedCoachId, result.getCoachId());

            ArgumentCaptor<MemberHealthAssessment> assessmentCaptor = ArgumentCaptor.forClass(MemberHealthAssessment.class);
            verify(assessmentRepository).save(assessmentCaptor.capture());
            assertTrue(assessmentCaptor.getValue().isDoctorApproved());
            assertFalse(assessmentCaptor.getValue().isRequiresMedicalReport());
            assertEquals(doctorId, assessmentCaptor.getValue().getDoctorId());
            assertEquals(doctorNotes, assessmentCaptor.getValue().getDoctorNotes());

            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());
            assertEquals(StatusEnum.ACTIVE, memberCaptor.getValue().getStatus());
            assertEquals(assignedCoachId, memberCaptor.getValue().getCoachId());

            verify(emailService).sendRegistrationApproval(member1_active.getEmail());
            verify(storageService, never()).deleteFile(anyString());
        }

        @Test
        void processDoctorMedicalReportReview_notEligible_documentValid_setsRejectedAndSendsEmail() throws IOException {
            assessment1.setMedicalReportPath("s3/valid_report.pdf");
            doNothing().when(emailService).sendRegistrationRejection(member1_active.getEmail(), doctorNotes);

            MemberDTO result = memberService.processDoctorMedicalReportReview(
                    member1_active.getId(), doctorId, false, false, doctorNotes);

            assertEquals(StatusEnum.REJECTED_HEALTH_REPORT.name(), result.getStatus());

            ArgumentCaptor<MemberHealthAssessment> assessmentCaptor = ArgumentCaptor.forClass(MemberHealthAssessment.class);
            verify(assessmentRepository).save(assessmentCaptor.capture());
            assertFalse(assessmentCaptor.getValue().isDoctorApproved());
            assertFalse(assessmentCaptor.getValue().isRequiresMedicalReport()); // Report was reviewed, no longer "required" in the pending sense

            verify(emailService).sendRegistrationRejection(member1_active.getEmail(), doctorNotes);
            verify(storageService, never()).deleteFile(anyString());
        }

        @Test
        void processDoctorMedicalReportReview_documentInvalid_deletesFileAndSetsPending() throws IOException {
            String invalidReportPath = "s3/invalid_report.pdf";
            assessment1.setMedicalReportPath(invalidReportPath);
            assessment1.setDoctorApproved(true); // Previous state

            doNothing().when(storageService).deleteFile(invalidReportPath);
            doNothing().when(emailService).sendInvalidDocumentNotification(member1_active.getEmail(), member1_active.getName(), doctorNotes);

            MemberDTO result = memberService.processDoctorMedicalReportReview(
                    member1_active.getId(), doctorId, false, true, doctorNotes); // isEligible=false, isDocumentInvalid=true

            assertEquals(StatusEnum.PENDING_HEALTH_REPORT.name(), result.getStatus());

            ArgumentCaptor<MemberHealthAssessment> assessmentCaptor = ArgumentCaptor.forClass(MemberHealthAssessment.class);
            verify(assessmentRepository).save(assessmentCaptor.capture());
            MemberHealthAssessment savedAssessment = assessmentCaptor.getValue();
            assertFalse(savedAssessment.isDoctorApproved());
            assertTrue(savedAssessment.isRequiresMedicalReport());
            assertNull(savedAssessment.getMedicalReportPath()); // Path should be cleared

            verify(storageService).deleteFile(invalidReportPath);
            verify(emailService).sendInvalidDocumentNotification(member1_active.getEmail(), member1_active.getName(), doctorNotes);
        }

        @Test
        void processDoctorMedicalReportReview_documentInvalid_noExistingFile_setsPending() throws IOException {
            assessment1.setMedicalReportPath(null); // No report path initially

            doNothing().when(emailService).sendInvalidDocumentNotification(member1_active.getEmail(), member1_active.getName(), doctorNotes);

            MemberDTO result = memberService.processDoctorMedicalReportReview(
                    member1_active.getId(), doctorId, false, true, doctorNotes);

            assertEquals(StatusEnum.PENDING_HEALTH_REPORT.name(), result.getStatus());
            verify(storageService, never()).deleteFile(anyString()); // No file to delete
        }

    }

    @Test
    void getHealthAssessmentReviewForMember_assessmentExists_returnsDTO() {
        // assessment1 has riskScore 5.0 and now has 2 answers from setup
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(member1_active.getId()))
                .thenReturn(Optional.of(assessment1));
        when(riskAssessmentService.determineRiskLevel(5.0)).thenReturn(RiskLevel.LOW);

        MemberHealthAssessmentDTO result = memberService.getHealthAssessmentReviewForMember(member1_active.getId());

        assertNotNull(result);
        assertEquals(5.0, result.getRiskScore());
        assertEquals(RiskLevel.LOW.name(), result.getRiskLevel());
        assertEquals(RiskLevel.LOW.getDescription(), result.getRiskLevelDescription());
        assertEquals(assessment1.isRequiresMedicalReport(), result.isRequiresMedicalReport()); // This was already in assessment1

        assertNotNull(result.getAnswers());
        assertEquals(2, result.getAnswers().size());

        HealthAnswerDTO answerDTO1 = result.getAnswers().stream()
                .filter(a -> a.getQuestionText().equals("Q1 Text"))
                .findFirst().orElse(null);
        assertNotNull(answerDTO1);
        assertTrue(answerDTO1.isAnswer());
        assertEquals("Notes for Q1", answerDTO1.getNotes());

        HealthAnswerDTO answerDTO2 = result.getAnswers().stream()
                .filter(a -> a.getQuestionText().equals("Q2 Text"))
                .findFirst().orElse(null);
        assertNotNull(answerDTO2);
        assertFalse(answerDTO2.isAnswer());
        assertNull(answerDTO2.getNotes());
    }

    @Test
    void getHealthAssessmentReviewForMember_noAssessment_throwsIllegalArgumentException() {
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(member1_active.getId()))
                .thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> memberService.getHealthAssessmentReviewForMember(member1_active.getId()));
    }
}