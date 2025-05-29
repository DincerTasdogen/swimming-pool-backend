package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.PoolDTO;
import com.sp.SwimmingPool.model.entity.Pool;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.CustomOAuth2UserService;
import com.sp.SwimmingPool.service.CustomUserDetailsService;
import com.sp.SwimmingPool.service.PoolService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PoolController.class)
@Import(SecurityConfig.class)
public class PoolControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PoolService poolService;

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

    private Pool createSamplePool(int id, String name) {
        Pool pool = new Pool();
        pool.setId(id);
        pool.setName(name);
        pool.setLocation("Test Location");
        pool.setCity("Test City");
        pool.setLatitude(40.7128);
        pool.setLongitude(-74.0060);
        pool.setDepth(2.5);
        pool.setCapacity(100);
        pool.setOpenAt("08:00");
        pool.setCloseAt("20:00");
        pool.setActive(true);
        pool.setCreatedAt(LocalDateTime.now());
        pool.setUpdatedAt(LocalDateTime.now());
        pool.setFeatures(new ArrayList<>(List.of("Showers", "Lockers")));
        return pool;
    }

    private PoolDTO createSamplePoolDTO(String name) {
        PoolDTO dto = new PoolDTO();
        dto.setName(name);
        dto.setLocation("DTO Location");
        dto.setCity("DTO City");
        dto.setLatitude(41.0082);
        dto.setLongitude(28.9784);
        dto.setDepth(3.0);
        dto.setCapacity(150);
        dto.setOpenAt("09:00");
        dto.setCloseAt("21:00");
        dto.setActive(true);
        dto.setFeatures(List.of("Cafe", "WiFi"));
        return dto;
    }

    @Test
    void getAllPools_shouldReturnPools() throws Exception {
        Pool pool1 = createSamplePool(1, "Main Pool");
        Pool pool2 = createSamplePool(2, "Kids Pool");
        when(poolService.findAll()).thenReturn(List.of(pool1, pool2));

        mockMvc.perform(get("/api/pools")) // Public endpoint
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Main Pool")));
    }

    @Test
    void getRandomPools_shouldReturnRandomPools() throws Exception {
        int count = 2;
        Pool pool1 = createSamplePool(1, "Random Pool 1");
        Pool pool2 = createSamplePool(2, "Random Pool 2");
        when(poolService.getRandomPools(count)).thenReturn(List.of(pool1, pool2));

        mockMvc.perform(get("/api/pools/random/{count}", count)) // Public endpoint
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getPoolById_shouldReturnPool() throws Exception {
        int poolId = 1;
        Pool pool = createSamplePool(poolId, "Specific Pool");
        when(poolService.findById(poolId)).thenReturn(Optional.of(pool));

        mockMvc.perform(get("/api/pools/{id}", poolId)) // Public endpoint
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Specific Pool")));
    }

    @Test
    void getPoolById_shouldReturnNotFound_whenPoolDoesNotExist() throws Exception {
        int poolId = 99;
        when(poolService.findById(poolId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/pools/{id}", poolId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPool_asAdmin_shouldCreatePool() throws Exception {
        PoolDTO requestDTO = createSamplePoolDTO("New Admin Pool");
        Pool savedPool = createSamplePool(3, "New Admin Pool");
        // Ensure the savedPool reflects what the service would return after conversion and saving
        savedPool.setLocation(requestDTO.getLocation()); // Match DTO for verification

        // The controller converts DTO to Entity, sets timestamps, then calls service.save
        // We mock the service.save part.
        when(poolService.save(any(Pool.class))).thenAnswer(invocation -> {
            Pool poolToSave = invocation.getArgument(0);
            poolToSave.setId(3); // Simulate ID generation by DB
            poolToSave.setCreatedAt(LocalDateTime.now()); // Simulate timestamp set by controller
            poolToSave.setUpdatedAt(LocalDateTime.now());
            return poolToSave;
        });


        mockMvc.perform(post("/api/pools")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.name", is("New Admin Pool")))
                .andExpect(jsonPath("$.location", is("DTO Location")));
    }

    @Test
    void createPool_asMember_shouldBeForbidden() throws Exception {
        PoolDTO requestDTO = createSamplePoolDTO("Member Attempt Pool");
        mockMvc.perform(post("/api/pools")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatePool_asAdmin_shouldUpdatePool() throws Exception {
        int poolId = 1;
        PoolDTO requestDTO = createSamplePoolDTO("Updated Pool Name");
        requestDTO.setCapacity(200);

        Pool existingPool = createSamplePool(poolId, "Old Pool Name");
        Pool updatedPool = createSamplePool(poolId, "Updated Pool Name");
        updatedPool.setCapacity(200);
        updatedPool.setFeatures(requestDTO.getFeatures()); // Ensure features match

        when(poolService.findById(poolId)).thenReturn(Optional.of(existingPool));
        when(poolService.save(any(Pool.class))).thenReturn(updatedPool);

        mockMvc.perform(put("/api/pools/{id}", poolId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Pool Name")))
                .andExpect(jsonPath("$.capacity", is(200)))
                .andExpect(jsonPath("$.features", containsInAnyOrder("Cafe", "WiFi")));
    }

    @Test
    void deletePool_asAdmin_shouldDeletePool() throws Exception {
        int poolId = 1;
        Pool existingPool = createSamplePool(poolId, "To Delete");
        when(poolService.findById(poolId)).thenReturn(Optional.of(existingPool));
        doNothing().when(poolService).delete(poolId);

        mockMvc.perform(delete("/api/pools/{id}", poolId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf()))
                .andExpect(status().isNoContent());
        verify(poolService).delete(poolId);
    }

    @Test
    void filterPools_shouldReturnFilteredPools() throws Exception {
        String city = "Test City";
        Boolean isActive = true;
        Pool pool1 = createSamplePool(1, "Filtered Pool 1");
        pool1.setCity(city);
        pool1.setActive(isActive);

        when(poolService.filterPools(city, isActive)).thenReturn(List.of(pool1));

        mockMvc.perform(get("/api/pools/filter")
                        .param("city", city)
                        .param("isActive", isActive.toString())) // Public endpoint
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].city", is(city)))
                .andExpect(jsonPath("$[0].active", is(isActive)));
    }
}