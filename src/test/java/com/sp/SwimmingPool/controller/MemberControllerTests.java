package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.MemberDTO;
import com.sp.SwimmingPool.dto.MemberHealthAssessmentDTO;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.CustomOAuth2UserService;
import com.sp.SwimmingPool.service.CustomUserDetailsService;
import com.sp.SwimmingPool.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberController.class)
@Import(SecurityConfig.class)
public class MemberControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private Authentication adminAuth;
    private Authentication coachAuth;
    private Authentication doctorAuth;
    private Authentication memberAuth; // For unauthorized tests

    @BeforeEach
    void setUp() {
        UserPrincipal adminPrincipal = UserPrincipal.builder().id(1).email("admin@example.com").role("ADMIN").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).build();
        adminAuth = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());

        UserPrincipal coachPrincipal = UserPrincipal.builder().id(2).email("coach@example.com").role("COACH").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_COACH"))).build();
        coachAuth = new UsernamePasswordAuthenticationToken(coachPrincipal, null, coachPrincipal.getAuthorities());

        UserPrincipal doctorPrincipal = UserPrincipal.builder().id(3).email("doctor@example.com").role("DOCTOR").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))).build();
        doctorAuth = new UsernamePasswordAuthenticationToken(doctorPrincipal, null, doctorPrincipal.getAuthorities());

        UserPrincipal memberPrincipal = UserPrincipal.builder().id(4).email("member@example.com").role("MEMBER").userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))).build();
        memberAuth = new UsernamePasswordAuthenticationToken(memberPrincipal, null, memberPrincipal.getAuthorities());
    }

    private MemberDTO createSampleMemberDTO(int id, String name, String status) {
        MemberDTO dto = new MemberDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setSurname("User");
        dto.setEmail(name.toLowerCase().replace(" ", "") + "@example.com");
        dto.setStatus(status);
        dto.setRegistrationDate(LocalDateTime.now().minusDays(10));
        dto.setBirthDate(LocalDate.of(1990,1,1));
        return dto;
    }

    // --- GET /api/members ---
    @Test
    void getMemberList_asAdmin_shouldReturnMembers() throws Exception {
        MemberDTO member1 = createSampleMemberDTO(1, "John Doe", "ACTIVE");
        when(memberService.listAllMembers()).thenReturn(List.of(member1));
        mockMvc.perform(get("/api/members")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("John Doe")));
    }

    @Test
    void getMemberList_asAdmin_whenNoMembers_shouldReturnNoContent() throws Exception {
        when(memberService.listAllMembers()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/members")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isNoContent());
    }

    @Test
    void getMemberList_asCoach_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/members")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuth)))
                .andExpect(status().isForbidden());
    }

    // --- GET /api/members/{id} ---
    @Test
    void getMemberById_asAdmin_shouldReturnMember() throws Exception {
        int memberId = 1;
        MemberDTO member = createSampleMemberDTO(memberId, "Detail Member", "ACTIVE");
        when(memberService.getMemberDetails(memberId)).thenReturn(member);
        mockMvc.perform(get("/api/members/{id}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Detail Member")));
    }

    @Test
    void getMemberById_asCoach_shouldReturnMember() throws Exception {
        int memberId = 1;
        MemberDTO member = createSampleMemberDTO(memberId, "Coach View Member", "ACTIVE");
        when(memberService.getMemberDetails(memberId)).thenReturn(member);
        mockMvc.perform(get("/api/members/{id}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Coach View Member")));
    }

    @Test
    void getMemberById_asDoctor_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/members/{id}", 1)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuth)))
                .andExpect(status().isForbidden());
    }


    @Test
    void getMemberById_whenNotFound_shouldReturnNotFound() throws Exception {
        int memberId = 99;
        when(memberService.getMemberDetails(memberId)).thenThrow(new RuntimeException("Not found")); // Or specific exception
        mockMvc.perform(get("/api/members/{id}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isNotFound());
    }


    // --- GET /api/members/coach/{coachId} ---
    @Test
    void getMembersOfCoach_asCoach_shouldReturnMembers() throws Exception {
        int coachId = ((UserPrincipal)coachAuth.getPrincipal()).getId();
        MemberDTO member = createSampleMemberDTO(5, "Coached Member", "ACTIVE");
        when(memberService.listMembersOfCoach(coachId)).thenReturn(List.of(member));
        mockMvc.perform(get("/api/members/coach/{coachId}", coachId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getMembersOfCoach_asAdmin_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/members/coach/{coachId}", 1)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isForbidden());
    }

    // --- PUT /api/members/{memberId} ---
    @Test
    void editMember_asAdmin_shouldUpdateAndReturnMember() throws Exception {
        int memberId = 1;
        MemberDTO requestDTO = createSampleMemberDTO(memberId, "Updated John", "ACTIVE");
        requestDTO.setPhoneNumber("1234567890");
        when(memberService.updateMember(eq(memberId), any(MemberDTO.class))).thenReturn(requestDTO);

        mockMvc.perform(put("/api/members/{memberId}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated John")))
                .andExpect(jsonPath("$.phoneNumber", is("1234567890")));
    }

    @Test
    void editMember_asAdmin_whenServiceThrowsException_shouldReturnInternalServerError() throws Exception {
        int memberId = 1;
        MemberDTO requestDTO = createSampleMemberDTO(memberId, "Update Fail", "ACTIVE");
        when(memberService.updateMember(eq(memberId), any(MemberDTO.class))).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(put("/api/members/{memberId}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isInternalServerError());
    }


    // --- PUT /api/members/{memberId}/swim-status ---
    @Test
    void editMemberSwimStatus_asCoach_shouldUpdate() throws Exception {
        int memberId = 1;
        MemberDTO requestDTO = new MemberDTO();
        requestDTO.setCanSwim(true);

        MemberDTO initialMember = createSampleMemberDTO(memberId, "Swimmer", "ACTIVE");
        initialMember.setCanSwim(false);
        MemberDTO updatedMember = createSampleMemberDTO(memberId, "Swimmer", "ACTIVE");
        updatedMember.setCanSwim(true);

        when(memberService.getMemberDetails(memberId)).thenReturn(initialMember);
        when(memberService.updateMember(eq(memberId), any(MemberDTO.class))).thenReturn(updatedMember);

        mockMvc.perform(put("/api/members/{memberId}/swim-status", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canSwim", is(true)));
        verify(memberService).updateMember(eq(memberId), argThat(m -> m.isCanSwim() && m.getName().equals("Swimmer")));
    }

    // --- PUT /api/members/{memberId}/swimming-level ---
    @Test
    void updateSwimmingLevel_asCoach_shouldUpdate() throws Exception {
        int memberId = 1;
        Map<String, String> payload = Map.of("level", "ADVANCED");
        MemberDTO updatedMember = createSampleMemberDTO(memberId, "Pro Swimmer", "ACTIVE");
        updatedMember.setSwimmingLevel("ADVANCED");
        when(memberService.updateSwimmingLevel(memberId, "ADVANCED")).thenReturn(updatedMember);

        mockMvc.perform(put("/api/members/{memberId}/swimming-level", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.swimmingLevel", is("ADVANCED")));
    }

    @Test
    void updateSwimmingLevel_asCoach_withNullLevel_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/members/{memberId}/swimming-level", 1)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("level", "")))) // Empty level
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSwimmingLevel_asCoach_whenServiceThrowsIllegalArgument_shouldReturnBadRequest() throws Exception {
        int memberId = 1;
        Map<String, String> payload = Map.of("level", "INVALID_LEVEL");
        when(memberService.updateSwimmingLevel(memberId, "INVALID_LEVEL")).thenThrow(new IllegalArgumentException("Bad level"));

        mockMvc.perform(put("/api/members/{memberId}/swimming-level", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }


    // --- PUT /api/members/{memberId}/swimming-notes ---
    @Test
    void updateSwimmingNotes_asCoach_shouldUpdate() throws Exception {
        int memberId = 1;
        Map<String, String> payload = Map.of("notes", "Improved technique.");
        MemberDTO initialMember = createSampleMemberDTO(memberId, "Student", "ACTIVE");
        MemberDTO updatedMember = createSampleMemberDTO(memberId, "Student", "ACTIVE");
        updatedMember.setSwimmingNotes("Improved technique.");

        when(memberService.getMemberDetails(memberId)).thenReturn(initialMember);
        when(memberService.updateMember(eq(memberId), any(MemberDTO.class))).thenReturn(updatedMember);

        mockMvc.perform(put("/api/members/{memberId}/swimming-notes", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.swimmingNotes", is("Improved technique.")));
        verify(memberService).updateMember(eq(memberId), argThat(m -> "Improved technique.".equals(m.getSwimmingNotes())));
    }

    @Test
    void updateSwimmingNotes_asCoach_withNullNotes_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/members/{memberId}/swimming-notes", 1)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonMap("notes", null))))
                .andExpect(status().isBadRequest());
    }


    // --- DELETE /api/members/{id} ---
    @Test
    void deleteMember_asAdmin_shouldDelete() throws Exception {
        int memberId = 1;
        doNothing().when(memberService).deleteMember(memberId);
        mockMvc.perform(delete("/api/members/{id}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf()))
                .andExpect(status().isNoContent());
        verify(memberService).deleteMember(memberId);
    }

    @Test
    void deleteMember_asAdmin_whenNotFound_shouldReturnNotFound() throws Exception {
        int memberId = 99;
        doThrow(new IllegalArgumentException("Not found")).when(memberService).deleteMember(memberId);
        mockMvc.perform(delete("/api/members/{id}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/members/status/{status} ---
    @ParameterizedTest
    @EnumSource(StatusEnum.class)
    void getMembersByStatus_asDoctor_shouldReturnMembers(StatusEnum status) throws Exception {
        MemberDTO member = createSampleMemberDTO(1, "Status Member", status.name());
        when(memberService.getMembersByStatus(status)).thenReturn(List.of(member));
        mockMvc.perform(get("/api/members/status/{status}", status.name())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is(status.name())));
    }

    // --- GET /api/members/status/{status}/count ---
    @ParameterizedTest
    @EnumSource(StatusEnum.class)
    void getCountByStatus_asDoctor_shouldReturnCount(StatusEnum status) throws Exception {
        when(memberService.countMembersByStatus(status)).thenReturn(5);
        mockMvc.perform(get("/api/members/status/{status}/count", status.name())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuth)))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void getMemberByEmail_shouldReturnMemberId() throws Exception {
        String email = "findme@example.com";
        MemberDTO member = new MemberDTO();
        member.setId(123);
        when(memberService.findByEmail(email)).thenReturn(member);
        mockMvc.perform(get("/api/members/by-email").param("email", email).with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(123)));
    }

    // --- GET /api/members/{memberId}/health-assessment ---
    @Test
    void getHealthAssessmentReview_asDoctor_shouldReturnAssessment() throws Exception {
        int memberId = 1;
        MemberHealthAssessmentDTO dto = new MemberHealthAssessmentDTO();
        dto.setRiskLevel("LOW");
        when(memberService.getHealthAssessmentReviewForMember(memberId)).thenReturn(dto);
        mockMvc.perform(get("/api/members/{memberId}/health-assessment", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel", is("LOW")));
    }

    @Test
    void getHealthAssessmentReview_asDoctor_whenNotFound_shouldReturnNotFound() throws Exception {
        int memberId = 99;
        when(memberService.getHealthAssessmentReviewForMember(memberId)).thenThrow(new IllegalArgumentException("Not found"));
        mockMvc.perform(get("/api/members/{memberId}/health-assessment", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuth)))
                .andExpect(status().isNotFound());
    }

    // --- PUT /api/members/{memberId}/doctor/review-health-form ---
    @Test
    void reviewHealthForm_asDoctor_shouldUpdateMember() throws Exception {
        int memberId = 1;
        boolean requiresReport = true;
        MemberDTO updatedMember = createSampleMemberDTO(memberId, "FormReviewed", StatusEnum.PENDING_HEALTH_REPORT.name());
        updatedMember.setRequiresMedicalReport(requiresReport);
        when(memberService.reviewHealthForm(memberId, requiresReport)).thenReturn(updatedMember);

        mockMvc.perform(put("/api/members/{memberId}/doctor/review-health-form", memberId)
                        .param("requiresMedicalReport", String.valueOf(requiresReport))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuth))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresMedicalReport", is(true)));
    }

    // --- PUT /api/members/{memberId}/status ---
    @Test
    void updateMemberStatus_asAdmin_shouldUpdate() throws Exception {
        int memberId = 1;
        String newStatusStr = StatusEnum.DISABLED.name();
        Map<String, String> payload = Map.of("status", newStatusStr);
        MemberDTO updatedMember = createSampleMemberDTO(memberId, "StatusChanged", newStatusStr);
        when(memberService.updateMemberStatus(memberId, newStatusStr)).thenReturn(updatedMember);

        mockMvc.perform(put("/api/members/{memberId}/status", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(newStatusStr)));
    }

    @Test
    void updateMemberStatus_asAdmin_withNullStatus_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/members/{memberId}/status", 1)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonMap("status", null))))
                .andExpect(status().isBadRequest());
    }


    // --- GET /api/members/pending, /active, /rejected, /disabled ---
    @Test
    void getPendingApplications_asAdmin_shouldReturnPending() throws Exception {
        MemberDTO pendingMember = createSampleMemberDTO(1, "Pending App", StatusEnum.PENDING_DOCTOR_APPROVAL.name());
        List<StatusEnum> expectedStatuses = List.of(
                StatusEnum.PENDING_ID_CARD_VERIFICATION, StatusEnum.PENDING_PHOTO_VERIFICATION,
                StatusEnum.PENDING_HEALTH_REPORT, StatusEnum.PENDING_HEALTH_FORM_APPROVAL,
                StatusEnum.PENDING_DOCTOR_APPROVAL);
        when(memberService.getMembersByStatuses(expectedStatuses)).thenReturn(List.of(pendingMember));

        mockMvc.perform(get("/api/members/pending")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getActiveMembers_asAdmin_shouldReturnActive() throws Exception {
        MemberDTO activeMember = createSampleMemberDTO(1, "Active Member", StatusEnum.ACTIVE.name());
        when(memberService.getMembersByStatus(StatusEnum.ACTIVE)).thenReturn(List.of(activeMember));
        mockMvc.perform(get("/api/members/active")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getRejectedMembers_asAdmin_shouldReturnRejected() throws Exception {
        MemberDTO rejectedMember = createSampleMemberDTO(1, "Rejected Member", StatusEnum.REJECTED_HEALTH_REPORT.name());
        List<StatusEnum> expectedStatuses = List.of(
                StatusEnum.REJECTED_ID_CARD, StatusEnum.REJECTED_PHOTO, StatusEnum.REJECTED_HEALTH_REPORT);
        when(memberService.getMembersByStatuses(expectedStatuses)).thenReturn(List.of(rejectedMember));
        mockMvc.perform(get("/api/members/rejected")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getDisabledMembers_asAdmin_shouldReturnDisabled() throws Exception {
        MemberDTO disabledMember = createSampleMemberDTO(1, "Disabled Member", StatusEnum.DISABLED.name());
        when(memberService.getMembersByStatus(StatusEnum.DISABLED)).thenReturn(List.of(disabledMember));
        mockMvc.perform(get("/api/members/disabled")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}