package com.sp.SwimmingPool.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private int id;
    private String token;
    private String userType; // MEMBER or STAFF
    private String role;
    private String email;
    private String name;
}