package com.sp.SwimmingPool.dto;

import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.model.enums.SwimmingLevelEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private int id;
    private String userType; // MEMBER or STAFF
    private String role;
    private String email;
    private String name;
    private String surname;
    private StatusEnum status;
    private Boolean canSwim;
    private SwimmingLevelEnum swimmingLevel;
}