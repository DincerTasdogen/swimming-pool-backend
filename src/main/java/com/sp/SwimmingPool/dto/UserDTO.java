package com.sp.SwimmingPool.dto;

import com.sp.SwimmingPool.model.enums.UserRoleEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserDTO {
    private int id;
    private String name;
    private String surname;
    private String email;
    private UserRoleEnum role;
    private int memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
