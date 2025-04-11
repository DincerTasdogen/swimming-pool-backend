package com.sp.SwimmingPool.config;

import com.sp.SwimmingPool.service.ReservationService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class ReservationSchedulerConfig {

    private final ReservationService reservationService;

    public ReservationSchedulerConfig(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(cron = "0 0 * * * *") // Run at the top of every hour
    public void processMissedReservations() {
        reservationService.processMissedReservations();
    }
}