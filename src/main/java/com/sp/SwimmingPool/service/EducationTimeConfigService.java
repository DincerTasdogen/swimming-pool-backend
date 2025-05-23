package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.EducationTimeConfig;
import com.sp.SwimmingPool.repos.EducationTimeConfigRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set; // Import Set
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to manage education time configurations.
 * Only COACH users can configure education times.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EducationTimeConfigService {

    private final EducationTimeConfigRepository educationTimeConfigRepository;

    /**
     * Creates a new education time configuration.
     *
     * @param startTime The start time for the education period
     * @param endTime The end time for the education period
     * @param applicableDays The set of days of the week this config applies to
     * @param description Optional description
     * @return The created configuration entity if successful, null otherwise
     */
    @Transactional
    public EducationTimeConfig createEducationTimeConfig(
            LocalTime startTime,
            LocalTime endTime,
            Set<DayOfWeek> applicableDays, // Added
            String description
    ) {
        // Validate time range
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            log.warn(
                    "Attempt to create an invalid education time range: {} to {}",
                    startTime,
                    endTime
            );
            return null;
        }

        if (applicableDays == null || applicableDays.isEmpty()) {
            log.warn(
                    "Attempt to create an education time config with no applicable days."
            );
            return null;
        }

        // Create and save the configuration
        EducationTimeConfig config = new EducationTimeConfig();
        config.setStartTime(startTime);
        config.setEndTime(endTime);
        config.setApplicableDays(applicableDays); // Set days
        config.setDescription(description);
        config.setActive(true);
        // createdAt and updatedAt will be set by @PrePersist

        EducationTimeConfig savedConfig =
                educationTimeConfigRepository.save(config);
        log.info(
                "Education time config created: {} to {} on days: {}",
                startTime,
                endTime,
                applicableDays
        );

        return savedConfig;
    }

    /**
     * Updates an existing education time configuration.
     *
     * @param configId The ID of the configuration to update
     * @param startTime The new start time (or null to keep existing)
     * @param endTime The new end time (or null to keep existing)
     * @param applicableDays The new set of applicable days (or null to keep existing)
     * @param description The new description (or null to keep existing)
     * @param active The new active status (or null to keep existing)
     * @return The updated configuration entity if successful, null otherwise
     */
    @Transactional
    public EducationTimeConfig updateEducationTimeConfig(
            Long configId,
            LocalTime startTime,
            LocalTime endTime,
            Set<DayOfWeek> applicableDays, // Added
            String description,
            Boolean active
    ) {
        EducationTimeConfig config = educationTimeConfigRepository
                .findById(configId)
                .orElse(null);
        if (config == null) {
            log.warn(
                    "Attempt to update a non-existent education time config: {}",
                    configId
            );
            return null;
        }

        boolean changed = false;

        LocalTime effectiveStartTime = config.getStartTime();
        LocalTime effectiveEndTime = config.getEndTime();

        if (startTime != null) {
            effectiveStartTime = startTime;
            changed = true;
        }
        if (endTime != null) {
            effectiveEndTime = endTime;
            changed = true;
        }

        if (
                (startTime != null || endTime != null) &&
                        (
                                effectiveStartTime.isAfter(effectiveEndTime) ||
                                        effectiveStartTime.equals(effectiveEndTime)
                        )
        ) {
            log.warn(
                    "Attempt to update to an invalid education time range: {} to {}",
                    effectiveStartTime,
                    effectiveEndTime
            );
            return null;
        }
        if (startTime != null) config.setStartTime(startTime);
        if (endTime != null) config.setEndTime(endTime);

        if (applicableDays != null && !applicableDays.isEmpty()) {
            config.setApplicableDays(applicableDays); // Set days
            changed = true;
        } else if (applicableDays != null && applicableDays.isEmpty()) {
            log.warn(
                    "Attempt to update education time config {} with empty applicable days. Days not changed.",
                    configId
            );
        }

        if (description != null) {
            config.setDescription(description);
            changed = true;
        }

        if (active != null) {
            config.setActive(active);
            changed = true;
        }

        if (changed) {
            // updatedAt will be set by @PreUpdate
            EducationTimeConfig savedConfig =
                    educationTimeConfigRepository.save(config);
            log.info(
                    "Education time config updated: ID {}, Days: {}",
                    configId,
                    savedConfig.getApplicableDays()
            );
            return savedConfig;
        } else {
            log.info(
                    "No changes made to education time config: ID {}",
                    configId
            );
            return config;
        }
    }

    @Transactional
    public boolean deleteEducationTimeConfig(Long id) {
        EducationTimeConfig config = educationTimeConfigRepository.findById(id).orElse(null);
        if (config == null) return false;
        educationTimeConfigRepository.delete(config);
        return true;
    }

    public List<EducationTimeConfig> getAllActiveConfigs() {
        return educationTimeConfigRepository.findAllActive();
    }

    public List<EducationTimeConfig> getAllConfigs() {
        return educationTimeConfigRepository.findAll();
    }
}
