package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.Holiday;
import com.sp.SwimmingPool.repos.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.MonthDay;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayService {
    private static final Set<MonthDay> FIXED_NATIONAL_HOLIDAYS = new HashSet<>();

    static {
        // Fixed national holidays for Turkey
        FIXED_NATIONAL_HOLIDAYS.add(MonthDay.of(Month.JANUARY, 1));   // New Year's Day
        FIXED_NATIONAL_HOLIDAYS.add(MonthDay.of(Month.APRIL, 23));    // National Sovereignty and Children's Day
        FIXED_NATIONAL_HOLIDAYS.add(MonthDay.of(Month.MAY, 1));       // Labor and Solidarity Day
        FIXED_NATIONAL_HOLIDAYS.add(MonthDay.of(Month.MAY, 19));      // Commemoration of Atatürk, Youth and Sports Day
        FIXED_NATIONAL_HOLIDAYS.add(MonthDay.of(Month.JULY, 15));     // Democracy and National Unity Day
        FIXED_NATIONAL_HOLIDAYS.add(MonthDay.of(Month.AUGUST, 30));   // Victory Day
        FIXED_NATIONAL_HOLIDAYS.add(MonthDay.of(Month.OCTOBER, 29));  // Republic Day
    }

    private final HolidayRepository holidayRepository;

    public List<LocalDate> getHolidayDatesInRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return Collections.emptyList();
        }

        Set<LocalDate> holidays = new HashSet<>();

        // Add fixed national holidays
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            MonthDay currentMonthDay = MonthDay.from(date);
            if (FIXED_NATIONAL_HOLIDAYS.contains(currentMonthDay)) {
                holidays.add(date);
            }
        }

        // Add custom holidays from DB
        List<Holiday> customHolidays = holidayRepository.findByDateBetween(startDate, endDate);
        customHolidays.forEach(holiday -> holidays.add(holiday.getDate()));

        return holidays.stream().sorted().collect(Collectors.toList());
    }

    /**
     * Checks if a specific date is a holiday (fixed or custom).
     *
     * @param date The date to check.
     * @return true if the date is a holiday, false otherwise.
     */
    public boolean isHoliday(LocalDate date) {
        if (date == null) {
            return false;
        }

        // Check fixed national holidays
        if (FIXED_NATIONAL_HOLIDAYS.contains(MonthDay.from(date))) {
            return true;
        }

        // Check custom holidays
        return holidayRepository.existsByDate(date);
    }

    /**
     * Adds a custom holiday.
     * Only ADMIN users can add custom holidays.
     *
     * @param date The holiday date
     * @param description The holiday description
     * @return The created Holiday entity if successful, null otherwise
     */
    @Transactional
    public Holiday addCustomHoliday(LocalDate date, String description) {
        // Check if date is already a fixed holiday
        if (FIXED_NATIONAL_HOLIDAYS.contains(MonthDay.from(date))) {
            log.warn("Attempt to add a custom holiday on a fixed national holiday: {}", date);
            return null;
        }

        // Check if date is already a custom holiday
        if (holidayRepository.existsByDate(date)) {
            log.warn("Attempt to add a duplicate custom holiday for date: {}", date);
            return null;
        }

        // Create and save the custom holiday
        Holiday holiday = new Holiday();
        holiday.setDate(date);
        holiday.setDescription(description);
        holiday.setCreatedAt(LocalDateTime.now());

        Holiday savedHoliday = holidayRepository.save(holiday);
        log.info("Custom holiday added for date: {} ", date);

        return savedHoliday;
    }

    /**
     * Removes a custom holiday.
     * Only ADMIN users can remove custom holidays.
     *
     * @param date The holiday date to remove
     * @return true if successfully removed, false otherwise
     */
    @Transactional
    public boolean removeCustomHoliday(LocalDate date) {

        // Fixed holidays cannot be removed
        if (FIXED_NATIONAL_HOLIDAYS.contains(MonthDay.from(date))) {
            log.warn("Attempt to remove a fixed national holiday: {}", date);
            return false;
        }

        // Find and remove the custom holiday
        Holiday holiday = holidayRepository.findByDate(date).orElse(null);
        if (holiday == null) {
            log.warn("Attempt to remove a non-existent custom holiday for date: {}", date);
            return false;
        }

        holidayRepository.delete(holiday);
        log.info("Custom holiday removed for date: {}", date);

        return true;
    }

    /**
     * Lists all fixed national holidays.
     *
     * @return Set of MonthDay objects representing fixed national holidays
     */
    public Set<MonthDay> getFixedNationalHolidays() {
        return Collections.unmodifiableSet(FIXED_NATIONAL_HOLIDAYS);
    }

    /**
     * Lists all custom holidays for the current year.
     *
     * @return List of Holiday entities
     */
    public List<Holiday> getCustomHolidaysForCurrentYear() {
        LocalDate startOfYear = LocalDate.now().withDayOfYear(1);
        LocalDate endOfYear = LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear());
        return holidayRepository.findByDateBetween(startOfYear, endOfYear);
    }
}