package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.DoctorReviewRequest;
import com.sp.SwimmingPool.dto.MemberDTO;
import com.sp.SwimmingPool.dto.MemberReportStatusDTO;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.MemberHealthAssessment;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.repos.MemberHealthAssessmentRepository;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.CustomOAuth2UserService;
import com.sp.SwimmingPool.service.CustomUserDetailsService;
import com.sp.SwimmingPool.service.MemberService;
import com.sp.SwimmingPool.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MedicalReportController.class)
@Import(SecurityConfig.class)
public class MedicalReportControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MemberRepository memberRepository;
    @MockBean
    private MemberHealthAssessmentRepository assessmentRepository;
    @MockBean
    private StorageService storageService;
    @MockBean
    private MemberService memberService;

    // Mocks for SecurityConfig dependencies
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;
    @MockBean
    private OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    private Authentication member1Auth; // For memberId = 1
    private Authentication member2Auth; // For memberId = 2
    private Authentication doctorAuth;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        UserPrincipal member1Principal = UserPrincipal.builder().id(1).email("member1@example.com").role("MEMBER").userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))).build();
        member1Auth = new UsernamePasswordAuthenticationToken(member1Principal, null, member1Principal.getAuthorities());

        UserPrincipal member2Principal = UserPrincipal.builder().id(2).email("member2@example.com").role("MEMBER").userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))).build();
        member2Auth = new UsernamePasswordAuthenticationToken(member2Principal, null, member2Principal.getAuthorities());

        UserPrincipal doctorPrincipal = UserPrincipal.builder().id(100).email("doctor@example.com").role("DOCTOR").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))).build();
        doctorAuth = new UsernamePasswordAuthenticationToken(doctorPrincipal, null, doctorPrincipal.getAuthorities());

        UserPrincipal adminPrincipal = UserPrincipal.builder().id(200).email("admin@example.com").role("ADMIN").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).build();
        adminAuth = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
    }

    @Test
    void uploadMedicalReport_asOwnerMember_shouldSucceed() throws Exception {
        int memberId = 1;
        MockMultipartFile reportFile = new MockMultipartFile("file", "report.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf_content".getBytes());

        Member memberToSave = new Member();
        memberToSave.setId(memberId);
        memberToSave.setStatus(StatusEnum.PENDING_HEALTH_REPORT);

        MemberHealthAssessment assessmentToSave = new MemberHealthAssessment();
        assessmentToSave.setMemberId(memberId);
        assessmentToSave.setRequiresMedicalReport(true);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(memberToSave));
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId)).thenReturn(Optional.of(assessmentToSave));
        when(storageService.storeFile(any(MockMultipartFile.class), anyString())).thenReturn("path/to/report.pdf");

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/members/{memberId}/medical-report", memberId)
                        .file(reportFile)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(member1Auth))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Medical report uploaded successfully. It is now pending doctor review."));

        ArgumentCaptor<Member> memberArgumentCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberArgumentCaptor.capture());
        Member savedMember = memberArgumentCaptor.getValue();
        assertEquals(StatusEnum.PENDING_DOCTOR_APPROVAL, savedMember.getStatus());
        assertEquals(memberId, savedMember.getId());

        // Similarly for assessment
        ArgumentCaptor<MemberHealthAssessment> assessmentArgumentCaptor = ArgumentCaptor.forClass(MemberHealthAssessment.class);
        verify(assessmentRepository).save(assessmentArgumentCaptor.capture());
        MemberHealthAssessment savedAssessment = assessmentArgumentCaptor.getValue();
        assertEquals("path/to/report.pdf", savedAssessment.getMedicalReportPath());
        assertFalse(savedAssessment.isRequiresMedicalReport());
    }

    @Test
    void uploadMedicalReport_asDifferentMember_shouldBeForbidden() throws Exception {
        int memberIdToAccess = 1; // Trying to upload for member 1
        MockMultipartFile reportFile = new MockMultipartFile("file", "report.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf_content".getBytes());

        // Authenticated as member 2
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/members/{memberId}/medical-report", memberIdToAccess)
                        .file(reportFile)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(member2Auth))
                        .with(csrf()))
                .andExpect(status().isForbidden()); // Due to #memberId == principal.id
    }

    @Test
    void uploadMedicalReport_asOwnerMember_butStatusNotPendingHealthReport_shouldBeBadRequest() throws Exception {
        int memberId = 1;
        MockMultipartFile reportFile = new MockMultipartFile("file", "report.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf_content".getBytes());

        Member member = new Member();
        member.setId(memberId);
        member.setStatus(StatusEnum.ACTIVE); // Not PENDING_HEALTH_REPORT

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        // No need to mock assessmentRepository or storageService as it should fail before

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/members/{memberId}/medical-report", memberId)
                        .file(reportFile)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(member1Auth))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cannot upload medical report at this time. Current status: ACTIVE"));
    }

    @Test
    void downloadMedicalReport_asDoctor_shouldSucceed() throws Exception {
        int memberId = 1;
        String reportPath = "members/1/medical-report/report.pdf";
        byte[] pdfContent = "sample_pdf_content".getBytes();
        Resource resource = new ByteArrayResource(pdfContent);

        MemberHealthAssessment assessment = new MemberHealthAssessment();
        assessment.setMedicalReportPath(reportPath);

        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId)).thenReturn(Optional.of(assessment));
        when(storageService.loadFileAsResource(reportPath)).thenReturn(resource);

        MvcResult result = mockMvc.perform(get("/api/members/{memberId}/medical-report", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuth)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"medical-report-" + memberId + ".pdf\""))
                .andReturn();

        assertArrayEquals(pdfContent, result.getResponse().getContentAsByteArray());
    }

    @Test
    void downloadMedicalReport_asAdmin_shouldSucceed() throws Exception {
        int memberId = 1;
        String reportPath = "members/1/medical-report/report.pdf";
        Resource resource = new ByteArrayResource("pdf".getBytes());
        MemberHealthAssessment assessment = new MemberHealthAssessment();
        assessment.setMedicalReportPath(reportPath);

        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId)).thenReturn(Optional.of(assessment));
        when(storageService.loadFileAsResource(reportPath)).thenReturn(resource);

        mockMvc.perform(get("/api/members/{memberId}/medical-report", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk());
    }


    @Test
    void downloadMedicalReport_asMember_shouldBeForbidden() throws Exception {
        int memberId = 1;
        // Member is not allowed by @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
        mockMvc.perform(get("/api/members/{memberId}/medical-report", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(member1Auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    void doctorReviewMedicalReport_asDoctor_shouldSucceed() throws Exception {
        int memberId = 1;
        int doctorId = 100; // From doctorAuth principal
        DoctorReviewRequest reviewRequest = new DoctorReviewRequest();
        reviewRequest.setEligibleForPool(true);
        reviewRequest.setDocumentInvalid(false);
        reviewRequest.setDoctorNotes("All good.");

        MemberDTO updatedMemberDTO = new MemberDTO();
        updatedMemberDTO.setId(memberId);
        updatedMemberDTO.setStatus(StatusEnum.ACTIVE.name()); // Assuming this is what service returns

        when(memberService.processDoctorMedicalReportReview(
                eq(memberId), eq(doctorId), eq(true), eq(false), eq("All good.")
        )).thenReturn(updatedMemberDTO);

        mockMvc.perform(put("/api/members/{memberId}/doctor/review-report", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(memberId)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void doctorReviewMedicalReport_asAdmin_shouldSucceed() throws Exception {
        int memberId = 1;
        int adminUserId = 200; // From adminAuth principal
        DoctorReviewRequest reviewRequest = new DoctorReviewRequest();
        reviewRequest.setEligibleForPool(false);
        reviewRequest.setDocumentInvalid(true);
        reviewRequest.setDoctorNotes("Invalid ID document.");

        MemberDTO updatedMemberDTO = new MemberDTO();
        updatedMemberDTO.setId(memberId);
        updatedMemberDTO.setStatus(StatusEnum.REJECTED_ID_CARD.name());

        when(memberService.processDoctorMedicalReportReview(
                eq(memberId), eq(adminUserId), eq(false), eq(true), eq("Invalid ID document.")
        )).thenReturn(updatedMemberDTO);

        mockMvc.perform(put("/api/members/{memberId}/doctor/review-report", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(StatusEnum.REJECTED_ID_CARD.name())));
    }

    @Test
    void doctorReviewMedicalReport_asMember_shouldBeForbidden() throws Exception {
        int memberId = 1;
        DoctorReviewRequest reviewRequest = new DoctorReviewRequest();
        reviewRequest.setEligibleForPool(true);
        reviewRequest.setDocumentInvalid(false);

        mockMvc.perform(put("/api/members/{memberId}/doctor/review-report", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(member1Auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMemberReportStatusInfo_asOwnerMember_shouldSucceed() throws Exception {
        int memberId = 1;
        MemberReportStatusDTO statusDTO = new MemberReportStatusDTO(
                StatusEnum.PENDING_DOCTOR_APPROVAL, "Awaiting review", false, "path/to/report.pdf", LocalDateTime.now(), false
        );
        when(memberService.getMemberReportStatus(memberId)).thenReturn(statusDTO);

        mockMvc.perform(get("/api/members/{memberId}/report-status", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(member1Auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentMemberStatus", is("PENDING_DOCTOR_APPROVAL")))
                .andExpect(jsonPath("$.medicalReportPath", is("path/to/report.pdf")));
    }

    @Test
    void getMemberReportStatusInfo_asDoctor_shouldSucceed() throws Exception {
        int memberId = 2;
        MemberReportStatusDTO statusDTO = new MemberReportStatusDTO(
                StatusEnum.ACTIVE, null, false, null, null, true
        );
        when(memberService.getMemberReportStatus(memberId)).thenReturn(statusDTO);

        mockMvc.perform(get("/api/members/{memberId}/report-status", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentMemberStatus", is("ACTIVE")));
    }

    @Test
    void getMemberReportStatusInfo_asDifferentMember_shouldBeForbidden() throws Exception {
        int memberIdToAccess = 1; // Trying to access status for member 1
        // Authenticated as member 2
        mockMvc.perform(get("/api/members/{memberId}/report-status", memberIdToAccess)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(member2Auth)))
                .andExpect(status().isForbidden()); // Due to #memberId == principal.id
    }

    @Test
    void getMemberReportStatusInfo_asAdmin_shouldBeForbidden() throws Exception {
        int memberId = 1;
        // Admin is not explicitly allowed by @PreAuthorize("(hasRole('MEMBER') and #memberId == principal.id) or hasAnyRole('DOCTOR')")
        mockMvc.perform(get("/api/members/{memberId}/report-status", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isForbidden());
    }
}