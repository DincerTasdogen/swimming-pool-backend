// src/test/java/com/sp/SwimmingPool/controller/DoctorReviewControllerTests.java
package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
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
import com.sp.SwimmingPool.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorReviewController.class)
@Import(SecurityConfig.class)
public class DoctorReviewControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MemberRepository memberRepository;
    @MockBean
    private MemberHealthAssessmentRepository assessmentRepository;
    @MockBean
    private EmailService emailService;

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

    private Authentication doctorAuthentication;
    private Authentication nonDoctorAuthentication; // For unauthorized tests

    @BeforeEach
    void setUp() {
        UserPrincipal doctorPrincipal = UserPrincipal.builder()
                .id(100) // Doctor's user ID
                .email("doctor@example.com")
                .name("Dr. Who")
                .role("DOCTOR")
                .userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                .build();
        doctorAuthentication = new UsernamePasswordAuthenticationToken(
                doctorPrincipal, null, doctorPrincipal.getAuthorities()
        );

        UserPrincipal memberPrincipal = UserPrincipal.builder().id(1).email("member@example.com").role("MEMBER").userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))).build();
        nonDoctorAuthentication = new UsernamePasswordAuthenticationToken(memberPrincipal, null, memberPrincipal.getAuthorities());
    }

    private Member createMember(int id, String email, StatusEnum status) {
        Member member = new Member();
        member.setId(id);
        member.setEmail(email);
        member.setStatus(status);
        return member;
    }

    private MemberHealthAssessment createAssessment(int memberId) {
        MemberHealthAssessment assessment = new MemberHealthAssessment();
        assessment.setMemberId(memberId);
        assessment.setCreatedAt(LocalDateTime.now().minusDays(1));
        return assessment;
    }

    @Test
    void reviewMedicalReport_asDoctor_approveMember_shouldSucceed() throws Exception {
        int memberId = 1;
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("approved", true);
        requestBody.put("doctorNotes", "Looks good for swimming.");

        Member member = createMember(memberId, "member1@example.com", StatusEnum.PENDING_DOCTOR_APPROVAL);
        MemberHealthAssessment assessment = createAssessment(memberId);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId))
                .thenReturn(Optional.of(assessment));
        // Mock save calls to return the saved entity
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assessmentRepository.save(any(MemberHealthAssessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/doctor/review-medical-report/{memberId}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(content().string("Değerlendirme kaydedildi."));

        verify(memberRepository).save(argThat(savedMember ->
                savedMember.getStatus() == StatusEnum.ACTIVE
        ));
        verify(assessmentRepository).save(argThat(savedAssessment ->
                savedAssessment.isDoctorApproved() && "Looks good for swimming.".equals(savedAssessment.getDoctorNotes())
        ));
        verify(emailService).sendRegistrationApproval("member1@example.com");
        verify(emailService, never()).sendRegistrationRejection(anyString(), anyString());
    }

    @Test
    void reviewMedicalReport_asDoctor_rejectMember_shouldSucceed() throws Exception {
        int memberId = 2;
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("approved", false);
        requestBody.put("doctorNotes", "Further medical checks required.");

        Member member = createMember(memberId, "member2@example.com", StatusEnum.PENDING_DOCTOR_APPROVAL);
        MemberHealthAssessment assessment = createAssessment(memberId);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId))
                .thenReturn(Optional.of(assessment));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assessmentRepository.save(any(MemberHealthAssessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/doctor/review-medical-report/{memberId}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(content().string("Değerlendirme kaydedildi."));

        verify(memberRepository).save(argThat(savedMember ->
                savedMember.getStatus() == StatusEnum.REJECTED_HEALTH_REPORT
        ));
        verify(assessmentRepository).save(argThat(savedAssessment ->
                !savedAssessment.isDoctorApproved() && "Further medical checks required.".equals(savedAssessment.getDoctorNotes())
        ));
        verify(emailService).sendRegistrationRejection("member2@example.com", "Further medical checks required.");
        verify(emailService, never()).sendRegistrationApproval(anyString());
    }

    @Test
    void reviewMedicalReport_asDoctor_whenMemberNotFound_shouldThrowRuntimeException() throws Exception {
        int memberId = 99; // Non-existent
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("approved", true);

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/doctor/review-medical-report/{memberId}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isInternalServerError()); // Or a more specific error if GlobalExceptionHandler handles RuntimeException differently
        // Assuming it results in 500 for unhandled RuntimeException
    }

    @Test
    void reviewMedicalReport_asDoctor_whenAssessmentNotFound_shouldThrowRuntimeException() throws Exception {
        int memberId = 1;
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("approved", true);

        Member member = createMember(memberId, "member1@example.com", StatusEnum.PENDING_DOCTOR_APPROVAL);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId))
                .thenReturn(Optional.empty()); // Assessment not found

        mockMvc.perform(post("/api/doctor/review-medical-report/{memberId}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void reviewMedicalReport_asNonDoctor_shouldBeForbidden() throws Exception {
        int memberId = 1;
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("approved", true);

        mockMvc.perform(post("/api/doctor/review-medical-report/{memberId}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(nonDoctorAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reviewMedicalReport_asDoctor_withNullApproved_shouldHandleGracefully() throws Exception {
        // Test how the controller handles null for 'approved' if it's possible from request
        // Boolean.TRUE.equals(null) is false, so it should act as rejection.
        int memberId = 3;
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("approved", null); // Sending null
        requestBody.put("doctorNotes", "Approval status unclear.");

        Member member = createMember(memberId, "member3@example.com", StatusEnum.PENDING_DOCTOR_APPROVAL);
        MemberHealthAssessment assessment = createAssessment(memberId);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(assessmentRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId))
                .thenReturn(Optional.of(assessment));

        mockMvc.perform(post("/api/doctor/review-medical-report/{memberId}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());

        verify(memberRepository).save(argThat(savedMember ->
                savedMember.getStatus() == StatusEnum.REJECTED_HEALTH_REPORT // Because approved is effectively false
        ));
        verify(assessmentRepository).save(argThat(savedAssessment ->
                !savedAssessment.isDoctorApproved()
        ));
        verify(emailService).sendRegistrationRejection(eq("member3@example.com"), anyString());
    }
}