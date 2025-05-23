package com.sp.SwimmingPool.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Data
public class EducationTimeConfigRequest {
    @NotNull
    private LocalTime startTime;
    @NotNull
    private LocalTime endTime;
    @NotNull
    private Set<DayOfWeek> applicableDays;
    private String description;
    private Boolean active;
}