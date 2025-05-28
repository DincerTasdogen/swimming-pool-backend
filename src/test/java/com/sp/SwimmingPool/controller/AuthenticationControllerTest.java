package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.AuthResponse;
import com.sp.SwimmingPool.dto.LoginRequest;
import com.sp.SwimmingPool.dto.RegisterRequest;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.User;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.repos.UserRepository;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.*;
import jakarta.servlet.http.HttpServletResponse;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@Import(SecurityConfig.class)
public class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;
    @MockBean
    private RegistrationService registrationService;
    @MockBean
    private CookieService cookieService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private VerificationService verificationService;
    @MockBean
    private EmailService emailService;
    @MockBean
    private MemberRepository memberRepository;
    @MockBean
    private MemberService memberService;
    @MockBean
    private UserRepository userRepository;

    // Required for JwtAuthenticationFilter if it's active with @WebMvcTest
    @MockBean
    private CustomUserDetailsService customUserDetailsService;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;
    @MockBean
    private OAuth2AuthenticationFailureHandler oAuth2FailureHandler;


    private UserPrincipal testUserPrincipal;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Default principal, can be overridden in specific tests
        testUserPrincipal = UserPrincipal.builder()
                .id(1)
                .email("test@example.com")
                .name("Test User")
                .role("MEMBER")
                .userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER")))
                .build();
        authentication = new UsernamePasswordAuthenticationToken(
                testUserPrincipal, null, testUserPrincipal.getAuthorities()
        );
    }

    @Test
    void login_shouldReturnAuthResponse_whenCredentialsAreValid() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");

        AuthResponse authResponse = AuthResponse.builder().id(1).email("test@example.com").role("MEMBER").build();

        when(authService.authenticate(any(LoginRequest.class), any(HttpServletResponse.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk()) // Should now be 200
                .andExpect(jsonPath("$.email", is("test@example.com")));

        verify(authService).authenticate(any(LoginRequest.class), any(HttpServletResponse.class));
    }

    @Test
    void logout_shouldClearCookieAndReturnOk() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication)))
                .andExpect(status().isOk());

        verify(cookieService).clearAuthCookie(any(HttpServletResponse.class));
    }

    @Test
    void getCurrentUser_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUser_shouldReturnMemberDetails_whenAuthenticatedAsMember() throws Exception {
        UserPrincipal memberPrincipal = UserPrincipal.builder()
                .id(1)
                .email("member@example.com")
                .name("Member Name") // Name from UserPrincipal
                .role("MEMBER")
                .userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER")))
                .build();
        Authentication memberAuth = new UsernamePasswordAuthenticationToken(
                memberPrincipal, null, memberPrincipal.getAuthorities()
        );

        Member memberDetails = new Member();
        memberDetails.setId(1);
        memberDetails.setEmail("member@example.com");
        memberDetails.setName("Actual Member Name");
        memberDetails.setSurname("MemberSurname");
        memberDetails.setStatus(StatusEnum.ACTIVE);

        when(memberRepository.findById(1)).thenReturn(Optional.of(memberDetails));

        mockMvc.perform(get("/api/auth/me")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.email", is("member@example.com")))
                .andExpect(jsonPath("$.name", is("Actual Member Name")))
                .andExpect(jsonPath("$.surname", is("MemberSurname")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.userType", is("MEMBER")));
    }

    @Test
    void getCurrentUser_shouldReturnStaffDetails_whenAuthenticatedAsStaff() throws Exception {
        UserPrincipal staffPrincipal = UserPrincipal.builder()
                .id(2)
                .email("staff@example.com")
                .name("Staff Name")
                .role("ADMIN")
                .userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
        Authentication staffAuth = new UsernamePasswordAuthenticationToken(
                staffPrincipal, null, staffPrincipal.getAuthorities()
        );

        User staffDetails = new User();
        staffDetails.setId(2);
        staffDetails.setEmail("staff@example.com");
        staffDetails.setName("Actual Staff Name");
        staffDetails.setSurname("StaffSurname");
        // Staff doesn't have status, swimmingLevel, canSwim in this context

        when(userRepository.findById(2)).thenReturn(Optional.of(staffDetails));

        mockMvc.perform(get("/api/auth/me")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(staffAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.email", is("staff@example.com")))
                .andExpect(jsonPath("$.name", is("Actual Staff Name")))
                .andExpect(jsonPath("$.surname", is("StaffSurname")))
                .andExpect(jsonPath("$.userType", is("STAFF")))
                .andExpect(jsonPath("$.status").doesNotExist()); // Or .isNull() depending on impl
    }


    @Test
    void registerOAuthUser_shouldReturnBadRequest_whenEmailAlreadyRegistered() throws Exception {
        Map<String, Object> oauthData = new HashMap<>();
        oauthData.put("email", "existing@example.com");
        oauthData.put("name", "OAuth");
        oauthData.put("surname", "User");
        oauthData.put("provider", "google");

        when(registrationService.isEmailRegistered("existing@example.com")).thenReturn(true);

        mockMvc.perform(post("/api/auth/register/oauth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oauthData)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email already registered"));
    }

    @Test
    void registerOAuthUser_shouldStoreTempData_whenRegistrationIsIncomplete() throws Exception {
        Map<String, Object> oauthData = new HashMap<>();
        oauthData.put("email", "new@example.com");
        oauthData.put("name", "OAuth");
        oauthData.put("surname", "User");
        oauthData.put("provider", "google");
        // Missing: phoneNumber, identityNumber, birthDate, height, weight, gender

        when(registrationService.isEmailRegistered("new@example.com")).thenReturn(false);
        when(registrationService.isIdentityNumberRegistered(any())).thenReturn(false); // Assuming not provided or not registered

        mockMvc.perform(post("/api/auth/register/oauth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oauthData)))
                .andExpect(status().isOk()); // Expects OK because it stores temp data

        verify(registrationService).storeOAuthTempData(oauthData);
    }

    @Test
    void registerOAuthUser_shouldCompleteRegistration_whenDataIsComplete() throws Exception {
        Map<String, Object> oauthData = new HashMap<>();
        oauthData.put("email", "complete@example.com");
        oauthData.put("name", "OAuthComplete");
        oauthData.put("surname", "UserComplete");
        oauthData.put("provider", "google");
        oauthData.put("phoneNumber", "+905551234567");
        oauthData.put("identityNumber", "12345678901");
        oauthData.put("birthDate", "1990-01-01");
        oauthData.put("height", "180.0");
        oauthData.put("weight", "75.0");
        oauthData.put("gender", "MALE");
        oauthData.put("canSwim", "true");
        oauthData.put("photo", "path/to/photo.jpg");
        oauthData.put("idPhotoFront", "path/to/id_front.jpg");
        oauthData.put("idPhotoBack", "path/to/id_back.jpg");


        when(registrationService.isEmailRegistered("complete@example.com")).thenReturn(false);
        when(registrationService.isIdentityNumberRegistered("12345678901")).thenReturn(false);

        Member registeredMember = new Member();
        registeredMember.setId(10);
        registeredMember.setEmail("complete@example.com");
        registeredMember.setName("OAuthComplete");

        when(registrationService.register(any(RegisterRequest.class))).thenReturn(registeredMember);
        when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("test.jwt.token");

        mockMvc.perform(post("/api/auth/register/oauth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oauthData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.email", is("complete@example.com")));

        verify(cookieService).createAuthCookie(eq("test.jwt.token"), any(HttpServletResponse.class));
    }


    @Test
    void forgotPassword_shouldSendEmail_whenMemberExists() throws Exception {
        Map<String, String> requestBody = Collections.singletonMap("email", "member@example.com");
        Member member = new Member();
        member.setEmail("member@example.com");

        when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(verificationService.generateAndStoreCode(eq("member@example.com"), anyMap())).thenReturn("123456");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(content().string("Doğrulama E-Postası gönderildi!"));

        verify(emailService).sendPasswordResetEmail("member@example.com", "123456");
    }

    @Test
    void forgotPassword_shouldReturnOk_whenMemberDoesNotExist() throws Exception {
        Map<String, String> requestBody = Collections.singletonMap("email", "unknown@example.com");
        when(memberRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(content().string("Doğrulama E-Postası gönderildi!")); // Message is same for privacy

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void verifyResetCode_shouldReturnOk_whenCodeIsValid() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", "test@example.com");
        requestBody.put("code", "123456");

        when(verificationService.verifyCode("test@example.com", "123456")).thenReturn(true);

        mockMvc.perform(post("/api/auth/verify-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("E-posta adresinizi doğruladınız! Şifrenizi şimdi yenileyebilirsiniz."));

        verify(verificationService).extendVerificationExpiry("test@example.com", 5);
    }

    @Test
    void verifyResetCode_shouldReturnBadRequest_whenCodeIsInvalid() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", "test@example.com");
        requestBody.put("code", "wrongcode");

        when(verificationService.verifyCode("test@example.com", "wrongcode")).thenReturn(false);

        mockMvc.perform(post("/api/auth/verify-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Doğrulama kodu yanlış ya da süresi geçmiş."));
    }

    @Test
    void resetPassword_shouldUpdatePassword_whenCodeIsValid() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", "test@example.com");
        requestBody.put("code", "123456");
        requestBody.put("newPassword", "newSecurePassword123");

        when(verificationService.verifyCode("test@example.com", "123456")).thenReturn(true);
        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new Member()));
        // memberService.updatePassword does not return a value (void)
        // verificationService.removeVerificationData does not return a value (void)

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(content().string("Şifreniz başarıyla güncellendi."));

        verify(memberService).updatePassword("test@example.com", "newSecurePassword123");
        verify(verificationService).removeVerificationData("test@example.com");
    }

    @Test
    void resetPassword_shouldReturnBadRequest_whenCodeIsInvalid() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", "test@example.com");
        requestBody.put("code", "invalidcode");
        requestBody.put("newPassword", "newSecurePassword123");

        when(verificationService.verifyCode("test@example.com", "invalidcode")).thenReturn(false);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Doğrulama kodu yanlış ya da süresi geçmiş."));
    }
}