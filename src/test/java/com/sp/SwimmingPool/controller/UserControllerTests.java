package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.UserDTO;
import com.sp.SwimmingPool.model.entity.User;
import com.sp.SwimmingPool.model.enums.UserRoleEnum;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.CustomOAuth2UserService;
import com.sp.SwimmingPool.service.CustomUserDetailsService;
import com.sp.SwimmingPool.service.EmailService;
import com.sp.SwimmingPool.service.UserService;
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
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
public class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;
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

    private Authentication adminAuth;
    private Authentication nonAdminAuth; // For testing forbidden access

    @BeforeEach
    void setUp() {
        UserPrincipal adminPrincipal = UserPrincipal.builder().id(1).email("admin@example.com").role("ADMIN").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).build();
        adminAuth = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());

        UserPrincipal coachPrincipal = UserPrincipal.builder().id(2).email("coach@example.com").role("COACH").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_COACH"))).build();
        nonAdminAuth = new UsernamePasswordAuthenticationToken(coachPrincipal, null, coachPrincipal.getAuthorities());
    }

    private UserDTO createSampleUserDTO(int id, String name, UserRoleEnum role, String email) {
        UserDTO dto = new UserDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setSurname("Staff");
        dto.setEmail(email);
        dto.setRole(role);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    private User createSampleUserEntity(int id, String email, UserRoleEnum role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName("Test");
        user.setSurname("User");
        user.setRole(role);
        user.setPassword("hashedpassword"); // Not used directly in response
        return user;
    }

    @Test
    void createStaffUser_asAdmin_shouldCreateAndSendEmail() throws Exception {
        UserDTO requestDTO = new UserDTO();
        requestDTO.setName("Newest");
        requestDTO.setSurname("Staff");
        requestDTO.setEmail("newstaff@example.com");
        requestDTO.setRole(UserRoleEnum.COACH);

        User savedUserEntity = createSampleUserEntity(5, "newstaff@example.com", UserRoleEnum.COACH);
        UserDTO responseDTO = createSampleUserDTO(5, "Newest", UserRoleEnum.COACH, "newstaff@example.com");


        when(userService.createUser(any(UserDTO.class))).thenReturn(savedUserEntity);
        when(userService.storePasswordToken(eq(savedUserEntity.getId()), anyString(), any(LocalDateTime.class))).thenReturn(true);
        doNothing().when(emailService).sendStaffInvitation(eq("newstaff@example.com"), anyString());
        when(userService.convertToDto(savedUserEntity)).thenReturn(responseDTO);


        mockMvc.perform(post("/api/staff")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(5)))
                .andExpect(jsonPath("$.email", is("newstaff@example.com")));

        verify(emailService).sendStaffInvitation(eq("newstaff@example.com"), anyString());
    }

    @Test
    void createStaffUser_asNonAdmin_shouldBeForbidden() throws Exception {
        UserDTO requestDTO = new UserDTO();
        requestDTO.setEmail("attempt@example.com");
        requestDTO.setRole(UserRoleEnum.DOCTOR);

        mockMvc.perform(post("/api/staff")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(nonAdminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAllUsers_asAdmin_shouldReturnUsers() throws Exception {
        UserDTO user1 = createSampleUserDTO(1, "AdminUser", UserRoleEnum.ADMIN, "admin@example.com");
        UserDTO user2 = createSampleUserDTO(2, "CoachUser", UserRoleEnum.COACH, "coach@example.com");
        when(userService.listAllUsers()).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/api/staff")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("AdminUser")));
    }

    @Test
    void getUserDetails_asAdmin_shouldReturnUser() throws Exception {
        int userId = 1;
        UserDTO user = createSampleUserDTO(userId, "DetailedUser", UserRoleEnum.DOCTOR, "doctor@example.com");
        when(userService.getUserDetails(userId)).thenReturn(user);

        mockMvc.perform(get("/api/staff/{id}", userId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("DetailedUser")));
    }

    @Test
    void updateUser_asAdmin_shouldUpdateUser() throws Exception {
        int userId = 1;
        UserDTO requestDTO = createSampleUserDTO(userId, "UpdatedName", UserRoleEnum.COACH, "coach@example.com");
        UserDTO updatedDTO = createSampleUserDTO(userId, "UpdatedName", UserRoleEnum.COACH, "coach@example.com");

        when(userService.updateUser(eq(userId), any(UserDTO.class))).thenReturn(updatedDTO);

        mockMvc.perform(put("/api/staff/{id}", userId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("UpdatedName")));
    }

    @Test
    void deleteUser_asAdmin_shouldDeleteUser() throws Exception {
        int userId = 1;
        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/staff/{id}", userId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf()))
                .andExpect(status().isNoContent());
        verify(userService).deleteUser(userId);
    }

    @Test
    void activateStaffAccount_shouldSucceed_whenTokenIsValid() throws Exception {
        Map<String, String> requestBody = Map.of("token", "valid-token", "password", "newPassword123");
        when(userService.activateStaffWithToken("valid-token", "newPassword123")).thenReturn(true);

        mockMvc.perform(post("/api/staff/activate") // Public endpoint
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(content().string("Hesabınız başarıyla aktifleştirildi."));
    }

    @Test
    void activateStaffAccount_shouldFail_whenTokenIsInvalid() throws Exception {
        Map<String, String> requestBody = Map.of("token", "invalid-token", "password", "newPassword123");
        when(userService.activateStaffWithToken("invalid-token", "newPassword123")).thenReturn(false);

        mockMvc.perform(post("/api/staff/activate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Geçersiz veya süresi dolmuş token."));
    }
}