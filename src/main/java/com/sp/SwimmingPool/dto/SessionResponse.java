package com.sp.SwimmingPool.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@Data
public class SessionResponse {
    private int id;
    private int poolId;
    private String poolName;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private int capacity;
    private int currentBookings;
    private int availableSpots;
    private boolean isEducationSession;
    private boolean isBookable;
    private String bookableReason; // In case the session is not bookable, this explains why
}