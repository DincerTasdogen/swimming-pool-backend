package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.HealthQuestionDTO;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.CustomOAuth2UserService;
import com.sp.SwimmingPool.service.CustomUserDetailsService;
import com.sp.SwimmingPool.service.HealthQuestionService;
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

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthQuestionController.class)
@Import(SecurityConfig.class)
public class HealthQuestionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HealthQuestionService healthQuestionService;

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
    private Authentication nonDoctorAuthentication;


    @BeforeEach
    void setUp() {
        UserPrincipal doctorPrincipal = UserPrincipal.builder().id(1).email("doctor@example.com").role("DOCTOR").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))).build();
        doctorAuthentication = new UsernamePasswordAuthenticationToken(doctorPrincipal, null, doctorPrincipal.getAuthorities());

        UserPrincipal memberPrincipal = UserPrincipal.builder().id(2).email("member@example.com").role("MEMBER").userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))).build();
        nonDoctorAuthentication = new UsernamePasswordAuthenticationToken(memberPrincipal, null, memberPrincipal.getAuthorities());
    }

    @Test
    void getHealthQuestionsForRegistration_shouldReturnQuestions() throws Exception {
        HealthQuestionDTO q1 = HealthQuestionDTO.builder().id(1L).questionText("Question 1?").build();
        HealthQuestionDTO q2 = HealthQuestionDTO.builder().id(2L).questionText("Question 2?").build();
        when(healthQuestionService.getAllActiveQuestions()).thenReturn(List.of(q1, q2));

        mockMvc.perform(get("/api/health-questions")
                        .contentType(MediaType.APPLICATION_JSON)) // No auth needed as it's permitAll
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].questionText", is("Question 1?")));
    }

    @Test
    void getHealthQuestionsForRegistration_shouldReturnNoContent_whenNoQuestions() throws Exception {
        when(healthQuestionService.getAllActiveQuestions()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/health-questions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void getHealthQuestionsForRegistration_shouldReturnInternalServerError_onServiceError() throws Exception {
        when(healthQuestionService.getAllActiveQuestions()).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/health-questions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Failed to retrieve health questions.")));
    }

    @Test
    void addHealthQuestion_asDoctor_shouldCreateQuestion() throws Exception {
        HealthQuestionDTO requestDTO = HealthQuestionDTO.builder().questionText("New Question?").build();
        HealthQuestionDTO responseDTO = HealthQuestionDTO.builder().id(1L).questionText("New Question?").build();

        when(healthQuestionService.addQuestion(any(HealthQuestionDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/health-questions")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.questionText", is("New Question?")));
    }

    @Test
    void addHealthQuestion_asNonDoctor_shouldBeForbidden() throws Exception {
        HealthQuestionDTO requestDTO = HealthQuestionDTO.builder().questionText("New Question?").build();

        mockMvc.perform(post("/api/health-questions")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(nonDoctorAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateHealthQuestion_asDoctor_shouldUpdateQuestion() throws Exception {
        Long questionId = 1L;
        HealthQuestionDTO requestDTO = HealthQuestionDTO.builder().id(questionId).questionText("Updated Question?").build();
        HealthQuestionDTO responseDTO = HealthQuestionDTO.builder().id(questionId).questionText("Updated Question?").build();

        when(healthQuestionService.updateQuestion(eq(questionId), any(HealthQuestionDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/api/health-questions/{id}", questionId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionText", is("Updated Question?")));
    }

    @Test
    void deleteHealthQuestion_asDoctor_shouldDeleteQuestion() throws Exception {
        Long questionId = 1L;
        // healthQuestionService.deleteQuestion is void

        mockMvc.perform(delete("/api/health-questions/{id}", questionId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuthentication))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(healthQuestionService).deleteQuestion(questionId);
    }
}