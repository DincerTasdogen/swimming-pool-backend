package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.Holiday;
import com.sp.SwimmingPool.repos.HolidayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private HolidayService holidayService;

    private LocalDate newYearsDay;
    private LocalDate customHolidayDate;
    private Holiday customHolidayEntity;

    @BeforeEach
    void setUp() {
        newYearsDay = LocalDate.of(2025, Month.JANUARY, 1);
        customHolidayDate = LocalDate.of(2025, Month.JUNE, 10);

        customHolidayEntity = new Holiday();
        customHolidayEntity.setId(1L);
        customHolidayEntity.setDate(customHolidayDate);
        customHolidayEntity.setDescription("Custom Test Holiday");
    }

    @Test
    void getHolidayDatesInRange_invalidRange_shouldReturnEmptyList() {
        assertTrue(holidayService.getHolidayDatesInRange(null, LocalDate.now()).isEmpty());
        assertTrue(holidayService.getHolidayDatesInRange(LocalDate.now(), null).isEmpty());
        assertTrue(holidayService.getHolidayDatesInRange(LocalDate.now().plusDays(1), LocalDate.now()).isEmpty());
    }

    @Test
    void getHolidayDatesInRange_rangeWithOnlyFixedHoliday() {
        LocalDate startDate = LocalDate.of(2025, Month.JANUARY, 1);
        LocalDate endDate = LocalDate.of(2025, Month.JANUARY, 2);

        when(holidayRepository.findByDateBetween(startDate, endDate)).thenReturn(Collections.emptyList());

        List<LocalDate> holidays = holidayService.getHolidayDatesInRange(startDate, endDate);

        assertEquals(1, holidays.size());
        assertTrue(holidays.contains(newYearsDay));
    }

    @Test
    void getHolidayDatesInRange_rangeWithOnlyCustomHoliday() {
        LocalDate startDate = LocalDate.of(2025, Month.JUNE, 10);
        LocalDate endDate = LocalDate.of(2025, Month.JUNE, 11);

        when(holidayRepository.findByDateBetween(startDate, endDate)).thenReturn(List.of(customHolidayEntity));

        List<LocalDate> holidays = holidayService.getHolidayDatesInRange(startDate, endDate);

        assertEquals(1, holidays.size());
        assertTrue(holidays.contains(customHolidayDate));
    }

    @Test
    void getHolidayDatesInRange_rangeWithFixedAndCustomHolidays_shouldBeSorted() {
        LocalDate startDate = LocalDate.of(2025, Month.JANUARY, 1);
        LocalDate endDate = LocalDate.of(2025, Month.JUNE, 10);

        Holiday anotherCustomHoliday = new Holiday();
        anotherCustomHoliday.setDate(LocalDate.of(2025, Month.MARCH, 3));
        anotherCustomHoliday.setDescription("Another Custom");


        when(holidayRepository.findByDateBetween(startDate, endDate))
                .thenReturn(List.of(customHolidayEntity, anotherCustomHoliday));

        List<LocalDate> holidays = holidayService.getHolidayDatesInRange(startDate, endDate);

        // Expected: Jan 1, Mar 3, Apr 23, May 1, May 19, Jun 10, Jul 15, Aug 30, Oct 29
        // Within range: Jan 1, Mar 3 (custom), Apr 23, May 1, May 19, Jun 10 (custom)
        // The fixed holidays are added based on iterating through the date range.
        // The custom holidays are fetched from the repo.
        // The service combines and sorts them.

        assertTrue(holidays.contains(newYearsDay)); // Fixed
        assertTrue(holidays.contains(customHolidayDate)); // Custom
        assertTrue(holidays.contains(LocalDate.of(2025, Month.APRIL, 23))); // Fixed
        assertTrue(holidays.contains(LocalDate.of(2025, Month.MARCH, 3))); // Custom

        // Check sorting
        for (int i = 0; i < holidays.size() - 1; i++) {
            assertTrue(holidays.get(i).isBefore(holidays.get(i + 1)));
        }
    }

    @Test
    void getHolidayDatesInRange_noHolidaysInRange() {
        LocalDate startDate = LocalDate.of(2025, Month.FEBRUARY, 1);
        LocalDate endDate = LocalDate.of(2025, Month.FEBRUARY, 5);
        when(holidayRepository.findByDateBetween(startDate, endDate)).thenReturn(Collections.emptyList());
        List<LocalDate> holidays = holidayService.getHolidayDatesInRange(startDate, endDate);
        assertTrue(holidays.isEmpty());
    }

    @Test
    void isHoliday_nullDate_shouldReturnFalse() {
        assertFalse(holidayService.isHoliday(null));
    }

    @Test
    void isHoliday_fixedHoliday_shouldReturnTrue() {
        assertTrue(holidayService.isHoliday(newYearsDay));
        // No need to mock repository as fixed holidays are checked first
    }

    @Test
    void isHoliday_customHoliday_shouldReturnTrue() {
        when(holidayRepository.existsByDate(customHolidayDate)).thenReturn(true);
        assertTrue(holidayService.isHoliday(customHolidayDate));
    }

    @Test
    void isHoliday_notAHoliday_shouldReturnFalse() {
        LocalDate notHoliday = LocalDate.of(2025, Month.FEBRUARY, 15);
        when(holidayRepository.existsByDate(notHoliday)).thenReturn(false);
        assertFalse(holidayService.isHoliday(notHoliday));
    }

    @Test
    void addCustomHoliday_onFixedHoliday_shouldReturnNull() {
        assertNull(holidayService.addCustomHoliday(newYearsDay, "Attempt to overwrite New Year"));
        verify(holidayRepository, never()).save(any(Holiday.class));
    }

    @Test
    void addCustomHoliday_duplicateCustomHoliday_shouldReturnNull() {
        when(holidayRepository.existsByDate(customHolidayDate)).thenReturn(true);
        assertNull(holidayService.addCustomHoliday(customHolidayDate, "Duplicate Custom"));
        verify(holidayRepository, never()).save(any(Holiday.class));
    }

    @Test
    void addCustomHoliday_success() {
        LocalDate newCustomDate = LocalDate.of(2025, Month.DECEMBER, 20);
        Holiday newHoliday = new Holiday();
        newHoliday.setDate(newCustomDate);
        newHoliday.setDescription("New Custom Holiday");

        when(holidayRepository.existsByDate(newCustomDate)).thenReturn(false);
        when(holidayRepository.save(any(Holiday.class))).thenAnswer(invocation -> {
            Holiday h = invocation.getArgument(0);
            h.setId(2L); // Simulate save
            return h;
        });

        Holiday result = holidayService.addCustomHoliday(newCustomDate, "New Custom Holiday");

        assertNotNull(result);
        assertEquals(newCustomDate, result.getDate());
        assertEquals("New Custom Holiday", result.getDescription());
        verify(holidayRepository).save(any(Holiday.class));
    }

    @Test
    void removeCustomHoliday_fixedHoliday_shouldReturnFalse() {
        assertFalse(holidayService.removeCustomHoliday(newYearsDay));
        verify(holidayRepository, never()).delete(any(Holiday.class));
    }

    @Test
    void removeCustomHoliday_nonExistentCustomHoliday_shouldReturnFalse() {
        LocalDate nonExistentDate = LocalDate.of(2025, Month.NOVEMBER, 5);
        when(holidayRepository.findByDate(nonExistentDate)).thenReturn(Optional.empty());
        assertFalse(holidayService.removeCustomHoliday(nonExistentDate));
        verify(holidayRepository, never()).delete(any(Holiday.class));
    }

    @Test
    void removeCustomHoliday_success() {
        when(holidayRepository.findByDate(customHolidayDate)).thenReturn(Optional.of(customHolidayEntity));
        doNothing().when(holidayRepository).delete(customHolidayEntity);

        assertTrue(holidayService.removeCustomHoliday(customHolidayDate));
        verify(holidayRepository).delete(customHolidayEntity);
    }

    @Test
    void getFixedNationalHolidays_shouldReturnCorrectSet() {
        Set<MonthDay> fixedHolidays = holidayService.getFixedNationalHolidays();
        assertNotNull(fixedHolidays);
        assertEquals(7, fixedHolidays.size()); // As per static block
        assertTrue(fixedHolidays.contains(MonthDay.of(Month.JANUARY, 1)));
        // Attempting to modify should throw exception
        assertThrows(UnsupportedOperationException.class, () -> fixedHolidays.add(MonthDay.of(Month.DECEMBER, 25)));
    }

    @Test
    void getCustomHolidaysForCurrentYear() {
        LocalDate startOfYear = LocalDate.now().withDayOfYear(1);
        LocalDate endOfYear = LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear());

        List<Holiday> expectedHolidays = List.of(customHolidayEntity);
        when(holidayRepository.findByDateBetween(startOfYear, endOfYear)).thenReturn(expectedHolidays);

        List<Holiday> actualHolidays = holidayService.getCustomHolidaysForCurrentYear();

        assertEquals(expectedHolidays, actualHolidays);
        verify(holidayRepository).findByDateBetween(startOfYear, endOfYear);
    }
}