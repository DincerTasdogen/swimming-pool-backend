package com.sp.SwimmingPool.dto;

import com.sp.SwimmingPool.model.enums.ReservationStatusEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationDTO {
    private int id;
    private int memberId;
    private int sessionId;
    private int memberPackageId;
    private ReservationStatusEnum status;
}
