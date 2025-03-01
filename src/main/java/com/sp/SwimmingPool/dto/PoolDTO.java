package com.sp.SwimmingPool.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PoolDTO {
    private String name;
    private String location;
    private String city;
    private double latitude;
    private double longitude;
    private double depth;
    private int capacity;
    private String openAt;
    private String closeAt;
    private String description;
    private String imagePath;
    private List<String> features;
    private boolean isActive;

}
