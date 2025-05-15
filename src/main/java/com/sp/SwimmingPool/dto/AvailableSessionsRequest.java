package com.sp.SwimmingPool.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class AvailableSessionsRequest {
    @NotNull(message = "Member Package ID is required")
    @Min(value = 1, message = "Member Package ID must be positive")
    private int memberPackageId;

    @NotNull(message = "Pool ID is required")
    @Min(value = 1, message = "Pool ID must be positive")
    private int poolId;

    @NotNull(message = "Date is required")
    private LocalDate date;
}
