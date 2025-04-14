package com.sp.SwimmingPool;
import com.sp.SwimmingPool.repos.ReservationRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.dto.CreateReservationRequest;
import com.sp.SwimmingPool.dto.ReservationResponse;
import com.sp.SwimmingPool.dto.SessionResponse;
import com.sp.SwimmingPool.model.entity.Reservation;
import com.sp.SwimmingPool.service.ReservationService;
import com.sp.SwimmingPool.service.SessionService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ReservationControllerTests {

    @Autowired
    private MockMvc mockMvc; // ✅ Artık doğru şekilde enjekte ediliyor

    @MockBean
    private ReservationService reservationService; // Bu mock’lanıyor çünkü birim testte kullanılacak
    private ReservationRepository reservationRepository; // Burada repository'yi mockluyoruz

    @Autowired
    private SessionService sessionService;

    @Test
    public void AvailableSessionsForMember() {
        int memberId = 1;
        int packageId = 8;
        LocalDate date = LocalDate.now();

        try {
            List<SessionResponse> availableSessions = sessionService.getAvailableSessionsForMemberPackage(
                            memberId, packageId, date
                    ).stream()
                    .map(session -> {
                        SessionResponse resp = new SessionResponse();
                        resp.setId(session.getId());
                        resp.setSessionDate(session.getSessionDate());
                        resp.setStartTime(session.getStartTime());
                        resp.setEndTime(session.getEndTime());
                        resp.setAvailableSpots(session.getCapacity() - session.getCurrentBookings());
                        return resp;
                    })
                    .toList();

            assertThat(availableSessions).isNotNull();

        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void createReservationTest() {
        int memberId = 1;
        int sessionId = 2;
        int memberPackageId = 8;

        CreateReservationRequest request = new CreateReservationRequest();
        request.setMemberId(memberId);
        request.setSessionId(sessionId);
        request.setMemberPackageId(memberPackageId);

        Reservation mockReservation = new Reservation();
        mockReservation.setId(1);
        mockReservation.setMemberId(memberId);
        mockReservation.setSessionId(sessionId);
        mockReservation.setMemberPackageId(memberPackageId);

        try {
            when(reservationService.createReservation(memberId, sessionId, memberPackageId))
                    .thenReturn(mockReservation);

            Reservation result = reservationService.createReservation(memberId, sessionId, memberPackageId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(mockReservation.getId());
            assertThat(result.getMemberId()).isEqualTo(mockReservation.getMemberId());
            assertThat(result.getSessionId()).isEqualTo(mockReservation.getSessionId());
            assertThat(result.getMemberPackageId()).isEqualTo(mockReservation.getMemberPackageId());

        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Exception occurred: " + e.getMessage());
        }
    }

    @Test
    void getAvailableSessionsWithBody_shouldReturnSessionsFromDb() {
        int memberId = 2;
        int memberPackageId = 10;
        String date = "2025-04-15";

        String requestBody = String.format("""
    {
        "memberId": %d,
        "memberPackageId": %d,
        "date": "%s"
    }
    """, memberId, memberPackageId, date);

        try {
            MvcResult result = mockMvc.perform(post("/api/reservations/available-sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Response JSON: " + content);

            ObjectMapper mapper = new ObjectMapper();
            List<?> sessionList = mapper.readValue(content, List.class);
            assertFalse(sessionList.isEmpty(), "Session listesi boş olmamalı");
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Request sırasında hata oluştu: " + e.getMessage());
        }
    }

}
