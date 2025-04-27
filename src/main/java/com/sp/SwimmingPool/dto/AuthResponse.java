package com.sp.SwimmingPool.dto;

import com.sp.SwimmingPool.model.enums.StatusEnum;
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
}