package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.MemberDTO;
import com.sp.SwimmingPool.dto.UserDTO;
import com.sp.SwimmingPool.model.enums.UserRoleEnum;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.*;
import com.sp.SwimmingPool.util.FilePathUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
public class ProfileControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MemberService memberService;
    @MockBean
    private UserService userService;
    @MockBean
    private StorageService storageService;
    @MockBean
    private FilePathUtil filePathUtil;

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
    private Authentication staffAuth;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        UserPrincipal memberPrincipal = UserPrincipal.builder().id(1).email("member@example.com").role("MEMBER").userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))).build();
        memberAuth = new UsernamePasswordAuthenticationToken(memberPrincipal, null, memberPrincipal.getAuthorities());

        UserPrincipal staffPrincipal = UserPrincipal.builder().id(10).email("staff@example.com").role("COACH").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_COACH"))).build();
        staffAuth = new UsernamePasswordAuthenticationToken(staffPrincipal, null, staffPrincipal.getAuthorities());

        UserPrincipal adminPrincipal = UserPrincipal.builder().id(10).email("admin@example.com").role("ADMIN").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).build();
        adminAuth = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
    }

    private MemberDTO createSampleMemberDTO(int id) {
        MemberDTO dto = new MemberDTO();
        dto.setId(id);
        dto.setName("Test Member");
        dto.setSurname("User");
        dto.setEmail("member" + id + "@example.com");
        dto.setPhoto("path/to/photo.jpg");
        dto.setIdPhotoFront("path/to/id_front.jpg");
        dto.setIdPhotoBack("path/to/id_back.jpg");
        dto.setBirthDate(LocalDate.of(1990,1,1));
        return dto;
    }

    private UserDTO createSampleUserDTO(int id) {
        UserDTO dto = new UserDTO();
        dto.setId(id);
        dto.setName("Test Staff");
        dto.setSurname("Person");
        dto.setEmail("staff" + id + "@example.com");
        dto.setRole(UserRoleEnum.COACH);
        return dto;
    }

    @Test
    void getMemberProfileDetails_asAuthenticatedUser_shouldReturnMember() throws Exception {
        int memberId = 1;
        MemberDTO memberDTO = createSampleMemberDTO(memberId);

        when(memberService.getMemberDetails(memberId)).thenReturn(memberDTO);
        when(filePathUtil.getFileUrl("path/to/photo.jpg")).thenReturn("http://host/url/photo.jpg");
        when(filePathUtil.getFileUrl("path/to/id_front.jpg")).thenReturn("http://host/url/id_front.jpg");
        when(filePathUtil.getFileUrl("path/to/id_back.jpg")).thenReturn("http://host/url/id_back.jpg");

        mockMvc.perform(get("/api/profile/member/{id}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth))) // Assuming member can view their own
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test Member")))
                .andExpect(jsonPath("$.photo", is("http://host/url/photo.jpg")));
    }

    @Test
    void getStaffProfileDetails_asAuthenticatedUser_shouldReturnStaff() throws Exception {
        int staffId = 10;
        UserDTO userDTO = createSampleUserDTO(staffId);
        when(userService.getUserDetails(staffId)).thenReturn(userDTO);

        mockMvc.perform(get("/api/profile/staff/{id}", staffId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(staffAuth))) // Assuming staff can view their own
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test Staff")))
                .andExpect(jsonPath("$.role", is("COACH")));
    }

    @Test
    void updateStaffProfileDetails_asAuthenticatedAdmin_shouldUpdate() throws Exception {
        int staffId = 10;
        UserDTO requestDTO = createSampleUserDTO(staffId);
        requestDTO.setName("Updated Staff Name");

        when(userService.updateUser(staffId, requestDTO)).thenReturn(requestDTO);

        mockMvc.perform(put("/api/profile/staff/{id}", staffId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());

        verify(userService).updateUser(eq(staffId), argThat(dto -> dto.getName().equals("Updated Staff Name")));
    }

    @Test
    void updateMemberProfileDetails_asAuthenticatedMember_shouldUpdate() throws Exception {
        int memberId = 1;
        MemberDTO requestDTO = createSampleMemberDTO(memberId);
        requestDTO.setPhoneNumber("+905557778899");

        when(memberService.updateMember(memberId, requestDTO)).thenReturn(requestDTO);

        mockMvc.perform(put("/api/profile/member/{id}", memberId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());

        verify(memberService).updateMember(eq(memberId), argThat(dto -> dto.getPhoneNumber().equals("+905557778899")));
    }

    @Test
    void uploadMemberPhoto_asAuthenticatedMember_shouldUploadAndReturnUrl() throws Exception {
        int memberId = 1;
        MockMultipartFile photoFile = new MockMultipartFile("photo", "new_profile.jpg", MediaType.IMAGE_JPEG_VALUE, "newphotodata".getBytes());
        String storedPath = "members/photos/new_profile.jpg";
        String fileUrl = "http://host/url/members/photos/new_profile.jpg";

        when(storageService.storeFile(any(MockMultipartFile.class), eq("members/photos"))).thenReturn(storedPath);
        MemberDTO updatedMemberByService = createSampleMemberDTO(memberId);
        updatedMemberByService.setPhoto(storedPath);

        when(memberService.updateMember(eq(memberId), any(MemberDTO.class))).thenReturn(updatedMemberByService);
        when(filePathUtil.getFileUrl(storedPath)).thenReturn(fileUrl);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/profile/member/{id}/photo", memberId)
                        .file(photoFile)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl", is(fileUrl)));

        verify(memberService).updateMember(eq(memberId), argThat(dto -> dto.getPhoto().equals(storedPath)));
    }
}