package com.sp.SwimmingPool.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PoolDTO {
    private int id;
    private String name;
    private String location;
    private double latitude;
    private double longitude;
    private double depth;
    private int capacity;
    private String openAt;
    private String closeAt;
    private boolean isActive;

}
