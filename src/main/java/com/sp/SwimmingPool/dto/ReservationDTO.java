package com.sp.SwimmingPool.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationDTO {
    private int memberId;
    private int sessionId;
    private int memberPackageId;
    private String status;
}
