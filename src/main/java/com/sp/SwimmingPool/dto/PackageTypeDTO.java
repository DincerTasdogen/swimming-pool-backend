package com.sp.SwimmingPool.dto;

import lombok.*;

import java.time.LocalTime;

@NoArgsConstructor
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

    @Builder
    public PackageTypeDTO(
            int id,
            String name,
            String description,
            int sessionLimit,
            double price,
            LocalTime startTime,
            LocalTime endTime,
            boolean isEducationPackage,
            boolean requiresSwimmingAbility

    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sessionLimit = sessionLimit;
        this.price = price;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isEducationPackage = isEducationPackage;
        this.requiresSwimmingAbility = requiresSwimmingAbility;
    }


}
