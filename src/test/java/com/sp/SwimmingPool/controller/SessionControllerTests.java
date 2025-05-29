package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.EducationStatusUpdateRequest;
import com.sp.SwimmingPool.model.entity.Session;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.CustomOAuth2UserService;
import com.sp.SwimmingPool.service.CustomUserDetailsService;
import com.sp.SwimmingPool.service.ScheduledSessionCreationService;
import com.sp.SwimmingPool.service.SessionService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
@Import(SecurityConfig.class)
public class SessionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockBean
    private ScheduledSessionCreationService scheduledSessionCreationService;
    @MockBean
    private SessionService sessionService;

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
    private Authentication memberAuth;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        UserPrincipal memberPrincipal = UserPrincipal.builder().id(1).email("member@example.com").role("MEMBER").userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).build();
        memberAuth = new UsernamePasswordAuthenticationToken(memberPrincipal, null, memberPrincipal.getAuthorities());

        UserPrincipal adminPrincipal = UserPrincipal.builder().id(1).email("admin@example.com").role("ADMIN").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).build();
        adminAuth = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());

        UserPrincipal coachPrincipal = UserPrincipal.builder().id(2).email("coach@example.com").role("COACH").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_COACH"))).build();
        coachAuth = new UsernamePasswordAuthenticationToken(coachPrincipal, null, coachPrincipal.getAuthorities());
    }

    private Session createSampleSession(int id, LocalDate date, LocalTime start, boolean isEducation) {
        Session session = new Session();
        session.setId(id);
        session.setPoolId(1);
        session.setSessionDate(date);
        session.setStartTime(start);
        session.setEndTime(start.plusHours(1));
        session.setCapacity(20);
        session.setCurrentBookings(5);
        session.setEducationSession(isEducation);
        return session;
    }

    @Test
    void generateSessions_asAdmin_shouldSucceed() throws Exception {
        doNothing().when(scheduledSessionCreationService).generateScheduledSessions();

        mockMvc.perform(post("/api/sessions/generate")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Otomatik seans oluşturma işlemi başarıyla tamamlandı.")));
        verify(scheduledSessionCreationService).generateScheduledSessions();
    }

    @Test
    void getSessionsByDate_shouldReturnSessions() throws Exception {
        LocalDate date = LocalDate.now();
        Session session1 = createSampleSession(1, date, LocalTime.of(10,0), false);
        when(sessionService.getSessionsByDate(date)).thenReturn(List.of(session1));

        mockMvc.perform(get("/api/sessions/date/{date}", date.toString()).with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)));
    }

    @Test
    void getSessionsByDate_shouldReturnNoContent_whenNoSessions() throws Exception {
        LocalDate date = LocalDate.now();
        when(sessionService.getSessionsByDate(date)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/sessions/date/{date}", date.toString()).with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth)))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateSessionEducationStatus_asCoach_shouldUpdateStatus() throws Exception {
        int sessionId = 1;
        EducationStatusUpdateRequest request = new EducationStatusUpdateRequest();
        request.setEducationSession(true);

        Session updatedSession = createSampleSession(sessionId, LocalDate.now(), LocalTime.NOON, true);
        when(sessionService.updateSessionEducationStatus(sessionId, true)).thenReturn(updatedSession);

        mockMvc.perform(patch("/api/sessions/{id}/education-status", sessionId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.educationSession", is(true)));
    }

    @Test
    void updateSessionEducationStatus_asAdmin_shouldUpdateStatus() throws Exception {
        int sessionId = 1;
        EducationStatusUpdateRequest request = new EducationStatusUpdateRequest();
        request.setEducationSession(false);

        Session updatedSession = createSampleSession(sessionId, LocalDate.now(), LocalTime.NOON, false);
        when(sessionService.updateSessionEducationStatus(sessionId, false)).thenReturn(updatedSession);

        mockMvc.perform(patch("/api/sessions/{id}/education-status", sessionId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.educationSession", is(false)));
    }


    @Test
    void ensureSessionAvailability_asAdmin_shouldSucceed() throws Exception {
        doNothing().when(scheduledSessionCreationService).ensureMinimumSessionAvailability();

        mockMvc.perform(post("/api/sessions/ensure-availability")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Seans uygunluğu başarıyla alındı.")));
        verify(scheduledSessionCreationService).ensureMinimumSessionAvailability();
    }

    @Test
    void getSessionsForPoolInRange_asCoach_shouldReturnSessions() throws Exception {
        int poolId = 1;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(7);
        Session session1 = createSampleSession(1, startDate, LocalTime.of(9,0), false);
        when(sessionService.getSessionsForPoolInRange(poolId, startDate, endDate)).thenReturn(List.of(session1));

        mockMvc.perform(get("/api/sessions/pool/{poolId}", poolId)
                        .param("start", startDate.toString())
                        .param("end", endDate.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)));
    }
}