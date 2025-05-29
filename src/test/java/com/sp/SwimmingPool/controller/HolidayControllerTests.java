package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.HolidayDTO;
import com.sp.SwimmingPool.model.entity.Holiday;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.CustomOAuth2UserService;
import com.sp.SwimmingPool.service.CustomUserDetailsService;
import com.sp.SwimmingPool.service.HolidayService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HolidayController.class)
@Import(SecurityConfig.class)
public class HolidayControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HolidayService holidayService;

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
    private Authentication nonAdminAuthentication;

    @BeforeEach
    void setUp() {
        UserPrincipal adminPrincipal = UserPrincipal.builder().id(1).email("admin@example.com").role("ADMIN").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).build();
        adminAuthentication = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());

        UserPrincipal memberPrincipal = UserPrincipal.builder().id(2).email("member@example.com").role("MEMBER").userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))).build();
        nonAdminAuthentication = new UsernamePasswordAuthenticationToken(memberPrincipal, null, memberPrincipal.getAuthorities());
    }

    @Test
    void getHolidaysInRange_shouldReturnHolidays() throws Exception {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 5);
        List<LocalDate> holidays = List.of(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 3));

        when(holidayService.getHolidayDatesInRange(startDate, endDate)).thenReturn(holidays);

        mockMvc.perform(get("/api/holidays")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]", is("2025-01-01")));
    }

    @Test
    void getFixedHolidays_shouldReturnFormattedHolidays() throws Exception {
        Set<MonthDay> fixedHolidays = Set.of(MonthDay.of(1, 1), MonthDay.of(5, 19));

        when(holidayService.getFixedNationalHolidays()).thenReturn(fixedHolidays);

        mockMvc.perform(get("/api/holidays/fixed")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]", is("JANUARY 1")))
                .andExpect(jsonPath("$[1]", is("MAY 19")));
    }

    @Test
    void getCustomHolidays_shouldReturnHolidayDTOs() throws Exception {
        Holiday holiday1 = new Holiday(1L, LocalDate.of(2025, 7, 4), "Independence Day Custom", LocalDateTime.now());
        Holiday holiday2 = new Holiday(2L, LocalDate.of(2025, 12, 25), "Christmas Custom", LocalDateTime.now());
        when(holidayService.getCustomHolidaysForCurrentYear()).thenReturn(List.of(holiday1, holiday2));

        mockMvc.perform(get("/api/holidays/custom")
                .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].description", is("Independence Day Custom")))
                .andExpect(jsonPath("$[1].date", is("2025-12-25")));
    }

    @Test
    void isHoliday_shouldReturnTrue_whenDateIsHoliday() throws Exception {
        LocalDate date = LocalDate.of(2025, 1, 1);
        when(holidayService.isHoliday(date)).thenReturn(true);

        mockMvc.perform(get("/api/holidays/check").param("date", date.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void addHoliday_asAdmin_shouldCreateHoliday() throws Exception {
        HolidayDTO requestDTO = new HolidayDTO(null, LocalDate.of(2025, 11, 11), "Veterans Day");
        Holiday createdHoliday = new Holiday(3L, LocalDate.of(2025, 11, 11), "Veterans Day", LocalDateTime.now());

        when(holidayService.addCustomHoliday(requestDTO.getDate(), requestDTO.getDescription()))
                .thenReturn(createdHoliday);

        mockMvc.perform(post("/api/holidays")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.description", is("Veterans Day")));
    }

    @Test
    void addHoliday_asAdmin_shouldReturnBadRequest_whenServiceReturnsNull() throws Exception {
        HolidayDTO requestDTO = new HolidayDTO(null, LocalDate.of(2025, 1, 1), "Duplicate Day");
        when(holidayService.addCustomHoliday(requestDTO.getDate(), requestDTO.getDescription())).thenReturn(null);

        mockMvc.perform(post("/api/holidays")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Could not add holiday. It may be a duplicate or invalid date.")));
    }


    @Test
    void addHoliday_asNonAdmin_shouldBeForbidden() throws Exception {
        HolidayDTO requestDTO = new HolidayDTO(null, LocalDate.of(2025, 3, 17), "St. Patrick's Day");

        mockMvc.perform(post("/api/holidays")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(nonAdminAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteHoliday_asAdmin_shouldDeleteHoliday() throws Exception {
        LocalDate dateToDelete = LocalDate.of(2025, 11, 11);
        when(holidayService.removeCustomHoliday(dateToDelete)).thenReturn(true);

        mockMvc.perform(delete("/api/holidays")
                        .param("date", dateToDelete.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Holiday successfully deleted")));

        verify(holidayService).removeCustomHoliday(dateToDelete);
    }

    @Test
    void deleteHoliday_asAdmin_shouldReturnBadRequest_whenDeletionFails() throws Exception {
        LocalDate dateToDelete = LocalDate.of(2025, 1, 1); // e.g., a fixed holiday
        when(holidayService.removeCustomHoliday(dateToDelete)).thenReturn(false);

        mockMvc.perform(delete("/api/holidays")
                        .param("date", dateToDelete.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Could not delete holiday. It may be a fixed holiday or not exist.")));
    }
}