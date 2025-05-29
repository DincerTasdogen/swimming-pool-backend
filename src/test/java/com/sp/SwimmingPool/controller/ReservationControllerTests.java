package com.sp.SwimmingPool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sp.SwimmingPool.config.SecurityConfig;
import com.sp.SwimmingPool.dto.AvailableSessionsRequest;
import com.sp.SwimmingPool.dto.CreateReservationRequest;
import com.sp.SwimmingPool.dto.ReservationResponse;
import com.sp.SwimmingPool.dto.SessionResponse;
import com.sp.SwimmingPool.model.entity.Pool;
import com.sp.SwimmingPool.model.entity.Reservation;
import com.sp.SwimmingPool.model.entity.Session;
import com.sp.SwimmingPool.model.enums.ReservationStatusEnum;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sp.SwimmingPool.security.oauth2.OAuth2SuccessHandler;
import com.sp.SwimmingPool.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
@Import(SecurityConfig.class)
public class ReservationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper; // Initialized in setUp

    @MockBean
    private ReservationService reservationService;
    @MockBean
    private SessionService sessionService;
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

    private Authentication memberAuth;
    private Authentication adminAuth;
    private Authentication coachAuth; // For complete-by-qr
    private Authentication doctorAuth; // For complete-by-qr
    private UserPrincipal memberPrincipal;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        memberPrincipal = UserPrincipal.builder().id(1).email("member@example.com").name("Test Member").role("MEMBER").userType("MEMBER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))).build();
        memberAuth = new UsernamePasswordAuthenticationToken(memberPrincipal, null, memberPrincipal.getAuthorities());

        UserPrincipal adminPrincipal = UserPrincipal.builder().id(100).email("admin@example.com").name("Test Admin").role("ADMIN").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).build();
        adminAuth = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());

        UserPrincipal coachPrincipal = UserPrincipal.builder().id(101).email("coach@example.com").name("Test Coach").role("COACH").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_COACH"))).build();
        coachAuth = new UsernamePasswordAuthenticationToken(coachPrincipal, null, coachPrincipal.getAuthorities());

        UserPrincipal doctorPrincipal = UserPrincipal.builder().id(102).email("doctor@example.com").name("Test Doctor").role("DOCTOR").userType("STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))).build();
        doctorAuth = new UsernamePasswordAuthenticationToken(doctorPrincipal, null, doctorPrincipal.getAuthorities());
    }

    private SessionResponse createSampleSessionResponse(int id, int poolId, String poolName, LocalDate date, LocalTime start, LocalTime end) {
        return new SessionResponse(id, poolId, poolName, date, start, end, 20, 5, 15, false, true, null);
    }

    private Reservation createSampleReservation(int id, int memberId, int sessionId, int memberPackageId, ReservationStatusEnum status) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setMemberId(memberId);
        reservation.setSessionId(sessionId);
        reservation.setMemberPackageId(memberPackageId);
        reservation.setStatus(status);
        reservation.setCreatedAt(LocalDateTime.now().minusHours(1));
        reservation.setUpdatedAt(LocalDateTime.now());
        return reservation;
    }

    private Session createSampleSessionEntity(int id, int poolId, LocalDate date, LocalTime start, LocalTime end) {
        Session session = new Session();
        session.setId(id);
        session.setPoolId(poolId);
        session.setSessionDate(date);
        session.setStartTime(start);
        session.setEndTime(end);
        session.setCapacity(20);
        session.setCurrentBookings(5); // Initial bookings
        session.setEducationSession(false);
        return session;
    }

    // --- POST /api/reservations/available-sessions (body) ---
    @Test
    void getAvailableSessionsWithBody_asMember_shouldReturnSessions() throws Exception {
        int memberId = memberPrincipal.getId();
        AvailableSessionsRequest request = new AvailableSessionsRequest();
        request.setMemberPackageId(10);
        request.setPoolId(20);
        request.setDate(LocalDate.now().plusDays(1));

        SessionResponse sessionResp = createSampleSessionResponse(1, 20, "Main Pool", request.getDate(), LocalTime.of(10,0), LocalTime.of(11,0));
        when(sessionService.getAvailableSessionsForMemberPackage(memberId, request.getMemberPackageId(), request.getPoolId(), request.getDate()))
                .thenReturn(List.of(sessionResp));

        mockMvc.perform(post("/api/reservations/available-sessions")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].poolName", is("Main Pool")));
    }

    @Test
    void getAvailableSessionsWithBody_asMember_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        AvailableSessionsRequest invalidRequest = new AvailableSessionsRequest(); // Missing fields

        mockMvc.perform(post("/api/reservations/available-sessions")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()); // Validation error
    }


    // --- GET /api/reservations/available-sessions (params) ---
    @Test
    void getAvailableSessionsForMember_asMember_shouldReturnSessions() throws Exception {
        int memberId = memberPrincipal.getId();
        int memberPackageId = 10;
        int poolId = 20;
        LocalDate date = LocalDate.now().plusDays(1);

        SessionResponse sessionResp = createSampleSessionResponse(1, poolId, "Main Pool", date, LocalTime.of(10,0), LocalTime.of(11,0));
        when(sessionService.getAvailableSessionsForMemberPackage(memberId, memberPackageId, poolId, date))
                .thenReturn(List.of(sessionResp));

        mockMvc.perform(get("/api/reservations/available-sessions")
                        .param("memberPackageId", String.valueOf(memberPackageId))
                        .param("poolId", String.valueOf(poolId))
                        .param("date", date.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)));
    }

    // --- POST /api/reservations ---
    @Test
    void createReservation_asMember_shouldCreateAndReturnReservationResponse() throws Exception {
        int memberId = memberPrincipal.getId();
        CreateReservationRequest request = new CreateReservationRequest();
        request.setSessionId(5);
        request.setMemberPackageId(15);

        Reservation createdReservation = createSampleReservation(1, memberId, request.getSessionId(), request.getMemberPackageId(), ReservationStatusEnum.CONFIRMED);
        Session sessionForReservation = createSampleSessionEntity(request.getSessionId(), 25, LocalDate.now().plusDays(1), LocalTime.of(14,0), LocalTime.of(15,0));
        Pool poolForReservation = new Pool();
        poolForReservation.setId(25);
        poolForReservation.setName("Sunny Pool");

        when(reservationService.createReservation(memberId, request.getSessionId(), request.getMemberPackageId()))
                .thenReturn(createdReservation);
        when(sessionService.getSession(request.getSessionId())).thenReturn(sessionForReservation);
        when(poolService.findById(sessionForReservation.getPoolId())).thenReturn(Optional.of(poolForReservation));

        mockMvc.perform(post("/api/reservations")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(createdReservation.getId())))
                .andExpect(jsonPath("$.status", is(ReservationStatusEnum.CONFIRMED.name())))
                .andExpect(jsonPath("$.poolName", is("Sunny Pool")));
    }

    @Test
    void createReservation_asMember_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        CreateReservationRequest invalidRequest = new CreateReservationRequest(); // Missing fields

        mockMvc.perform(post("/api/reservations")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()); // Validation error
    }


    // --- GET /api/reservations/me ---
    @Test
    void getMyReservations_asMember_shouldReturnPageOfReservations() throws Exception {
        int memberId = memberPrincipal.getId();
        ReservationResponse resResponse = new ReservationResponse(1, memberId, 5, 15, ReservationStatusEnum.CONFIRMED, LocalDateTime.now(), LocalDateTime.now(), LocalDate.now().plusDays(1), LocalTime.NOON, LocalTime.NOON.plusHours(1), "My Pool", false, 10);
        Page<ReservationResponse> pageResponse = new PageImpl<>(List.of(resResponse), PageRequest.of(0,10), 1);

        when(reservationService.getReservationsByMember(memberId, 0, 10)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/reservations/me")
                        .param("page", "0")
                        .param("size", "10")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].poolName", is("My Pool")));
    }

    // --- PUT /api/reservations/{reservationId}/cancel ---
    @Test
    void cancelReservation_asOwnerMember_shouldSucceed() throws Exception {
        int reservationId = 1;
        int memberId = memberPrincipal.getId();
        doNothing().when(reservationService).cancelReservation(reservationId, memberId);

        mockMvc.perform(put("/api/reservations/{reservationId}/cancel", reservationId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth))
                        .with(csrf()))
                .andExpect(status().isNoContent());
        verify(reservationService).cancelReservation(reservationId, memberId);
    }

    @Test
    void cancelReservation_asAdmin_shouldBeForbidden() throws Exception {
        // Assuming only owner member can cancel
        mockMvc.perform(put("/api/reservations/{reservationId}/cancel", 1)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }


    // --- PUT /api/reservations/{reservationId}/complete ---
    @Test
    void markReservationAsCompleted_asAdmin_shouldSucceed() throws Exception {
        int reservationId = 1;
        doNothing().when(reservationService).markReservationAsCompleted(reservationId);

        mockMvc.perform(put("/api/reservations/{reservationId}/complete", reservationId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf()))
                .andExpect(status().isNoContent());
        verify(reservationService).markReservationAsCompleted(reservationId);
    }

    @Test
    void markReservationAsCompleted_asMember_shouldBeForbidden() throws Exception {
        mockMvc.perform(put("/api/reservations/{reservationId}/complete", 1)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // --- PUT /api/reservations/{reservationId}/no-show ---
    @Test
    void markReservationAsNoShow_asAdmin_shouldSucceed() throws Exception {
        int reservationId = 1;
        doNothing().when(reservationService).markReservationAsNoShow(reservationId);

        mockMvc.perform(put("/api/reservations/{reservationId}/no-show", reservationId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf()))
                .andExpect(status().isNoContent());
        verify(reservationService).markReservationAsNoShow(reservationId);
    }

    // --- POST /api/reservations/complete-by-qr ---
    @Test
    void completeReservationByQr_asAdmin_shouldSucceed() throws Exception {
        Map<String, String> requestBody = Map.of("qrToken", "valid.qr.token");
        doNothing().when(reservationService).completeReservationByQrToken("valid.qr.token");

        mockMvc.perform(post("/api/reservations/complete-by-qr")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(content().string("Giriş başarılı, rezervasyon tamamlandı."));
        verify(reservationService).completeReservationByQrToken("valid.qr.token");
    }
    @Test
    void completeReservationByQr_asCoach_shouldSucceed() throws Exception {
        Map<String, String> requestBody = Map.of("qrToken", "valid.qr.token.coach");
        doNothing().when(reservationService).completeReservationByQrToken("valid.qr.token.coach");

        mockMvc.perform(post("/api/reservations/complete-by-qr")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(coachAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
    }
    @Test
    void completeReservationByQr_asDoctor_shouldSucceed() throws Exception {
        Map<String, String> requestBody = Map.of("qrToken", "valid.qr.token.doctor");
        doNothing().when(reservationService).completeReservationByQrToken("valid.qr.token.doctor");

        mockMvc.perform(post("/api/reservations/complete-by-qr")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(doctorAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
    }


    @Test
    void completeReservationByQr_withMissingToken_shouldReturnBadRequest() throws Exception {
        Map<String, String> requestBody = Map.of("qrToken", ""); // Empty token
        mockMvc.perform(post("/api/reservations/complete-by-qr")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("QR kodu eksik."));
    }

    @Test
    void completeReservationByQr_whenServiceThrowsException_shouldReturnBadRequestWithMessage() throws Exception {
        Map<String, String> requestBody = Map.of("qrToken", "exception.token");
        doThrow(new RuntimeException("Invalid token logic")).when(reservationService).completeReservationByQrToken("exception.token");

        mockMvc.perform(post("/api/reservations/complete-by-qr")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid token logic"));
    }


    // --- GET /api/reservations/{reservationId}/qr-token ---
    @Test
    void getReservationQrToken_asOwnerMember_shouldReturnToken() throws Exception {
        int reservationId = 1;
        int memberId = memberPrincipal.getId();
        String mockQrToken = "generated.qr.token.for.reservation";
        when(reservationService.generateReservationQrTokenForMember(reservationId, memberId)).thenReturn(mockQrToken);

        mockMvc.perform(get("/api/reservations/{reservationId}/qr-token", reservationId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(memberAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrToken", is(mockQrToken)));
    }

    @Test
    void getReservationQrToken_asAdmin_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/reservations/{reservationId}/qr-token", 1)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth)))
                .andExpect(status().isForbidden());
    }
}