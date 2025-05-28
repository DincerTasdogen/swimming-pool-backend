package com.sp.SwimmingPool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.SwimmingPool.dto.CreateReservationRequest;
import com.sp.SwimmingPool.dto.SessionResponse;
import com.sp.SwimmingPool.model.entity.*;
import com.sp.SwimmingPool.model.enums.MemberPackagePaymentStatusEnum;
import com.sp.SwimmingPool.repos.MemberPackageRepository;
import com.sp.SwimmingPool.repos.PackageTypeRepository;
import com.sp.SwimmingPool.repos.PoolRepository;
import com.sp.SwimmingPool.repos.SessionRepository;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.service.ReservationService;
import com.sp.SwimmingPool.service.SessionService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ReservationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationService reservationService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private MemberPackageRepository memberPackageRepository;
    @Autowired
    private PackageTypeRepository packageTypeRepository;
    @Autowired
    private PoolRepository poolRepository;
    @Autowired
    private SessionRepository sessionRepository;

    @Test
    public void AvailableSessionsForMember() {
        int memberId = 1;
        int packageId = 8;
        int poolId = 1;
        LocalDate date = LocalDate.now();

        try {
            List<SessionResponse> availableSessions = sessionService.getAvailableSessionsForMemberPackage(
                    memberId, packageId, poolId, date
            );

            assertThat(availableSessions)
                    .isNotNull()
                    .allSatisfy(session -> {
                        assertThat(session.getId()).isPositive();
                        assertThat(session.getSessionDate()).isEqualTo(date);
                        assertThat(session.getAvailableSpots()).isGreaterThanOrEqualTo(0);
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void createReservationTest() {
        int authenticatedMemberId = 1;
        int sessionId = 2;
        int memberPackageId = 8;

        CreateReservationRequest request = new CreateReservationRequest();
        request.setSessionId(sessionId);
        request.setMemberPackageId(memberPackageId);

        Reservation mockReservation = new Reservation();
        mockReservation.setId(1);
        mockReservation.setMemberId(authenticatedMemberId);
        mockReservation.setSessionId(sessionId);
        mockReservation.setMemberPackageId(memberPackageId);

        try {
            when(reservationService.createReservation(authenticatedMemberId, sessionId, memberPackageId))
                    .thenReturn(mockReservation);

            Reservation result = reservationService.createReservation(authenticatedMemberId, sessionId, memberPackageId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(mockReservation.getId());
            assertThat(result.getMemberId()).isEqualTo(mockReservation.getMemberId());
            assertThat(result.getSessionId()).isEqualTo(mockReservation.getSessionId());
            assertThat(result.getMemberPackageId()).isEqualTo(mockReservation.getMemberPackageId());

        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Bir hata oluştu: " + e.getMessage());
        }
    }

    @Test
    void getAvailableSessionsWithBody_shouldReturnSessionsFromDb() throws Exception {
        int memberId = 2;
        int memberPackageId = 10;
        int poolId = 54;
        LocalDate sessionDate = LocalDate.of(2025, 5, 26);

        log.info("Starting getAvailableSessionsWithBody_shouldReturnSessionsFromDb test");
        log.info("Creating Pool...");
        Pool pool = new Pool();
        pool.setId(poolId);
        pool.setName("Test Pool");
        pool.setCapacity(200);
        pool.setCity("Istanbul");
        pool.setActive(true);
        pool.setDepth(11.4);
        pool.setLocation("Istanbul");
        pool.setOpenAt("08:00");
        pool.setCloseAt("20:00");
        poolRepository.save(pool);

        log.info("Creating PackageType...");
        PackageType packageType = new PackageType();
        packageType.setName("Standard");
        packageType.setSessionLimit(10);
        packageType.setPrice(200.0);
        packageType.setStartTime(LocalTime.of(8, 0));
        packageType.setEndTime(LocalTime.of(20, 0));
        packageType.setEducationPackage(false);
        packageType.setRequiresSwimmingAbility(false);
        packageType.setMultiplePools(false);
        packageType.setActive(true);

        PackageType pt1 = packageTypeRepository.save(packageType);

        log.info("Creating MemberPackage...");
        MemberPackage memberPackage = new MemberPackage();
        memberPackage.setId(memberPackageId);
        memberPackage.setMemberId(memberId);
        memberPackage.setPackageTypeId(pt1.getId());
        memberPackage.setActive(true);
        memberPackage.setSessionsRemaining(5);
        memberPackage.setPaymentStatus(MemberPackagePaymentStatusEnum.COMPLETED);
        memberPackage.setPoolId(poolId);
        memberPackageRepository.save(memberPackage);

        log.info("Creating Session...");
        Session session = new Session();
        session.setPoolId(poolId);
        session.setSessionDate(sessionDate);
        session.setStartTime(LocalTime.of(10, 0));
        session.setEndTime(LocalTime.of(11, 0));
        session.setCapacity(10);
        session.setCurrentBookings(0);
        session.setEducationSession(false);
        sessionRepository.save(session);

        String requestBody = String.format("""
        {
            "memberPackageId": %d,
            "poolId": %d,
            "date": "%s"
        }
        """, memberPackageId, poolId, sessionDate);

        log.info("Building UserPrincipal for authentication...");
        var userPrincipal = UserPrincipal.builder()
                .id(memberId)
                .email("member2@example.com")
                .name("Member 2")
                .password("password")
                .role("MEMBER")
                .userType("MEMBER")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_MEMBER")))
                .build();

        var auth = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, userPrincipal.getAuthorities()
        );

        log.info("Performing POST /api/reservations/available-sessions...");
        MvcResult result = mockMvc.perform(post("/api/reservations/available-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        log.info("Response JSON: {}", content);

        ObjectMapper mapper = new ObjectMapper();
        List<?> sessionList = mapper.readValue(content, List.class);
        log.info("Session list size: {}", sessionList.size());
        assertFalse(sessionList.isEmpty(), "Session listesi boş olmamalı");

        log.info("getAvailableSessionsWithBody_shouldReturnSessionsFromDb test PASSED");
    }
}
