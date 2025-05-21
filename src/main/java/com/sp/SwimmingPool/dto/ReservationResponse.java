package com.sp.SwimmingPool.dto;

import com.sp.SwimmingPool.model.enums.ReservationStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@AllArgsConstructor
@Data
public class ReservationResponse {
    private int id;
    private int memberId;
    private int sessionId;
    private int memberPackageId;
    private ReservationStatusEnum status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String poolName;
    private boolean isEducationSession;
    private int remainingCapacity;
}
