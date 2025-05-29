package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.EducationTimeConfigRequest;
import com.sp.SwimmingPool.model.entity.EducationTimeConfig;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.CustomOAuth2UserService;
import com.sp.SwimmingPool.service.CustomUserDetailsService;
import com.sp.SwimmingPool.service.EducationTimeConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EducationTimeConfigController.class)
@Import(SecurityConfig.class)
public class EducationTimeConfigControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EducationTimeConfigService educationTimeConfigService;

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

    private Authentication adminAuthentication;
    private Authentication coachAuthentication;
    private Authentication memberAuthentication; // For unauthorized tests

    @BeforeEach
    void setUp() {
        UserPrincipal adminPrincipal = UserPrincipal.builder().id(1).email("admin@example.com").role("ADMIN").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).build();
        adminAuthentication = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());

        UserPrincipal coachPrincipal = UserPrincipal.builder().id(2).email("coach@example.com").role("COACH").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_COACH"))).build();
        coachAuthentication = new UsernamePasswordAuthenticationToken(coachPrincipal, null, coachPrincipal.getAuthorities());

        UserPrincipal memberPrincipal = UserPrincipal.builder().id(3).email("member@example.com").role("MEMBER").userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))).build();
        memberAuthentication = new UsernamePasswordAuthenticationToken(memberPrincipal, null, memberPrincipal.getAuthorities());
    }

    private EducationTimeConfig createSampleConfig(Long id, boolean isActive, String description) {
        EducationTimeConfig config = new EducationTimeConfig();
        config.setId(id);
        config.setStartTime(LocalTime.of(9, 0));
        config.setEndTime(LocalTime.of(10, 0));
        config.setApplicableDays(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
        config.setDescription(description);
        config.setActive(isActive);
        return config;
    }

    // --- Tests for getAllConfigs ---
    @Test
    void getAllConfigs_asAdmin_withActiveTrue_shouldReturnActiveConfigs() throws Exception {
        EducationTimeConfig activeConfig = createSampleConfig(1L, true, "Active Morning");
        when(educationTimeConfigService.getAllActiveConfigs()).thenReturn(List.of(activeConfig));

        mockMvc.perform(get("/api/education-time-configs")
                        .param("active", "true")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description", is("Active Morning")))
                .andExpect(jsonPath("$[0].active", is(true)));
        verify(educationTimeConfigService).getAllActiveConfigs();
        verify(educationTimeConfigService, never()).getAllConfigs();
    }

    @Test
    void getAllConfigs_asCoach_withActiveFalse_shouldReturnAllConfigs() throws Exception {
        // When 'active' is false, the controller logic defaults to getAllConfigs()
        EducationTimeConfig config1 = createSampleConfig(1L, true, "Config One");
        EducationTimeConfig config2 = createSampleConfig(2L, false, "Config Two");
        when(educationTimeConfigService.getAllConfigs()).thenReturn(List.of(config1, config2));

        mockMvc.perform(get("/api/education-time-configs")
                        .param("active", "false") // This specific value makes it call getAllConfigs()
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuthentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        verify(educationTimeConfigService, never()).getAllActiveConfigs();
        verify(educationTimeConfigService).getAllConfigs();
    }

    @ParameterizedTest
    @NullSource // For when 'active' parameter is not present
    @ValueSource(strings = {"anyOtherValueNotTrue"}) // For when 'active' is present but not "true" (case-insensitive)
    void getAllConfigs_asAdmin_withActiveNullOrNotTrue_shouldReturnAllConfigs(String activeParamValue) throws Exception {
        EducationTimeConfig config1 = createSampleConfig(1L, true, "Any Config 1");
        EducationTimeConfig config2 = createSampleConfig(2L, false, "Any Config 2");
        when(educationTimeConfigService.getAllConfigs()).thenReturn(List.of(config1, config2));

        var requestBuilder = get("/api/education-time-configs")
                .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication));
        if (activeParamValue != null) {
            requestBuilder.param("isActive", activeParamValue);
        }


        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        verify(educationTimeConfigService, never()).getAllActiveConfigs();
        verify(educationTimeConfigService).getAllConfigs();
    }


    @Test
    void getAllConfigs_asAdmin_whenServiceReturnsEmptyList_shouldReturnOkWithEmptyList() throws Exception {
        when(educationTimeConfigService.getAllConfigs()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/education-time-configs")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllConfigs_asMember_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/education-time-configs")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuthentication)))
                .andExpect(status().isForbidden());
    }

    // --- Tests for createConfig ---
    @Test
    void createConfig_asCoach_shouldCreateAndReturnConfig() throws Exception {
        EducationTimeConfigRequest request = new EducationTimeConfigRequest();
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(11, 0));
        request.setApplicableDays(Set.of(DayOfWeek.TUESDAY));
        request.setDescription("Coach Session");

        EducationTimeConfig createdConfig = createSampleConfig(3L, true, "Coach Session");
        createdConfig.setStartTime(request.getStartTime());

        when(educationTimeConfigService.createEducationTimeConfig(
                request.getStartTime(), request.getEndTime(), request.getApplicableDays(), request.getDescription()
        )).thenReturn(createdConfig);

        mockMvc.perform(post("/api/education-time-configs")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.description", is("Coach Session")));
    }

    @Test
    void createConfig_asAdmin_whenServiceReturnsNull_shouldReturnBadRequest() throws Exception {
        EducationTimeConfigRequest request = new EducationTimeConfigRequest();
        request.setStartTime(LocalTime.of(10,0)); // Valid request
        request.setEndTime(LocalTime.of(11,0));
        request.setApplicableDays(Set.of(DayOfWeek.FRIDAY));

        when(educationTimeConfigService.createEducationTimeConfig(any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/education-time-configs")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid config data."));
    }

    @Test
    void createConfig_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        EducationTimeConfigRequest invalidRequest = new EducationTimeConfigRequest(); // Missing required fields

        mockMvc.perform(post("/api/education-time-configs")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()); // Validation error
    }

    @Test
    void createConfig_asMember_shouldBeForbidden() throws Exception {
        EducationTimeConfigRequest request = new EducationTimeConfigRequest();
        request.setStartTime(LocalTime.of(10,0));
        request.setEndTime(LocalTime.of(11,0));
        request.setApplicableDays(Set.of(DayOfWeek.MONDAY));

        mockMvc.perform(post("/api/education-time-configs")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }


    // --- Tests for updateConfig ---
    @Test
    void updateConfig_asAdmin_shouldUpdateAndReturnConfig() throws Exception {
        Long configId = 1L;
        EducationTimeConfigRequest request = new EducationTimeConfigRequest();
        request.setDescription("Admin Updated Session");
        request.setActive(false);
        request.setStartTime(LocalTime.of(14,0));
        request.setEndTime(LocalTime.of(15,0));
        request.setApplicableDays(Set.of(DayOfWeek.SATURDAY));


        EducationTimeConfig updatedConfig = createSampleConfig(configId, false, "Admin Updated Session");
        updatedConfig.setStartTime(request.getStartTime());

        when(educationTimeConfigService.updateEducationTimeConfig(
                eq(configId), eq(request.getStartTime()), eq(request.getEndTime()), eq(request.getApplicableDays()),
                eq(request.getDescription()), eq(request.getActive())
        )).thenReturn(updatedConfig);

        mockMvc.perform(patch("/api/education-time-configs/{id}", configId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("Admin Updated Session")))
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    void updateConfig_asCoach_whenServiceReturnsNull_shouldReturnBadRequest() throws Exception {
        Long configId = 2L;
        EducationTimeConfigRequest request = new EducationTimeConfigRequest();
        request.setDescription("Coach Update Attempt");
        request.setActive(true);
        request.setStartTime(LocalTime.of(10,0));
        request.setEndTime(LocalTime.of(11,0));
        request.setApplicableDays(Set.of(DayOfWeek.SUNDAY));


        when(educationTimeConfigService.updateEducationTimeConfig(
                anyLong(), any(), any(), any(), any(), anyBoolean()
        )).thenReturn(null);

        mockMvc.perform(patch("/api/education-time-configs/{id}", configId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid or non-existent config."));
    }

    @Test
    void updateConfig_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        Long configId = 1L;
        EducationTimeConfigRequest invalidRequest = new EducationTimeConfigRequest();
        // Missing required fields like startTime, endTime, applicableDays for a full update
        // but @Valid on DTO will catch it.

        mockMvc.perform(patch("/api/education-time-configs/{id}", configId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()); // Validation error
    }

    @Test
    void updateConfig_asMember_shouldBeForbidden() throws Exception {
        Long configId = 1L;
        EducationTimeConfigRequest request = new EducationTimeConfigRequest();
        request.setDescription("Member Update Attempt");
        request.setStartTime(LocalTime.of(10,0));
        request.setEndTime(LocalTime.of(11,0));
        request.setApplicableDays(Set.of(DayOfWeek.MONDAY));


        mockMvc.perform(patch("/api/education-time-configs/{id}", configId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- Tests for deleteConfig ---
    @Test
    void deleteConfig_asAdmin_shouldDeleteAndReturnOk() throws Exception {
        Long configId = 1L;
        when(educationTimeConfigService.deleteEducationTimeConfig(configId)).thenReturn(true);

        mockMvc.perform(delete("/api/education-time-configs/{id}", configId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Config deleted."));
        verify(educationTimeConfigService).deleteEducationTimeConfig(configId);
    }

    @Test
    void deleteConfig_asCoach_whenConfigNotFound_shouldReturnNotFound() throws Exception {
        Long configId = 99L; // Non-existent
        when(educationTimeConfigService.deleteEducationTimeConfig(configId)).thenReturn(false);

        mockMvc.perform(delete("/api/education-time-configs/{id}", configId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuthentication))
                        .with(csrf()))
                .andExpect(status().isNotFound()) // Expecting 404 based on controller logic
                .andExpect(content().string("Config not found."));
        verify(educationTimeConfigService).deleteEducationTimeConfig(configId);
    }

    @Test
    void deleteConfig_asMember_shouldBeForbidden() throws Exception {
        Long configId = 1L;
        mockMvc.perform(delete("/api/education-time-configs/{id}", configId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuthentication))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}