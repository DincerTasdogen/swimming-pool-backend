package com.sp.SwimmingPool.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReservationRequest {
    @NotNull(message = "Session ID is required")
    @Min(value = 1, message = "Session ID must be positive")
    private int sessionId;

    @NotNull(message = "Member Package ID is required")
    @Min(value = 1, message = "Member Package ID must be positive")
    private int memberPackageId;
}
