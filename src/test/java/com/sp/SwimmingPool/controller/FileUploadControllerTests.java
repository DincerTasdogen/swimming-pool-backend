// src/test/java/com/sp/SwimmingPool/controller/FileUploadControllerTests.java
package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.CustomOAuth2UserService;
import com.sp.SwimmingPool.service.CustomUserDetailsService;
import com.sp.SwimmingPool.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileUploadController.class)
@Import(SecurityConfig.class)
public class FileUploadControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StorageService storageService;

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
    private Authentication regularUserAuthentication;

    @BeforeEach
    void setUp() {
        UserPrincipal adminPrincipal = UserPrincipal.builder().id(1).email("admin@example.com").role("ADMIN").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).build();
        adminAuthentication = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());

        UserPrincipal userPrincipal = UserPrincipal.builder().id(2).email("user@example.com").role("MEMBER").userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))).build();
        regularUserAuthentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
    }

    @Test
    void uploadPoolImage_asAdmin_shouldStoreFileAndReturnPath() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("file", "pool.jpg", MediaType.IMAGE_JPEG_VALUE, "imagedata".getBytes());
        String expectedPath = "pools/generated_pool.jpg";

        when(storageService.storeFile(imageFile, "pools")).thenReturn(expectedPath);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/upload/pool-image")
                        .file(imageFile)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path", is(expectedPath)));
    }

    @Test
    void uploadNewsImage_asAdmin_shouldStoreFileAndReturnPath() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("file", "news_article.png", MediaType.IMAGE_PNG_VALUE, "newsdata".getBytes());
        String expectedPath = "news/generated_news.png";

        when(storageService.storeFile(imageFile, "news")).thenReturn(expectedPath);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/upload/news-image")
                        .file(imageFile)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path", is(expectedPath)));
    }

    @Test
    void uploadPoolImage_asNonAdmin_shouldBeForbidden() throws Exception {
        UserPrincipal memberPrincipal = UserPrincipal.builder().id(2).email("member@example.com").role("MEMBER").userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))).build();
        Authentication memberAuthentication = new UsernamePasswordAuthenticationToken(memberPrincipal, null, memberPrincipal.getAuthorities());

        MockMultipartFile imageFile = new MockMultipartFile("file", "pool.jpg", MediaType.IMAGE_JPEG_VALUE, "imagedata".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/upload/pool-image")
                        .file(imageFile)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuthentication))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }


    @Test
    void downloadFile_shouldReturnFile_whenAccessible() throws Exception {
        String filePath = "public/images/some_image.jpg";
        byte[] fileContent = "jpegcontent".getBytes();
        Resource resource = new ByteArrayResource(fileContent);

        when(storageService.hasAccessToFile(filePath)).thenReturn(true);
        when(storageService.loadFileAsResource(filePath)).thenReturn(resource);

        MvcResult result = mockMvc.perform(get("/api/upload/files/" + filePath)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication)) // <--- ADD AUTH
                        .requestAttr(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/api/upload/files/" + filePath))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"some_image.jpg\""))
                .andReturn();

        assertArrayEquals(fileContent, result.getResponse().getContentAsByteArray());
    }

    @Test
    void downloadFile_shouldReturnForbidden_whenNotAccessible() throws Exception {
        String filePath = "restricted/secret.pdf";
        // Even if authenticated, the storageService denies access
        when(storageService.hasAccessToFile(filePath)).thenReturn(false);

        mockMvc.perform(get("/api/upload/files/" + filePath)
                        // Provide authentication
                        .with(SecurityMockMvcRequestPostProcessors.authentication(regularUserAuthentication))
                        .requestAttr(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/api/upload/files/" + filePath))
                .andExpect(status().isForbidden()); // Now this should work because the controller's logic is hit

        verify(storageService, never()).loadFileAsResource(anyString());
    }

    @Test
    void downloadFile_shouldReturnNotFound_whenIOException() throws Exception {
        String filePath = "public/images/non_existent.png";
        when(storageService.hasAccessToFile(filePath)).thenReturn(true); // Assuming access check passes before IO error
        when(storageService.loadFileAsResource(filePath)).thenThrow(new IOException("File not found"));

        mockMvc.perform(get("/api/upload/files/" + filePath)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication)) // <--- ADD AUTH
                        .requestAttr(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/api/upload/files/" + filePath))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteFile_asAdmin_shouldDeleteFile() throws Exception {
        String filePath = "news/to_delete.jpg";

        // storageService.deleteFile is void, so no when().thenReturn() needed for it
        // unless it throws an exception on failure, which we'd test separately.

        mockMvc.perform(delete("/api/upload/files/" + filePath)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                        .with(csrf())
                        .requestAttr(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/api/upload/files/" + filePath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("File deleted successfully")));

        verify(storageService).deleteFile(filePath);
    }

    @Test
    void deleteFile_asAdmin_shouldReturnBadRequest_whenIOException() throws Exception {
        String filePath = "news/problem_file.jpg";
        doThrow(new IOException("Deletion failed")).when(storageService).deleteFile(filePath);

        mockMvc.perform(delete("/api/upload/files/" + filePath)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                        .with(csrf())
                        .requestAttr(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/api/upload/files/" + filePath))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Failed to delete file: Deletion failed")));
    }
}