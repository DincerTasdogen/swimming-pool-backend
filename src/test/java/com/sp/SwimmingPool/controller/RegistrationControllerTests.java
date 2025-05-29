package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.InitialRegisterRequest;
import com.sp.SwimmingPool.dto.RegisterRequest;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.CustomOAuth2UserService;
import com.sp.SwimmingPool.service.CustomUserDetailsService;
import com.sp.SwimmingPool.service.EmailService;
import com.sp.SwimmingPool.service.RegistrationService;
import com.sp.SwimmingPool.service.VerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistrationController.class)
@Import(SecurityConfig.class)
public class RegistrationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;
    @MockBean
    private VerificationService verificationService;
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

    @Test
    void initiateRegistration_shouldSucceed_whenDataIsValidAndNew() throws Exception {
        InitialRegisterRequest request = new InitialRegisterRequest();
        request.setName("New");
        request.setSurname("User");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setPhoneNumber("+905001112233");
        request.setIdentityNumber("11223344556");

        when(registrationService.isEmailRegistered("newuser@example.com")).thenReturn(false);
        when(registrationService.isIdentityNumberRegistered("11223344556")).thenReturn(false);
        when(verificationService.generateAndStoreCode(eq("newuser@example.com"), anyMap())).thenReturn("654321");
        doNothing().when(emailService).sendVerificationEmail("newuser@example.com", "654321");

        mockMvc.perform(post("/api/auth/register/init")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(emailService).sendVerificationEmail("newuser@example.com", "654321");
    }

    @Test
    void initiateRegistration_shouldFail_whenEmailExists() throws Exception {
        InitialRegisterRequest request = new InitialRegisterRequest();
        request.setName("New");
        request.setSurname("User");
        request.setPhoneNumber("+905001112233");
        request.setIdentityNumber("11223344556");
        request.setEmail("existing@example.com");
        request.setPassword("dummyPassword");
        when(registrationService.isEmailRegistered("existing@example.com")).thenReturn(true);

        mockMvc.perform(post("/api/auth/register/init")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Bu e-posta adresi zaten kayıtlıdır"));
    }

    @Test
    void updateRegistrationData_shouldSucceed_whenRegistrationInProgress() throws Exception {
        String email = "test@example.com";
        Map<String, Object> updateData = Map.of("birthDate", "1995-05-15");
        Map<String, Object> existingUserData = new HashMap<>(); // Simulate existing data
        existingUserData.put("name", "Test");

        when(verificationService.getTempUserData(email)).thenReturn(existingUserData);
        when(verificationService.updateTempUserData(email, updateData)).thenReturn(true);

        mockMvc.perform(post("/api/auth/register/update-data")
                        .param("email", email)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk());

        verify(verificationService).updateTempUserData(email, updateData);
    }

    @Test
    void verifyEmail_shouldSucceed_whenCodeIsValid() throws Exception {
        String email = "test@example.com";
        String code = "123456";
        when(verificationService.verifyCode(email, code)).thenReturn(true);

        mockMvc.perform(post("/api/auth/register/verify")
                        .param("email", email)
                        .param("code", code)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void verifyEmail_shouldFail_whenCodeIsInvalid() throws Exception {
        String email = "test@example.com";
        String code = "wrongcode";
        when(verificationService.verifyCode(email, code)).thenReturn(false);

        mockMvc.perform(post("/api/auth/register/verify")
                        .param("email", email)
                        .param("code", code)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid verification code"));
    }

    @Test
    void completeRegistration_shouldSucceed_whenVerificationDataExists() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Final")
                .surname("User")
                .email("finaluser@example.com")
                .password("finalPass123")
                .phoneNumber("+905009998877")
                .identityNumber("99887766554")
                .birthDate(LocalDate.of(1985, 3, 3))
                .height(180.0)
                .weight(80.0)
                .gender("FEMALE")
                .canSwim(false)
                .build();

        Map<String, Object> tempUserData = new HashMap<>(); // Simulate existing temp data
        when(verificationService.getTempUserData("finaluser@example.com")).thenReturn(tempUserData);
        // registrationService.register returns Member, but controller doesn't use it for response body
        when(registrationService.register(any(RegisterRequest.class))).thenReturn(null); // Or a dummy Member
        doNothing().when(verificationService).removeVerificationData("finaluser@example.com");

        mockMvc.perform(post("/api/auth/register/complete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(registrationService).register(any(RegisterRequest.class));
        verify(verificationService).removeVerificationData("finaluser@example.com");
    }

    @Test
    void resendVerificationCode_shouldSucceed_whenRegistrationInProgress() throws Exception {
        String email = "test@example.com";
        Map<String, Object> tempUserData = new HashMap<>();
        when(verificationService.getTempUserData(email)).thenReturn(tempUserData);
        when(verificationService.generateAndStoreCode(eq(email), eq(tempUserData))).thenReturn("newcode123");
        doNothing().when(emailService).sendVerificationEmail(email, "newcode123");

        mockMvc.perform(post("/api/auth/register/resend-code")
                        .param("email", email)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(emailService).sendVerificationEmail(email, "newcode123");
    }
}