// src/test/java/com/sp/SwimmingPool/controller/NewsControllerTests.java
package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.NewsDTO;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.CustomOAuth2UserService;
import com.sp.SwimmingPool.service.CustomUserDetailsService;
import com.sp.SwimmingPool.service.NewsService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NewsController.class)
@Import(SecurityConfig.class)
public class NewsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NewsService newsService;

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
    private Authentication memberAuth;

    @BeforeEach
    void setUp() {
        UserPrincipal adminPrincipal = UserPrincipal.builder().id(1).email("admin@example.com").role("ADMIN").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).build();
        adminAuth = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());

        UserPrincipal memberPrincipal = UserPrincipal.builder().id(2).email("member@example.com").role("MEMBER").userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))).build();
        memberAuth = new UsernamePasswordAuthenticationToken(memberPrincipal, null, memberPrincipal.getAuthorities());
    }

    private NewsDTO createSampleNewsDTO(Integer id, String title) {
        return NewsDTO.builder()
                .id(id)
                .title(title)
                .content("Sample news content.")
                .author("Admin")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    void getAllNews_shouldReturnNewsList() throws Exception {
        NewsDTO news1 = createSampleNewsDTO(1, "Pool Reopens!");
        NewsDTO news2 = createSampleNewsDTO(2, "New Classes Announced");
        when(newsService.getAllNews()).thenReturn(List.of(news1, news2));

        mockMvc.perform(get("/api/news")) // Public endpoint
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Pool Reopens!")));
    }

    @Test
    void getNews_byId_shouldReturnNews() throws Exception {
        int newsId = 1;
        NewsDTO news = createSampleNewsDTO(newsId, "Specific News");
        when(newsService.getNewsById(newsId)).thenReturn(news);

        mockMvc.perform(get("/api/news/{id}", newsId)) // Public endpoint
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Specific News")));
    }

    @Test
    void createNews_asAdmin_shouldCreateNews() throws Exception {
        NewsDTO requestDTO = NewsDTO.builder().title("Breaking News").content("Something happened.").author("Editor").build();
        NewsDTO createdDTO = createSampleNewsDTO(3, "Breaking News");

        when(newsService.createNews(any(NewsDTO.class))).thenReturn(createdDTO);

        mockMvc.perform(post("/api/news")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk()) // Controller returns Ok, not Created
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.title", is("Breaking News")));
    }

    @Test
    void createNews_asMember_shouldBeForbidden() throws Exception {
        NewsDTO requestDTO = NewsDTO.builder().title("Member News").content("...").build();
        mockMvc.perform(post("/api/news")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateNews_asAdmin_shouldUpdateNews() throws Exception {
        int newsId = 1;
        NewsDTO requestDTO = NewsDTO.builder().title("Updated News Title").content("Updated content.").build();
        NewsDTO updatedDTO = createSampleNewsDTO(newsId, "Updated News Title");
        updatedDTO.setContent("Updated content.");

        when(newsService.updateNews(eq(newsId), any(NewsDTO.class))).thenReturn(updatedDTO);

        mockMvc.perform(put("/api/news/{id}", newsId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated News Title")));
    }

    @Test
    void deleteNews_asAdmin_shouldDeleteNews() throws Exception {
        int newsId = 1;
        doNothing().when(newsService).deleteNews(newsId); // void method

        mockMvc.perform(delete("/api/news/{id}", newsId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf()))
                .andExpect(status().isNoContent());
        verify(newsService).deleteNews(newsId);
    }
}