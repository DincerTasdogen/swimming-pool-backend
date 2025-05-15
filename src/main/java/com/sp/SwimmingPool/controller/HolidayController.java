package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.ApiResponse;
import com.sp.SwimmingPool.dto.HolidayDTO;
import com.sp.SwimmingPool.model.entity.Holiday;
import com.sp.SwimmingPool.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for managing holidays.
 */
@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
@Slf4j
public class HolidayController {

    private final HolidayService holidayService;

    /**
     * Get all holidays (both fixed and custom) for a date range.
     *
     * @param startDate The start date of the range (inclusive)
     * @param endDate The end date of the range (inclusive)
     * @return List of holiday dates in the specified range
     */
    @GetMapping
    public ResponseEntity<List<LocalDate>> getHolidaysInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting holidays between {} and {}", startDate, endDate);
        List<LocalDate> holidays = holidayService.getHolidayDatesInRange(startDate, endDate);
        return ResponseEntity.ok(holidays);
    }

    /**
     * Get all fixed national holidays.
     *
     * @return Set of fixed national holidays as month-day strings
     */
    @GetMapping("/fixed")
    public ResponseEntity<List<String>> getFixedHolidays() {
        log.info("Getting fixed national holidays");
        Set<MonthDay> fixedHolidays = holidayService.getFixedNationalHolidays();

        List<String> formattedHolidays = fixedHolidays.stream()
                .map(md -> md.getMonth().toString() + " " + md.getDayOfMonth())
                .sorted()
                .collect(Collectors.toList());

        return ResponseEntity.ok(formattedHolidays);
    }

    /**
     * Get all custom holidays for the current year.
     *
     * @return List of custom holidays
     */
    @GetMapping("/custom")
    public ResponseEntity<List<HolidayDTO>> getCustomHolidays() {
        log.info("Getting custom holidays for current year");
        List<Holiday> holidays = holidayService.getCustomHolidaysForCurrentYear();

        List<HolidayDTO> holidayDTOs = holidays.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(holidayDTOs);
    }

    /**
     * Check if a specific date is a holiday.
     *
     * @param date The date to check
     * @return True if the date is a holiday, false otherwise
     */
    @GetMapping("/check")
    public ResponseEntity<Boolean> isHoliday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Checking if {} is a holiday", date);
        boolean isHoliday = holidayService.isHoliday(date);
        return ResponseEntity.ok(isHoliday);
    }

    /**
     * Add a new custom holiday.
     * Restricted to ADMIN users only.
     *
     * @param holidayDTO The holiday to add
     * @return The created holiday
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addHoliday(
            @Valid @RequestBody HolidayDTO holidayDTO) {

        log.info("Adding holiday for date: {}", holidayDTO.getDate());
        Holiday holiday = holidayService.addCustomHoliday(
                holidayDTO.getDate(),
                holidayDTO.getDescription()
        );

        if (holiday == null) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Could not add holiday. It may be a duplicate or invalid date.")
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(holiday));
    }

    /**
     * Delete a custom holiday.
     * Restricted to ADMIN users only.
     *
     * @param date The date of the holiday to delete
     * @return Response indicating success or failure
     */
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteHoliday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Deleting holiday for date: {}", date);
        boolean success = holidayService.removeCustomHoliday(date);

        if (success) {
            return ResponseEntity.ok(new ApiResponse(true, "Holiday successfully deleted"));
        } else {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Could not delete holiday. It may be a fixed holiday or not exist.")
            );
        }
    }

    /**
     * Convert a Holiday entity to a HolidayDTO.
     *
     * @param holiday The Holiday entity
     * @return The corresponding HolidayDTO
     */
    private HolidayDTO convertToDTO(Holiday holiday) {
        return new HolidayDTO(
                holiday.getId(),
                holiday.getDate(),
                holiday.getDescription()
        );
    }
}
