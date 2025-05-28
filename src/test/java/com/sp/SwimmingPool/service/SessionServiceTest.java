package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.SessionResponse; // Your provided DTO
import com.sp.SwimmingPool.exception.EntityNotFoundException;
import com.sp.SwimmingPool.exception.InvalidOperationException;
import com.sp.SwimmingPool.model.entity.MemberPackage;
import com.sp.SwimmingPool.model.entity.PackageType;
import com.sp.SwimmingPool.model.entity.Pool;
import com.sp.SwimmingPool.model.entity.Reservation;
import com.sp.SwimmingPool.model.entity.Session;
import com.sp.SwimmingPool.model.enums.MemberPackagePaymentStatusEnum;
import com.sp.SwimmingPool.model.enums.ReservationStatusEnum;
import com.sp.SwimmingPool.repos.MemberPackageRepository;
import com.sp.SwimmingPool.repos.PackageTypeRepository;
import com.sp.SwimmingPool.repos.PoolRepository;
import com.sp.SwimmingPool.repos.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private MemberPackageRepository memberPackageRepository;
    @Mock
    private PackageTypeRepository packageTypeRepository;
    @Mock
    private ReservationService reservationService;
    @Mock
    private PoolRepository poolRepository;

    @InjectMocks
    private SessionService sessionService;

    private Session session1;
    private Session session2_education;
    private MemberPackage memberPackage_valid;
    private PackageType packageType_general;
    private Pool pool1;

    private final int memberId = 101;
    private final int poolId = 201;
    private final LocalDate testDate = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() {
        pool1 = new Pool();
        pool1.setId(poolId);
        pool1.setName("Test Pool");

        session1 = new Session();
        session1.setId(1);
        session1.setPoolId(poolId);
        session1.setSessionDate(testDate);
        session1.setStartTime(LocalTime.of(10, 0));
        session1.setEndTime(LocalTime.of(11, 0));
        session1.setCapacity(10);
        session1.setCurrentBookings(5);
        session1.setEducationSession(false);
        session1.setCreatedAt(LocalDateTime.now());
        session1.setUpdatedAt(LocalDateTime.now());

        session2_education = new Session();
        session2_education.setId(2);
        session2_education.setPoolId(poolId);
        session2_education.setSessionDate(testDate);
        session2_education.setStartTime(LocalTime.of(14, 0));
        session2_education.setEndTime(LocalTime.of(15, 0));
        session2_education.setCapacity(8);
        session2_education.setCurrentBookings(2);
        session2_education.setEducationSession(true);

        packageType_general = new PackageType();
        packageType_general.setId(11);
        packageType_general.setName("General Swim Package");
        packageType_general.setStartTime(LocalTime.of(9, 0));
        packageType_general.setEndTime(LocalTime.of(22, 0)); // Corrected to a valid time
        packageType_general.setEducationPackage(false);
        packageType_general.setRequiresSwimmingAbility(false);

        memberPackage_valid = new MemberPackage();
        memberPackage_valid.setId(1001);
        memberPackage_valid.setMemberId(memberId);
        memberPackage_valid.setPackageTypeId(packageType_general.getId());
        memberPackage_valid.setActive(true);
        memberPackage_valid.setSessionsRemaining(5);
        memberPackage_valid.setPaymentStatus(MemberPackagePaymentStatusEnum.COMPLETED);
        memberPackage_valid.setPoolId(poolId);
    }

    @Test
    void getSession_exists_returnsSession() {
        when(sessionRepository.findById(session1.getId())).thenReturn(Optional.of(session1));
        Session found = sessionService.getSession(session1.getId());
        assertNotNull(found);
        assertEquals(session1.getId(), found.getId());
    }

    @Test
    void getSession_notExists_throwsEntityNotFoundException() {
        when(sessionRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> sessionService.getSession(999));
    }

    @Test
    void getSessionsByDate_returnsListOfSessions() {
        when(sessionRepository.findBySessionDate(testDate)).thenReturn(List.of(session1, session2_education));
        List<Session> sessions = sessionService.getSessionsByDate(testDate);
        assertEquals(2, sessions.size());
    }

    @Nested
    class GetAvailableSessionsForMemberPackageTests {
        @Test
        void getAvailableSessionsForMemberPackage_success() {
            when(memberPackageRepository.findById(memberPackage_valid.getId())).thenReturn(Optional.of(memberPackage_valid));
            when(packageTypeRepository.findById(memberPackage_valid.getPackageTypeId())).thenReturn(Optional.of(packageType_general));
            when(poolRepository.findById(poolId)).thenReturn(Optional.of(pool1));
            when(sessionRepository.findAvailableSessionsForPackage(
                    eq(poolId), eq(testDate),
                    eq(packageType_general.getStartTime()), any(LocalTime.class),
                    eq(packageType_general.isEducationPackage()),
                    any(LocalDate.class), any(LocalTime.class),
                    any(LocalDate.class), any(LocalTime.class)
            )).thenReturn(List.of(session1));
            when(reservationService.getMemberConfirmedReservationsOnDate(memberId, testDate)).thenReturn(Collections.emptyList());

            // Be explicit with the overloaded method for hasTimeConflict
            when(reservationService.hasTimeConflict(
                    any(com.sp.SwimmingPool.dto.SessionResponse.class), // Specify the DTO type
                    anyList()
            )).thenReturn(false); // No conflict

            List<SessionResponse> availableSessions = sessionService.getAvailableSessionsForMemberPackage(
                    memberId, memberPackage_valid.getId(), poolId, testDate
            );

            assertNotNull(availableSessions);
            assertEquals(1, availableSessions.size());
            SessionResponse sr = availableSessions.getFirst();
            assertEquals(session1.getId(), sr.getId());
            assertEquals(pool1.getName(), sr.getPoolName());
            assertEquals(session1.getCapacity() - session1.getCurrentBookings(), sr.getAvailableSpots());
            assertTrue(sr.isBookable());
            assertNull(sr.getBookableReason());
        }

        @Test
        void getAvailableSessionsForMemberPackage_memberPackageNotFound_throwsEntityNotFound() {
            when(memberPackageRepository.findById(9999)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () ->
                    sessionService.getAvailableSessionsForMemberPackage(memberId, 9999, poolId, testDate));
        }

        @Test
        void getAvailableSessionsForMemberPackage_memberPackageNotBelongingToAuthMember_throwsInvalidOperation() {
            memberPackage_valid.setMemberId(memberId + 1);
            when(memberPackageRepository.findById(memberPackage_valid.getId())).thenReturn(Optional.of(memberPackage_valid));
            assertThrows(InvalidOperationException.class, () ->
                    sessionService.getAvailableSessionsForMemberPackage(memberId, memberPackage_valid.getId(), poolId, testDate));
        }

        @Test
        void getAvailableSessionsForMemberPackage_packageInactive_throwsInvalidOperation() {
            memberPackage_valid.setActive(false);
            when(memberPackageRepository.findById(memberPackage_valid.getId())).thenReturn(Optional.of(memberPackage_valid));
            assertThrows(InvalidOperationException.class, () ->
                    sessionService.getAvailableSessionsForMemberPackage(memberId, memberPackage_valid.getId(), poolId, testDate));
        }

        @Test
        void getAvailableSessionsForMemberPackage_noSessionsRemaining_throwsInvalidOperation() {
            memberPackage_valid.setSessionsRemaining(0);
            when(memberPackageRepository.findById(memberPackage_valid.getId())).thenReturn(Optional.of(memberPackage_valid));
            assertThrows(InvalidOperationException.class, () ->
                    sessionService.getAvailableSessionsForMemberPackage(memberId, memberPackage_valid.getId(), poolId, testDate));
        }

        @Test
        void getAvailableSessionsForMemberPackage_paymentPending_throwsInvalidOperation() {
            memberPackage_valid.setPaymentStatus(MemberPackagePaymentStatusEnum.PENDING);
            when(memberPackageRepository.findById(memberPackage_valid.getId())).thenReturn(Optional.of(memberPackage_valid));
            assertThrows(InvalidOperationException.class, () ->
                    sessionService.getAvailableSessionsForMemberPackage(memberId, memberPackage_valid.getId(), poolId, testDate));
        }

        @Test
        void getAvailableSessionsForMemberPackage_packageForDifferentPool_throwsInvalidOperation() {
            memberPackage_valid.setPoolId(poolId + 1);
            when(memberPackageRepository.findById(memberPackage_valid.getId())).thenReturn(Optional.of(memberPackage_valid));
            assertThrows(InvalidOperationException.class, () ->
                    sessionService.getAvailableSessionsForMemberPackage(memberId, memberPackage_valid.getId(), poolId, testDate));
        }

        @Test
        void getAvailableSessionsForMemberPackage_packageTypeNotFound_throwsEntityNotFound() {
            when(memberPackageRepository.findById(memberPackage_valid.getId())).thenReturn(Optional.of(memberPackage_valid));
            when(packageTypeRepository.findById(memberPackage_valid.getPackageTypeId())).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () ->
                    sessionService.getAvailableSessionsForMemberPackage(memberId, memberPackage_valid.getId(), poolId, testDate));
        }

        @Test
        void getAvailableSessionsForMemberPackage_filtersByTimeConflict() {
            when(memberPackageRepository.findById(memberPackage_valid.getId())).thenReturn(Optional.of(memberPackage_valid));
            when(packageTypeRepository.findById(memberPackage_valid.getPackageTypeId())).thenReturn(Optional.of(packageType_general));
            when(poolRepository.findById(poolId)).thenReturn(Optional.of(pool1));
            // This list will be mapped to SessionResponse objects
            when(sessionRepository.findAvailableSessionsForPackage(anyInt(), any(), any(), any(), anyBoolean(), any(), any(), any(), any()))
                    .thenReturn(List.of(session1, session2_education));

            Reservation existingReservation = new Reservation();
            existingReservation.setSessionId(99); // Dummy ID
            existingReservation.setStatus(ReservationStatusEnum.CONFIRMED);
            List<Reservation> memberReservations = List.of(existingReservation);

            when(reservationService.getMemberConfirmedReservationsOnDate(memberId, testDate)).thenReturn(memberReservations);

            // Use thenAnswer for more control over the overloaded method
            when(reservationService.hasTimeConflict(
                    any(com.sp.SwimmingPool.dto.SessionResponse.class), // Match the DTO version
                    eq(memberReservations) // Be specific with the list if possible, or anyList()
            )).thenAnswer(invocation -> {
                SessionResponse srArg = invocation.getArgument(0);
                if (srArg == null) return false; // Should not happen, but defensive
                if (srArg.getId() == session1.getId()) {
                    return true; // session1 conflicts
                }
                if (srArg.getId() == session2_education.getId()) {
                    return false; // session2_education does not conflict
                }
                return false; // Default for any other SessionResponse
            });

            List<SessionResponse> availableSessions = sessionService.getAvailableSessionsForMemberPackage(
                    memberId, memberPackage_valid.getId(), poolId, testDate
            );
            assertEquals(1, availableSessions.size());
            assertEquals(session2_education.getId(), availableSessions.getFirst().getId());
            assertTrue(availableSessions.getFirst().isBookable());
        }
    }

    @Test
    void createSession_success() {
        Session newSession = new Session();
        newSession.setPoolId(poolId);
        newSession.setSessionDate(testDate);
        newSession.setStartTime(LocalTime.of(16, 0));
        newSession.setEndTime(LocalTime.of(17, 0));
        newSession.setCapacity(15);
        newSession.setEducationSession(false);

        when(sessionRepository.existsByPoolIdAndSessionDateAndStartTime(
                newSession.getPoolId(), newSession.getSessionDate(), newSession.getStartTime()))
                .thenReturn(false);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session s = invocation.getArgument(0);
            s.setId(10);
            return s;
        });

        Session created = sessionService.createSession(newSession);

        assertNotNull(created);
        assertEquals(10, created.getId());
        assertEquals(0, created.getCurrentBookings());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getCurrentBookings());
    }

    @Test
    void createSession_startTimeAfterEndTime_throwsInvalidOperationException() {
        Session newSession = new Session();
        newSession.setStartTime(LocalTime.of(17, 0));
        newSession.setEndTime(LocalTime.of(16, 0));
        newSession.setCapacity(10);
        assertThrows(InvalidOperationException.class, () -> sessionService.createSession(newSession));
    }

    @Test
    void createSession_capacityNotPositive_throwsInvalidOperationException() {
        Session newSession = new Session();
        newSession.setStartTime(LocalTime.of(16, 0));
        newSession.setEndTime(LocalTime.of(17, 0));
        newSession.setCapacity(0);
        assertThrows(InvalidOperationException.class, () -> sessionService.createSession(newSession));
    }

    @Test
    void createSession_alreadyExists_throwsInvalidOperationException() {
        when(sessionRepository.existsByPoolIdAndSessionDateAndStartTime(
                session1.getPoolId(), session1.getSessionDate(), session1.getStartTime()))
                .thenReturn(true);
        assertThrows(InvalidOperationException.class, () -> sessionService.createSession(session1));
    }

    @Test
    void updateSession_success() {
        Session updatedDetails = new Session();
        updatedDetails.setCapacity(12);
        updatedDetails.setEducationSession(true);
        LocalDateTime originalUpdatedAt = session1.getUpdatedAt();


        when(sessionRepository.findById(session1.getId())).thenReturn(Optional.of(session1));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session updated = sessionService.updateSession(session1.getId(), updatedDetails);

        assertNotNull(updated);
        assertEquals(12, updated.getCapacity());
        assertTrue(updated.isEducationSession());
        assertNotNull(updated.getUpdatedAt());
        // Check if updated timestamp is after or equal to original (allowing for very fast execution)
        assertTrue(updated.getUpdatedAt().isAfter(originalUpdatedAt) || updated.getUpdatedAt().isEqual(originalUpdatedAt));


        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        assertEquals(12, captor.getValue().getCapacity());
        assertTrue(captor.getValue().isEducationSession());
    }

    @Test
    void updateSession_newCapacityLessThanCurrentBookings_throwsInvalidOperationException() {
        Session updatedDetails = new Session();
        updatedDetails.setCapacity(4); // session1 has 5 bookings
        updatedDetails.setEducationSession(false);

        when(sessionRepository.findById(session1.getId())).thenReturn(Optional.of(session1));
        assertThrows(InvalidOperationException.class, () -> sessionService.updateSession(session1.getId(), updatedDetails));
    }

    @Test
    void deleteSession_success_noBookings() {
        session1.setCurrentBookings(0);
        when(sessionRepository.findById(session1.getId())).thenReturn(Optional.of(session1));
        doNothing().when(sessionRepository).delete(session1);

        sessionService.deleteSession(session1.getId());
        verify(sessionRepository).delete(session1);
    }

    @Test
    void deleteSession_hasBookings_throwsInvalidOperationException() {
        session1.setCurrentBookings(1);
        when(sessionRepository.findById(session1.getId())).thenReturn(Optional.of(session1));
        assertThrows(InvalidOperationException.class, () -> sessionService.deleteSession(session1.getId()));
        verify(sessionRepository, never()).delete(any(Session.class));
    }

    @Test
    void updateSessionEducationStatus_success() {
        when(sessionRepository.findById(session1.getId())).thenReturn(Optional.of(session1));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session updated = sessionService.updateSessionEducationStatus(session1.getId(), true);
        assertTrue(updated.isEducationSession());

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        assertTrue(captor.getValue().isEducationSession());
    }

    @Test
    void incrementSessionBookings_success() {
        when(sessionRepository.findById(session1.getId())).thenReturn(Optional.of(session1));
        sessionService.incrementSessionBookings(session1.getId());
        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        assertEquals(6, captor.getValue().getCurrentBookings());
    }

    @Test
    void incrementSessionBookings_sessionFull_throwsInvalidOperationException() {
        // session2_full has capacity 5, bookings 5
        when(sessionRepository.findById(session2_education.getId())).thenReturn(Optional.of(session2_education));
        session2_education.setCurrentBookings(session2_education.getCapacity());
        assertThrows(InvalidOperationException.class, () -> sessionService.incrementSessionBookings(session2_education.getId()));
    }

    @Test
    void decrementSessionBookings_success() {
        when(sessionRepository.findById(session1.getId())).thenReturn(Optional.of(session1));
        sessionService.decrementSessionBookings(session1.getId());
        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        assertEquals(4, captor.getValue().getCurrentBookings());
    }

    @Test
    void decrementSessionBookings_bookingsAlreadyZero_noChangeAndLogsWarning() {
        session1.setCurrentBookings(0);
        when(sessionRepository.findById(session1.getId())).thenReturn(Optional.of(session1));

        sessionService.decrementSessionBookings(session1.getId());
        verify(sessionRepository, never()).save(any(Session.class));
        assertEquals(0, session1.getCurrentBookings());
    }

    @Test
    void getSessionsForPoolInRange_returnsCorrectSessions() {
        LocalDate startDate = testDate.minusDays(1);
        LocalDate endDate = testDate.plusDays(1);
        when(sessionRepository.findByPoolIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(poolId, startDate, endDate))
                .thenReturn(List.of(session1, session2_education));

        List<Session> result = sessionService.getSessionsForPoolInRange(poolId, startDate, endDate);

        assertEquals(2, result.size());
        assertTrue(result.contains(session1));
        assertTrue(result.contains(session2_education));
    }
}