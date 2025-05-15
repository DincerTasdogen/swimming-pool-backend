package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.EducationTimeConfig;
import com.sp.SwimmingPool.model.entity.Pool;
import com.sp.SwimmingPool.model.entity.Session;
import com.sp.SwimmingPool.repos.EducationTimeConfigRepository;
import com.sp.SwimmingPool.repos.PoolRepository;
import com.sp.SwimmingPool.repos.SessionRepository;
import java.time.DayOfWeek; // Import DayOfWeek
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledSessionCreationService implements SchedulingConfigurer {

    private static final int MINIMUM_DAYS_OF_SESSIONS = 3;

    private final PoolRepository poolRepository;
    private final SessionRepository sessionRepository;
    private final HolidayService holidayService;
    private final EducationTimeConfigRepository educationTimeConfigRepository;
    private final Random random = new Random();

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
                this::generateScheduledSessions,
                context -> {
                    int hour = (random.nextInt(6) + 23) % 24;
                    int minute = random.nextInt(60);
                    String cronExpression = String.format(
                            "0 %d %d * * ?",
                            minute,
                            hour
                    );
                    log.info(
                            "Next full session generation scheduled with cron expression: {}",
                            cronExpression
                    );
                    CronTrigger trigger = new CronTrigger(cronExpression);
                    return trigger.nextExecution(context);
                }
        );

        taskRegistrar.addCronTask(
                this::ensureMinimumSessionAvailability,
                "0 0 */12 * * ?"
        );
    }


    @Transactional
    public void ensureMinimumSessionAvailability() {
        log.info(
                "Checking minimum session availability at {}",
                LocalDateTime.now()
        );
        LocalDate today = LocalDate.now();
        LocalDate minimumEndDate = today.plusDays(MINIMUM_DAYS_OF_SESSIONS - 1);
        List<Pool> activePools = poolRepository.findByIsActiveTrue();
        if (activePools.isEmpty()) {
            log.info(
                    "No active pools found. Minimum session check completed."
            );
            return;
        }
        boolean needsGeneration = false;
        for (Pool pool : activePools) {
            for (
                    LocalDate date = today;
                    !date.isAfter(minimumEndDate);
                    date = date.plusDays(1)
            ) {
                if (holidayService.isHoliday(date)) {
                    continue;
                }
                long sessionCount = sessionRepository.countByPoolIdAndSessionDate(
                        pool.getId(),
                        date
                );
                if (sessionCount == 0) {
                    log.info(
                            "No sessions found for pool ID {} on date {}. Triggering generation.",
                            pool.getId(),
                            date
                    );
                    needsGeneration = true;
                    break;
                }
            }
            if (needsGeneration) {
                break;
            }
        }
        if (needsGeneration) {
            log.info(
                    "Insufficient sessions found. Triggering full session generation."
            );
            generateScheduledSessions();
        } else {
            log.info(
                    "Minimum session availability confirmed. No action needed."
            );
        }
    }

    @Transactional
    public void generateScheduledSessions() {
        log.info(
                "Scheduled session generation task started at {}",
                LocalDateTime.now()
        );
        LocalDate today = LocalDate.now();
        LocalDate windowEndDate = today.plusDays(13);
        List<Pool> activePools = poolRepository.findByIsActiveTrue();
        if (activePools.isEmpty()) {
            log.info("No active pools found. Session generation task finished.");
            return;
        }
        log.info("Found {} active pools to process.", activePools.size());
        int totalSessionsCreated = 0;
        int poolsProcessed = 0;

        for (Pool pool : activePools) {
            poolsProcessed++;
            log.debug(
                    "Processing pool ID: {}, Name: {}",
                    pool.getId(),
                    pool.getName()
            );
            try {
                LocalTime openAt;
                LocalTime closeAt;
                try {
                    openAt = LocalTime.parse(pool.getOpenAt());
                    closeAt = LocalTime.parse(pool.getCloseAt());
                } catch (Exception e) {
                    log.error(
                            "Could not parse openAt/closeAt for pool ID {}: {}/{}. Skipping pool. Error: {}",
                            pool.getId(),
                            pool.getOpenAt(),
                            pool.getCloseAt(),
                            e.getMessage()
                    );
                    continue;
                }
                if (openAt.isAfter(closeAt) || openAt.equals(closeAt)) {
                    log.warn(
                            "Pool ID {} has invalid open/close times (open: {}, close: {}). Skipping pool.",
                            pool.getId(),
                            openAt,
                            closeAt
                    );
                    continue;
                }

                for (
                        LocalDate currentDate = today;
                        !currentDate.isAfter(windowEndDate);
                        currentDate = currentDate.plusDays(1)
                ) {
                    if (holidayService.isHoliday(currentDate)) {
                        log.debug(
                                "Skipping date {} for pool {} as it is a holiday.",
                                currentDate,
                                pool.getName()
                        );
                        continue;
                    }
                    LocalTime currentSessionStartTime = openAt;
                    while (currentSessionStartTime.isBefore(closeAt)) {
                        LocalTime currentSessionEndTime =
                                currentSessionStartTime.plusHours(1);
                        if (currentSessionEndTime.isAfter(closeAt)) {
                            log.trace(
                                    "Session at {} for pool {} would end after closing time {}. Stopping for this day.",
                                    currentSessionStartTime,
                                    pool.getName(),
                                    closeAt
                            );
                            break;
                        }
                        if (
                                !sessionRepository.existsByPoolIdAndSessionDateAndStartTime(
                                        pool.getId(),
                                        currentDate,
                                        currentSessionStartTime
                                )
                        ) {
                            Session newSession = new Session();
                            newSession.setPoolId(pool.getId());
                            newSession.setSessionDate(currentDate);
                            newSession.setStartTime(currentSessionStartTime);
                            newSession.setEndTime(currentSessionEndTime);
                            newSession.setCapacity(pool.getCapacity());
                            newSession.setCurrentBookings(0);
                            newSession.setCreatedAt(LocalDateTime.now());

                            boolean isEducation = isEducationTime(
                                    currentSessionStartTime,
                                    currentDate
                            );
                            newSession.setEducationSession(isEducation);

                            sessionRepository.save(newSession);
                            totalSessionsCreated++;
                            log.trace(
                                    "Created session for pool ID {}, Date: {}, StartTime: {}, Education: {}",
                                    pool.getId(),
                                    currentDate,
                                    currentSessionStartTime,
                                    newSession.isEducationSession()
                            );
                        } else {
                            log.trace(
                                    "Session already exists for pool ID {}, Date: {}, StartTime: {}",
                                    pool.getId(),
                                    currentDate,
                                    currentSessionStartTime
                            );
                        }
                        currentSessionStartTime =
                                currentSessionStartTime.plusHours(1);
                    }
                }
            } catch (Exception e) {
                log.error(
                        "An unexpected error occurred while processing pool ID {}: {}",
                        pool.getId(),
                        e.getMessage(),
                        e
                );
            }
        }
        log.info(
                "Scheduled session generation task finished. Processed {} pools. Total new sessions created: {}.",
                poolsProcessed,
                totalSessionsCreated
        );
    }

    /**
     * Determines if a given time on a specific date is configured as an education session time.
     * Uses coach-defined configurations from the database, with fallback to default (9-12 on any day).
     *
     * @param time The time to check
     * @param date The date to check (for DayOfWeek)
     * @return true if the time is an education session time
     */
    private boolean isEducationTime(LocalTime time, LocalDate date) { // MODIFIED SIGNATURE
        DayOfWeek currentDayOfWeek = date.getDayOfWeek();
        List<EducationTimeConfig> configs =
                educationTimeConfigRepository.findAllActive();

        if (configs.isEmpty()) {
            // Fallback to default (9:00-12:00) if no active configurations
            // This default applies to any day of the week.
            return time.getHour() >= 9 && time.getHour() < 12;
        }

        for (EducationTimeConfig config : configs) {
            // Check if the config applies to the current day of the week
            if (
                    config.getApplicableDays() != null &&
                            config.getApplicableDays().contains(currentDayOfWeek)
            ) {
                LocalTime configStartTime = config.getStartTime();
                LocalTime configEndTime = config.getEndTime();

                if (
                        !time.isBefore(configStartTime) && time.isBefore(configEndTime)
                ) {
                    return true;
                }
            }
        }
        return false;
    }

    @Transactional
    public boolean updateSessionStatus(int sessionId, boolean isEducationSession) {
        Session session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            log.warn(
                    "Session with ID {} not found for status update",
                    sessionId
            );
            return false;
        }
        session.setEducationSession(isEducationSession);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
        log.info(
                "Session {} status updated to education={}",
                sessionId,
                isEducationSession
        );
        return true;
    }
}
