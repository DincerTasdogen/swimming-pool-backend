package com.sp.SwimmingPool.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PackageTypeDTO {
    private String name;
    private String description;
    private int sessionLimit;
    private double price;
    private String startTime;
    private String endTime;
    private boolean isEducationPackage;
    private boolean requiresSwimmingAbility;
}
