package com.sp.SwimmingPool.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AvailableSessionsRequest {

    @NotNull(message = "Member ID is required")
    @Min(value = 1, message = "Member ID must be positive")
    private int memberId;

    @NotNull(message = "Member Package ID is required")
    @Min(value = 1, message = "Member Package ID must be positive")
    private int memberPackageId;

    @NotNull(message = "Date is required")
    private LocalDate date;
}
