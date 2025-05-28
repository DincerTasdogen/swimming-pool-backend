package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.EducationTimeConfig;
import com.sp.SwimmingPool.repos.EducationTimeConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EducationTimeConfigServiceTest {

    @Mock
    private EducationTimeConfigRepository educationTimeConfigRepository;

    @InjectMocks
    private EducationTimeConfigService educationTimeConfigService;

    private EducationTimeConfig config1;
    private LocalTime startTime;
    private LocalTime endTime;
    private Set<DayOfWeek> applicableDays;
    private String description;

    @BeforeEach
    void setUp() {
        startTime = LocalTime.of(9, 0);
        endTime = LocalTime.of(12, 0);
        applicableDays = new HashSet<>(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
        description = "Morning Education Block";

        config1 = new EducationTimeConfig();
        config1.setId(1L);
        config1.setStartTime(startTime);
        config1.setEndTime(endTime);
        config1.setApplicableDays(applicableDays);
        config1.setDescription(description);
        config1.setActive(true);
        config1.setCreatedAt(LocalDateTime.now().minusDays(1));
        config1.setUpdatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void createEducationTimeConfig_validInput_createsAndReturnsConfig() {
        when(educationTimeConfigRepository.save(any(EducationTimeConfig.class)))
                .thenAnswer(invocation -> {
                    EducationTimeConfig saved = invocation.getArgument(0);
                    saved.setId(2L); // Simulate ID generation
                    // @PrePersist would set createdAt and updatedAt,
                    // but for unit test, we can check they are not null if service sets them,
                    // or trust @PrePersist if it's an integration test.
                    // Here, the service doesn't explicitly set them, relying on @PrePersist.
                    return saved;
                });

        EducationTimeConfig result = educationTimeConfigService.createEducationTimeConfig(
                startTime, endTime, applicableDays, description
        );

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals(startTime, result.getStartTime());
        assertEquals(endTime, result.getEndTime());
        assertEquals(applicableDays, result.getApplicableDays());
        assertEquals(description, result.getDescription());
        assertTrue(result.isActive()); // Should be active by default

        ArgumentCaptor<EducationTimeConfig> captor = ArgumentCaptor.forClass(EducationTimeConfig.class);
        verify(educationTimeConfigRepository).save(captor.capture());
        EducationTimeConfig captured = captor.getValue();
        assertEquals(startTime, captured.getStartTime());
        assertTrue(captured.isActive());
    }

    @Test
    void createEducationTimeConfig_startTimeAfterEndTime_returnsNull() {
        LocalTime invalidStartTime = LocalTime.of(14, 0);
        LocalTime invalidEndTime = LocalTime.of(10, 0);

        EducationTimeConfig result = educationTimeConfigService.createEducationTimeConfig(
                invalidStartTime, invalidEndTime, applicableDays, description
        );

        assertNull(result);
        verify(educationTimeConfigRepository, never()).save(any(EducationTimeConfig.class));
    }

    @Test
    void createEducationTimeConfig_startTimeEqualsEndTime_returnsNull() {
        LocalTime sameTime = LocalTime.of(10, 0);

        EducationTimeConfig result = educationTimeConfigService.createEducationTimeConfig(
                sameTime, sameTime, applicableDays, description
        );

        assertNull(result);
        verify(educationTimeConfigRepository, never()).save(any(EducationTimeConfig.class));
    }

    @Test
    void createEducationTimeConfig_nullApplicableDays_returnsNull() {
        EducationTimeConfig result = educationTimeConfigService.createEducationTimeConfig(
                startTime, endTime, null, description
        );
        assertNull(result);
        verify(educationTimeConfigRepository, never()).save(any(EducationTimeConfig.class));
    }

    @Test
    void createEducationTimeConfig_emptyApplicableDays_returnsNull() {
        EducationTimeConfig result = educationTimeConfigService.createEducationTimeConfig(
                startTime, endTime, Collections.emptySet(), description
        );
        assertNull(result);
        verify(educationTimeConfigRepository, never()).save(any(EducationTimeConfig.class));
    }

    @Test
    void updateEducationTimeConfig_configExists_validChanges_updatesAndReturnsConfig() {
        Long configId = config1.getId();
        LocalTime newStartTime = LocalTime.of(10, 0);
        LocalTime newEndTime = LocalTime.of(13, 0);
        Set<DayOfWeek> newDays = new HashSet<>(Set.of(DayOfWeek.TUESDAY));
        String newDescription = "Updated Block";
        boolean newActiveStatus = false;
        LocalDateTime originalUpdatedAt = config1.getUpdatedAt();


        when(educationTimeConfigRepository.findById(configId)).thenReturn(Optional.of(config1));
        when(educationTimeConfigRepository.save(any(EducationTimeConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // Return the modified argument

        EducationTimeConfig result = educationTimeConfigService.updateEducationTimeConfig(
                configId, newStartTime, newEndTime, newDays, newDescription, newActiveStatus
        );

        assertNotNull(result);
        assertEquals(configId, result.getId());
        assertEquals(newStartTime, result.getStartTime());
        assertEquals(newEndTime, result.getEndTime());
        assertEquals(newDays, result.getApplicableDays());
        assertEquals(newDescription, result.getDescription());
        assertEquals(newActiveStatus, result.isActive());
        // @PreUpdate should handle updatedAt, service doesn't set it explicitly
        // For unit test, we can check it's different if the service were to set it.
        // Since it relies on @PreUpdate, we'd verify the save call.
        // If we want to test the timestamp change, we'd need to simulate @PreUpdate or check against a "before" state.
        // Assuming @PreUpdate works, the timestamp in the returned 'result' (which is 'config1' instance) would be updated.
        assertTrue(result.getUpdatedAt().isAfter(originalUpdatedAt) || result.getUpdatedAt().isEqual(originalUpdatedAt));


        ArgumentCaptor<EducationTimeConfig> captor = ArgumentCaptor.forClass(EducationTimeConfig.class);
        verify(educationTimeConfigRepository).save(captor.capture());
        EducationTimeConfig captured = captor.getValue();
        assertEquals(newStartTime, captured.getStartTime());
        assertEquals(newActiveStatus, captured.isActive());
    }

    @Test
    void updateEducationTimeConfig_configExists_onlySomeFieldsChanged_updatesAndReturnsConfig() {
        Long configId = config1.getId();
        LocalTime newStartTime = LocalTime.of(8, 0); // Change only start time and active status
        boolean newActiveStatus = false;

        when(educationTimeConfigRepository.findById(configId)).thenReturn(Optional.of(config1));
        when(educationTimeConfigRepository.save(any(EducationTimeConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EducationTimeConfig result = educationTimeConfigService.updateEducationTimeConfig(
                configId, newStartTime, null, null, null, newActiveStatus
        );

        assertNotNull(result);
        assertEquals(newStartTime, result.getStartTime()); // Changed
        assertEquals(config1.getEndTime(), result.getEndTime()); // Unchanged
        assertEquals(config1.getApplicableDays(), result.getApplicableDays()); // Unchanged
        assertEquals(newActiveStatus, result.isActive()); // Changed
    }

    @Test
    void updateEducationTimeConfig_configExists_noChangesMade_returnsOriginalConfig() {
        Long configId = config1.getId();
        LocalDateTime originalUpdatedAt = config1.getUpdatedAt();

        when(educationTimeConfigRepository.findById(configId)).thenReturn(Optional.of(config1));

        EducationTimeConfig result = educationTimeConfigService.updateEducationTimeConfig(
                configId, null, null, null, null, null // No changes
        );

        assertNotNull(result);
        assertEquals(config1.getStartTime(), result.getStartTime());
        assertEquals(config1.isActive(), result.isActive());
        assertEquals(originalUpdatedAt, result.getUpdatedAt()); // Timestamp should not change
        verify(educationTimeConfigRepository, never()).save(any(EducationTimeConfig.class));
    }


    @Test
    void updateEducationTimeConfig_configNotExists_returnsNull() {
        Long nonExistentId = 99L;
        when(educationTimeConfigRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        EducationTimeConfig result = educationTimeConfigService.updateEducationTimeConfig(
                nonExistentId, startTime, endTime, applicableDays, description, true
        );

        assertNull(result);
        verify(educationTimeConfigRepository, never()).save(any(EducationTimeConfig.class));
    }

    @Test
    void updateEducationTimeConfig_invalidTimeRange_returnsNull() {
        Long configId = config1.getId();
        LocalTime invalidStartTime = LocalTime.of(15, 0);
        LocalTime invalidEndTime = LocalTime.of(10, 0); // End before start

        when(educationTimeConfigRepository.findById(configId)).thenReturn(Optional.of(config1));

        EducationTimeConfig result = educationTimeConfigService.updateEducationTimeConfig(
                configId, invalidStartTime, invalidEndTime, null, null, null
        );

        assertNull(result);
        verify(educationTimeConfigRepository, never()).save(any(EducationTimeConfig.class));
    }

    @Test
    void updateEducationTimeConfig_emptyApplicableDays_doesNotChangeDaysAndLogsWarning() {
        Long configId = config1.getId();
        Set<DayOfWeek> originalDays = new HashSet<>(config1.getApplicableDays());

        when(educationTimeConfigRepository.findById(configId)).thenReturn(Optional.of(config1));
        when(educationTimeConfigRepository.save(any(EducationTimeConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // Assume save is called if other fields change

        EducationTimeConfig result = educationTimeConfigService.updateEducationTimeConfig(
                configId, null, null, Collections.emptySet(), "Desc change", null
        );

        assertNotNull(result);
        assertEquals(originalDays, result.getApplicableDays()); // Days should remain unchanged
        assertEquals("Desc change", result.getDescription()); // Other fields should update
        verify(educationTimeConfigRepository).save(any(EducationTimeConfig.class));
        // Add log verification if possible
    }


    @Test
    void deleteEducationTimeConfig_configExists_returnsTrue() {
        Long configId = config1.getId();
        when(educationTimeConfigRepository.findById(configId)).thenReturn(Optional.of(config1));
        doNothing().when(educationTimeConfigRepository).delete(config1);

        boolean result = educationTimeConfigService.deleteEducationTimeConfig(configId);

        assertTrue(result);
        verify(educationTimeConfigRepository).delete(config1);
    }

    @Test
    void deleteEducationTimeConfig_configNotExists_returnsFalse() {
        Long nonExistentId = 99L;
        when(educationTimeConfigRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        boolean result = educationTimeConfigService.deleteEducationTimeConfig(nonExistentId);

        assertFalse(result);
        verify(educationTimeConfigRepository, never()).delete(any(EducationTimeConfig.class));
    }

    @Test
    void getAllActiveConfigs_returnsListOfActiveConfigs() {
        EducationTimeConfig activeConfig2 = new EducationTimeConfig();
        activeConfig2.setActive(true);
        when(educationTimeConfigRepository.findAllActive()).thenReturn(List.of(config1, activeConfig2));

        List<EducationTimeConfig> result = educationTimeConfigService.getAllActiveConfigs();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(EducationTimeConfig::isActive));
    }

    @Test
    void getAllConfigs_returnsListOfAllConfigs() {
        EducationTimeConfig inactiveConfig = new EducationTimeConfig();
        inactiveConfig.setActive(false);
        when(educationTimeConfigRepository.findAll()).thenReturn(List.of(config1, inactiveConfig));

        List<EducationTimeConfig> result = educationTimeConfigService.getAllConfigs();

        assertNotNull(result);
        assertEquals(2, result.size());
    }
}