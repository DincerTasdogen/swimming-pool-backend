// src/test/java/com/sp/SwimmingPool/controller/MemberPackageControllerTests.java
package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.MemberPackageDTO;
import com.sp.SwimmingPool.model.enums.MemberPackagePaymentStatusEnum;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberPackageController.class)
@Import(SecurityConfig.class)
public class MemberPackageControllerTests {

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

    private Authentication memberAuth;
    private Authentication adminAuth;


    @BeforeEach
    void setUp() {
        UserPrincipal memberPrincipal = UserPrincipal.builder().id(1).email("member@example.com").role("MEMBER").userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))).build();
        memberAuth = new UsernamePasswordAuthenticationToken(memberPrincipal, null, memberPrincipal.getAuthorities());

        UserPrincipal adminPrincipal = UserPrincipal.builder().id(100).email("admin@example.com").role("ADMIN").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).build();
        adminAuth = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
    }

    private MemberPackageDTO createSamplePackageDTO(int id, int memberId, String packageName) {
        return MemberPackageDTO.builder()
                .id(id)
                .memberId(memberId)
                .packageTypeId(1)
                .purchaseDate(LocalDateTime.now().minusDays(5))
                .sessionsRemaining(5)
                .active(true)
                .poolId(1)
                .paymentStatus(MemberPackagePaymentStatusEnum.COMPLETED)
                .packageName(packageName)
                .packageStartTime(LocalTime.of(9,0))
                .packageEndTime(LocalTime.of(18,0))
                .isEducationPackage(false)
                .build();
    }

    @Test
    void getAllMemberPackages_asMember_shouldReturnPackages() throws Exception {
        int memberId = 1;
        MemberPackageDTO pkg1 = createSamplePackageDTO(1, memberId, "Summer Pass");
        when(packageService.getMemberPackages(memberId)).thenReturn(List.of(pkg1));

        mockMvc.perform(get("/api/MemberPackages/all/{memberId}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].packageName", is("Summer Pass")));
    }

    @Test
    void getActiveMemberPackages_asMember_shouldReturnActivePackages() throws Exception {
        int memberId = 1;
        MemberPackageDTO activePkg = createSamplePackageDTO(2, memberId, "Active Gold");
        activePkg.setActive(true);
        when(packageService.getActiveMemberPackages(memberId)).thenReturn(List.of(activePkg));

        mockMvc.perform(get("/api/MemberPackages/active/{memberId}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].active", is(true)));
    }

    @Test
    void getPreviousMemberPackages_asMember_shouldReturnPreviousPackages() throws Exception {
        int memberId = 1;
        MemberPackageDTO prevPkg = createSamplePackageDTO(3, memberId, "Old Bronze");
        prevPkg.setActive(false); // Assuming previous means inactive
        when(packageService.getPreviousMemberPackages(memberId)).thenReturn(List.of(prevPkg));

        mockMvc.perform(get("/api/MemberPackages/previous/{memberId}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].active", is(false)));
    }

    @Test
    void canBuyPackage_asMember_shouldReturnBoolean() throws Exception {
        int memberId = 1;
        Integer poolId = 10;
        when(packageService.canBuyPackage(memberId, poolId)).thenReturn(true);

        mockMvc.perform(get("/api/MemberPackages/can-buy/{memberId}", memberId)
                        .param("poolId", poolId.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void canBuyPackage_asMember_withoutPoolId_shouldReturnBoolean() throws Exception {
        int memberId = 1;
        when(packageService.canBuyPackage(memberId, null)).thenReturn(false);

        mockMvc.perform(get("/api/MemberPackages/can-buy/{memberId}", memberId)
                        // No poolId param
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void createMemberPackage_asAdmin_shouldCreatePackage() throws Exception {
        // Assuming admin might create/assign packages. If it's member-only, adjust auth.
        MemberPackageDTO requestDTO = MemberPackageDTO.builder()
                .memberId(5)
                .packageTypeId(2)
                .poolId(3)
                .sessionsRemaining(10)
                .active(true)
                .paymentStatus(MemberPackagePaymentStatusEnum.PENDING)
                .build();

        MemberPackageDTO createdDTO = createSamplePackageDTO(10, 5, "Admin Assigned");
        createdDTO.setPaymentStatus(MemberPackagePaymentStatusEnum.PENDING);


        when(packageService.createMemberPackage(any(MemberPackageDTO.class))).thenReturn(createdDTO);

        mockMvc.perform(post("/api/MemberPackages")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth)) // Or memberAuth if members can create
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.packageName", is("Admin Assigned")));
    }
}