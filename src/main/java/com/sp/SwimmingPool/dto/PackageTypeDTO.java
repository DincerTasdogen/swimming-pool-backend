package com.sp.SwimmingPool.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class PackageTypeDTO {
    private int id;
    private String name;
    private String description;
    private int sessionLimit;
    private double price;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isEducationPackage;
    private boolean requiresSwimmingAbility;
}
