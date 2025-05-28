package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.EducationTimeConfig;
import com.sp.SwimmingPool.model.entity.Pool;
import com.sp.SwimmingPool.model.entity.Session;
import com.sp.SwimmingPool.repos.EducationTimeConfigRepository;
import com.sp.SwimmingPool.repos.PoolRepository;
import com.sp.SwimmingPool.repos.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledSessionCreationServiceTest {

    @Mock
    private PoolRepository poolRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private HolidayService holidayService;
    @Mock
    private EducationTimeConfigRepository educationTimeConfigRepository;

    @Spy
    @InjectMocks
    private ScheduledSessionCreationService scheduledSessionCreationService;

    private Pool pool1;
    private Pool pool2_invalidTimes;
    private EducationTimeConfig eduConfigMonday;
    private LocalDate today;
    private LocalDate tomorrow;
    private LocalDate dayAfterTomorrow;


    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        tomorrow = today.plusDays(1);
        dayAfterTomorrow = today.plusDays(2);


        pool1 = new Pool();
        pool1.setId(1);
        pool1.setName("Active Pool 1");
        pool1.setOpenAt("09:00");
        pool1.setCloseAt("21:00");
        pool1.setCapacity(20);
        pool1.setActive(true);

        pool2_invalidTimes = new Pool();
        pool2_invalidTimes.setId(2);
        pool2_invalidTimes.setName("Pool Invalid Times");
        pool2_invalidTimes.setOpenAt("22:00"); // Open after close
        pool2_invalidTimes.setCloseAt("10:00");
        pool2_invalidTimes.setCapacity(10);
        pool2_invalidTimes.setActive(true);

        eduConfigMonday = new EducationTimeConfig();
        eduConfigMonday.setStartTime(LocalTime.of(10, 0));
        eduConfigMonday.setEndTime(LocalTime.of(12, 0));
        Set<DayOfWeek> mondays = new HashSet<>();
        mondays.add(DayOfWeek.MONDAY);
        eduConfigMonday.setApplicableDays(mondays);
        eduConfigMonday.setActive(true);
    }

    @Test
    void ensureMinimumSessionAvailability_noActivePools_doesNothing() {
        when(poolRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());

        scheduledSessionCreationService.ensureMinimumSessionAvailability();

        verify(sessionRepository, never()).countByPoolIdAndSessionDate(anyInt(), any(LocalDate.class));
        // Verifying that generateScheduledSessions was NOT called.
        // Since generateScheduledSessions is public, we can use a spy to verify it wasn't called.
        verify(scheduledSessionCreationService, never()).generateScheduledSessions();
    }

    @Test
    void ensureMinimumSessionAvailability_sessionsExistForAllPoolsAndDays_doesNotGenerate() {
        when(poolRepository.findByIsActiveTrue()).thenReturn(List.of(pool1));
        // Assume today, tomorrow, dayAfterTomorrow are not holidays
        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);

        // Mock session counts to be > 0 for all relevant days
        when(sessionRepository.countByPoolIdAndSessionDate(eq(pool1.getId()), eq(today))).thenReturn(5L);
        when(sessionRepository.countByPoolIdAndSessionDate(eq(pool1.getId()), eq(tomorrow))).thenReturn(5L);
        when(sessionRepository.countByPoolIdAndSessionDate(eq(pool1.getId()), eq(dayAfterTomorrow))).thenReturn(5L);
        // Add more days if MINIMUM_DAYS_OF_SESSIONS is larger than 3

        scheduledSessionCreationService.ensureMinimumSessionAvailability();

        verify(scheduledSessionCreationService, never()).generateScheduledSessions();
    }

    @Test
    void ensureMinimumSessionAvailability_sessionsMissingForADay_triggersGeneration() {
        when(poolRepository.findByIsActiveTrue()).thenReturn(List.of(pool1));
        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);

        when(sessionRepository.countByPoolIdAndSessionDate(eq(pool1.getId()), eq(today))).thenReturn(5L);
        when(sessionRepository.countByPoolIdAndSessionDate(eq(pool1.getId()), eq(tomorrow))).thenReturn(0L);
        doNothing().when(scheduledSessionCreationService).generateScheduledSessions();

        scheduledSessionCreationService.ensureMinimumSessionAvailability();

        verify(scheduledSessionCreationService, times(1)).generateScheduledSessions();
    }

    @Test
    void ensureMinimumSessionAvailability_dayIsHoliday_skipsDayAndChecksNext() {
        LocalDate holidayDate = today.plusDays(1);
        LocalDate nonHolidayDateAfter = today.plusDays(2);

        when(poolRepository.findByIsActiveTrue()).thenReturn(List.of(pool1));
        when(holidayService.isHoliday(today)).thenReturn(false);
        when(holidayService.isHoliday(holidayDate)).thenReturn(true);
        when(holidayService.isHoliday(nonHolidayDateAfter)).thenReturn(false);


        when(sessionRepository.countByPoolIdAndSessionDate(eq(pool1.getId()), eq(today))).thenReturn(5L);
        when(sessionRepository.countByPoolIdAndSessionDate(eq(pool1.getId()), eq(nonHolidayDateAfter))).thenReturn(5L);

        scheduledSessionCreationService.ensureMinimumSessionAvailability();

        verify(sessionRepository, times(1)).countByPoolIdAndSessionDate(pool1.getId(), today);
        verify(sessionRepository, never()).countByPoolIdAndSessionDate(pool1.getId(), holidayDate);
        verify(sessionRepository, times(1)).countByPoolIdAndSessionDate(pool1.getId(), nonHolidayDateAfter);
        verify(scheduledSessionCreationService, never()).generateScheduledSessions();
    }


    @Test
    void generateScheduledSessions_noActivePools_doesNothing() {
        when(poolRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());
        scheduledSessionCreationService.generateScheduledSessions();
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void generateScheduledSessions_poolWithInvalidTimes_skipsPool() {
        when(poolRepository.findByIsActiveTrue()).thenReturn(List.of(pool2_invalidTimes));
        scheduledSessionCreationService.generateScheduledSessions();
        verify(sessionRepository, never()).save(any(Session.class));
        // Verify logging if a logger mock was injected
    }

    @Test
    void generateScheduledSessions_poolWithUnparseableTimes_skipsPool() {
        Pool poolUnparseable = new Pool();
        poolUnparseable.setId(3);
        poolUnparseable.setName("Unparseable Time Pool");
        poolUnparseable.setOpenAt("BAD_TIME");
        poolUnparseable.setCloseAt("10:00");
        poolUnparseable.setCapacity(10);
        poolUnparseable.setActive(true);

        when(poolRepository.findByIsActiveTrue()).thenReturn(List.of(poolUnparseable));
        scheduledSessionCreationService.generateScheduledSessions();
        verify(sessionRepository, never()).save(any(Session.class));
    }


    @Test
    void generateScheduledSessions_createsSessionsForOneDay_noExistingSessions_noHolidays_defaultEducation() {
        LocalDate windowEndDate = today.plusDays(13);

        when(poolRepository.findByIsActiveTrue()).thenReturn(List.of(pool1));
        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(educationTimeConfigRepository.findAllActive()).thenReturn(Collections.emptyList());
        when(sessionRepository.findByPoolIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
                eq(pool1.getId()), eq(today), eq(windowEndDate)))
                .thenReturn(Collections.emptyList()); // No existing sessions

        scheduledSessionCreationService.generateScheduledSessions();

        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        // Pool opens 09:00, closes 21:00. Sessions are 1 hour.
        // Expected sessions: 09-10, 10-11, ..., 20-21. Total 12 sessions per day.
        // For 14 days, 12 * 14 = 168 sessions
        verify(sessionRepository, times(12 * 14)).save(sessionCaptor.capture());

        List<Session> savedSessions = sessionCaptor.getAllValues();
        // Check one sample session for the first day (today)
        Optional<Session> firstDaySession9Am = savedSessions.stream()
                .filter(s -> s.getSessionDate().equals(today) && s.getStartTime().equals(LocalTime.of(9, 0)))
                .findFirst();
        assertTrue(firstDaySession9Am.isPresent());
        assertEquals(pool1.getId(), firstDaySession9Am.get().getPoolId());
        assertEquals(pool1.getCapacity(), firstDaySession9Am.get().getCapacity());
        assertTrue(firstDaySession9Am.get().isEducationSession()); // 9-10 is education by default

        Optional<Session> firstDaySession1Pm = savedSessions.stream()
                .filter(s -> s.getSessionDate().equals(today) && s.getStartTime().equals(LocalTime.of(13, 0)))
                .findFirst();
        assertTrue(firstDaySession1Pm.isPresent());
        assertFalse(firstDaySession1Pm.get().isEducationSession()); // 13-14 is not education by default
    }

    @Test
    void generateScheduledSessions_createsSessions_withCustomEducationConfig() {
        // Assume 'today' is a Monday for eduConfigMonday to apply
        LocalDate monday = today;
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            monday = monday.plusDays(1);
        }
        LocalDate windowEndDate = today.plusDays(13);


        when(poolRepository.findByIsActiveTrue()).thenReturn(List.of(pool1));
        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(educationTimeConfigRepository.findAllActive()).thenReturn(List.of(eduConfigMonday)); // Custom config
        when(sessionRepository.findByPoolIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
                eq(pool1.getId()), eq(today), eq(windowEndDate)))
                .thenReturn(Collections.emptyList());

        scheduledSessionCreationService.generateScheduledSessions();

        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository, times(12 * 14)).save(sessionCaptor.capture()); // 12 sessions/day * 14 days

        List<Session> savedSessions = sessionCaptor.getAllValues();

        // Check a session on Monday within custom education time (10:00-12:00)
        LocalDate finalMonday = monday; // effectively final for lambda
        Optional<Session> mondayEduSession = savedSessions.stream()
                .filter(s -> s.getSessionDate().equals(finalMonday) && s.getStartTime().equals(LocalTime.of(10, 0)))
                .findFirst();
        if (finalMonday.isAfter(windowEndDate)) { // If the first Monday is outside the 14-day window
            assertFalse(mondayEduSession.isPresent()); // It shouldn't be generated
        } else {
            assertTrue(mondayEduSession.isPresent(), "Education session on Monday 10 AM not found");
            assertTrue(mondayEduSession.get().isEducationSession());
        }


        // Check a session on Monday outside custom education time (e.g., 09:00)
        Optional<Session> mondayNonEduSession = savedSessions.stream()
                .filter(s -> s.getSessionDate().equals(finalMonday) && s.getStartTime().equals(LocalTime.of(9, 0)))
                .findFirst();
        if (finalMonday.isAfter(windowEndDate)) {
            assertFalse(mondayNonEduSession.isPresent());
        } else {
            assertTrue(mondayNonEduSession.isPresent(), "Non-education session on Monday 9 AM not found");
            assertFalse(mondayNonEduSession.get().isEducationSession());
        }
    }

    @Test
    void generateScheduledSessions_skipsHoliday() {
        LocalDate holiday = today.plusDays(1);

        when(poolRepository.findByIsActiveTrue()).thenReturn(List.of(pool1));
        when(holidayService.isHoliday(eq(holiday))).thenReturn(true);
        when(holidayService.isHoliday(argThat(date -> !date.equals(holiday)))).thenReturn(false); // Other days not holiday
        when(educationTimeConfigRepository.findAllActive()).thenReturn(Collections.emptyList());
        when(sessionRepository.findByPoolIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
                anyInt(), any(), any())).thenReturn(Collections.emptyList());


        scheduledSessionCreationService.generateScheduledSessions();

        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        // 12 sessions per day * 13 non-holiday days = 156 sessions
        verify(sessionRepository, times(12 * 13)).save(sessionCaptor.capture());
        List<Session> savedSessions = sessionCaptor.getAllValues();
        assertTrue(savedSessions.stream().noneMatch(s -> s.getSessionDate().equals(holiday)));
    }

    @Test
    void generateScheduledSessions_doesNotRecreateExistingSessions() {
        LocalDate windowEndDate = today.plusDays(13);
        // Simulate an existing session for today at 10:00
        Session existingSession = new Session();
        existingSession.setPoolId(pool1.getId());
        existingSession.setSessionDate(today);
        existingSession.setStartTime(LocalTime.of(10, 0));
        existingSession.setEndTime(LocalTime.of(11,0));


        when(poolRepository.findByIsActiveTrue()).thenReturn(List.of(pool1));
        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(false);
        when(educationTimeConfigRepository.findAllActive()).thenReturn(Collections.emptyList());
        when(sessionRepository.findByPoolIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
                eq(pool1.getId()), eq(today), eq(windowEndDate)))
                .thenReturn(List.of(existingSession)); // Return the existing session

        scheduledSessionCreationService.generateScheduledSessions();

        // Total possible slots: 12 sessions/day * 14 days = 168
        // One session already exists, so 168 - 1 = 167 new sessions should be created.
        verify(sessionRepository, times(168 - 1)).save(any(Session.class));
    }
}