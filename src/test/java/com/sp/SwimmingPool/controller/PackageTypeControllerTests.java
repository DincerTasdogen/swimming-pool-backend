package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.PackageTypeDTO;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.CustomOAuth2UserService;
import com.sp.SwimmingPool.service.CustomUserDetailsService;
import com.sp.SwimmingPool.service.PackageService;
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

import java.time.LocalTime;
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

@WebMvcTest(PackageTypeController.class)
@Import(SecurityConfig.class)
public class PackageTypeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PackageService packageService;

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
    private Authentication memberAuth; // For testing public endpoints if needed

    @BeforeEach
    void setUp() {
        UserPrincipal adminPrincipal = UserPrincipal.builder().id(1).email("admin@example.com").role("ADMIN").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).build();
        adminAuth = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());

        UserPrincipal memberPrincipal = UserPrincipal.builder().id(2).email("member@example.com").role("MEMBER").userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))).build();
        memberAuth = new UsernamePasswordAuthenticationToken(memberPrincipal, null, memberPrincipal.getAuthorities());
    }

    private PackageTypeDTO createSamplePackageTypeDTO(int id, String name) {
        return PackageTypeDTO.builder()
                .id(id)
                .name(name)
                .description("Sample package description")
                .sessionLimit(10)
                .price(100.0)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .isEducationPackage(false)
                .requiresSwimmingAbility(false)
                .multiplePools(false)
                .isActive(true)
                .build();
    }

    @Test
    void getAllPackageTypes_shouldReturnPackageTypes() throws Exception {
        PackageTypeDTO dto1 = createSamplePackageTypeDTO(1, "Gold Package");
        PackageTypeDTO dto2 = createSamplePackageTypeDTO(2, "Silver Package");
        when(packageService.listPackageTypes()).thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/packages")) // Public endpoint
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Gold Package")));
    }

    @Test
    void getPackageTypeById_shouldReturnPackageType() throws Exception {
        int packageId = 1;
        PackageTypeDTO dto = createSamplePackageTypeDTO(packageId, "Gold Package");
        when(packageService.getPackageById(packageId)).thenReturn(dto);

        mockMvc.perform(get("/api/packages/{id}", packageId)) // Public endpoint
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Gold Package")));
    }

    @Test
    void getPackageTypeById_shouldReturnNotFound_whenPackageDoesNotExist() throws Exception {
        int packageId = 99;
        when(packageService.getPackageById(packageId)).thenThrow(new IllegalArgumentException("Not found"));

        mockMvc.perform(get("/api/packages/{id}", packageId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPackageType_asAdmin_shouldCreatePackage() throws Exception {
        PackageTypeDTO requestDTO = PackageTypeDTO.builder().name("Bronze Package").sessionLimit(5).price(50.0).isActive(true).build();
        PackageTypeDTO createdDTO = createSamplePackageTypeDTO(3, "Bronze Package");

        when(packageService.createPackage(any(PackageTypeDTO.class))).thenReturn(createdDTO);

        mockMvc.perform(post("/api/packages")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.name", is("Bronze Package")));
    }

    @Test
    void createPackageType_asMember_shouldBeForbidden() throws Exception {
        PackageTypeDTO requestDTO = PackageTypeDTO.builder().name("Member Attempt Package").build();
        mockMvc.perform(post("/api/packages")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatePackageType_asAdmin_shouldUpdatePackage() throws Exception {
        int packageId = 1;
        PackageTypeDTO requestDTO = createSamplePackageTypeDTO(packageId, "Updated Gold Package");
        requestDTO.setPrice(120.0);
        PackageTypeDTO updatedDTO = createSamplePackageTypeDTO(packageId, "Updated Gold Package");
        updatedDTO.setPrice(120.0);

        when(packageService.updatePackage(eq(packageId), any(PackageTypeDTO.class))).thenReturn(updatedDTO);

        mockMvc.perform(put("/api/packages/{id}", packageId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Gold Package")))
                .andExpect(jsonPath("$.price", is(120.0)));
    }

    @Test
    void deletePackageType_asAdmin_shouldDeletePackage() throws Exception {
        int packageId = 1;
        doNothing().when(packageService).deletePackage(packageId); // void method

        mockMvc.perform(delete("/api/packages/{id}", packageId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf()))
                .andExpect(status().isNoContent());
        verify(packageService).deletePackage(packageId);
    }

    @Test
    void getEducationPackages_shouldReturnEducationPackages() throws Exception {
        PackageTypeDTO eduPkg = createSamplePackageTypeDTO(5, "Swimming Lessons");
        eduPkg.setEducationPackage(true);
        when(packageService.listEducationPackages()).thenReturn(List.of(eduPkg));

        mockMvc.perform(get("/api/packages/education")) // Public endpoint
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isEducationPackage", is(true)));
    }

    @Test
    void getOtherPackages_shouldReturnOtherPackages() throws Exception {
        PackageTypeDTO otherPkg = createSamplePackageTypeDTO(6, "Fun Swim Pack");
        otherPkg.setEducationPackage(false);
        when(packageService.listOtherPackages()).thenReturn(List.of(otherPkg));

        mockMvc.perform(get("/api/packages/other")) // Public endpoint
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isEducationPackage", is(false)));
    }
}