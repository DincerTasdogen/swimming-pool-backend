package com.sp.SwimmingPool.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EducationStatusUpdateRequest {

    @NotNull
    private Boolean educationSession;
}
