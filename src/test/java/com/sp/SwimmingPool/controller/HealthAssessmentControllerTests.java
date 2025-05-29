package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.AnswerRequest;
import com.sp.SwimmingPool.dto.HealthAssessmentRequest;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.CustomOAuth2UserService;
import com.sp.SwimmingPool.service.CustomUserDetailsService;
import com.sp.SwimmingPool.service.VerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthAssessmentController.class)
@Import(SecurityConfig.class)
public class HealthAssessmentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VerificationService verificationService;

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

    @Test
    void saveHealthAssessment_shouldUpdateTempData_whenSuccessful() throws Exception {
        HealthAssessmentRequest request = new HealthAssessmentRequest();
        request.setEmail("test@example.com");
        AnswerRequest answer = new AnswerRequest();
        answer.setQuestionId(1L);
        answer.setAnswer(true);
        answer.setNotes("Some notes");
        request.setAnswers(List.of(answer));

        Map<String, Object> expectedDataToUpdate = Map.of("healthAnswers", request.getAnswers());

        when(verificationService.updateTempUserData(eq("test@example.com"), eq(expectedDataToUpdate)))
                .thenReturn(true);

        mockMvc.perform(post("/api/registration/health-assessment")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(verificationService).updateTempUserData(eq("test@example.com"), eq(expectedDataToUpdate));
    }

    @Test
    void saveHealthAssessment_shouldReturnBadRequest_whenNoRegistrationInProgress() throws Exception {
        HealthAssessmentRequest request = new HealthAssessmentRequest();
        request.setEmail("unknown@example.com");
        AnswerRequest answer = new AnswerRequest();
        answer.setQuestionId(1L);
        answer.setAnswer(false);
        request.setAnswers(List.of(answer));

        Map<String, Object> expectedDataToUpdate = Map.of("healthAnswers", request.getAnswers());

        when(verificationService.updateTempUserData(eq("unknown@example.com"), eq(expectedDataToUpdate)))
                .thenReturn(false); // Simulate failure to update (e.g., no temp data for email)

        mockMvc.perform(post("/api/registration/health-assessment")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No registration in progress for this email"));
    }

    @Test
    void saveHealthAssessment_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        HealthAssessmentRequest invalidRequest = new HealthAssessmentRequest();

        mockMvc.perform(post("/api/registration/health-assessment")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()); // Due to MethodArgumentNotValidException
    }
}