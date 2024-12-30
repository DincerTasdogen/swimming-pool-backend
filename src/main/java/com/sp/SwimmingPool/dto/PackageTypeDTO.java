package com.sp.SwimmingPool.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sp.SwimmingPool.model.entity.PackageType;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double price;
    private LocalTime startTime;
    private LocalTime endTime;
    @JsonProperty("isEducationPackage")
    private boolean isEducationPackage;
    private boolean requiresSwimmingAbility;
    private boolean multiplePools;

    @Builder
    public PackageTypeDTO(
            int id,
            String name,
            String description,
            int sessionLimit,
            Double price,
            LocalTime startTime,
            LocalTime endTime,
            boolean isEducationPackage,
            boolean requiresSwimmingAbility,
            boolean multiplePools
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
        this.multiplePools = multiplePools;
    }

    public static PackageTypeDTO fromEntity(PackageType entity) {
        return PackageTypeDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .sessionLimit(entity.getSessionLimit())
                .price(entity.getPrice())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .isEducationPackage(entity.isEducationPackage())
                .requiresSwimmingAbility(entity.isRequiresSwimmingAbility())
                .multiplePools(entity.isMultiplePools())
                .build();
    }

    public PackageType toEntity() {
        PackageType entity = new PackageType();
        updateEntity(entity);
        return entity;
    }

    public void updateEntity(PackageType entity) {
        entity.setName(this.name);
        entity.setDescription(this.description);
        entity.setSessionLimit(this.sessionLimit);
        entity.setPrice(this.price);
        entity.setStartTime(this.startTime);
        entity.setEndTime(this.endTime);
        entity.setEducationPackage(this.isEducationPackage);
        entity.setRequiresSwimmingAbility(this.requiresSwimmingAbility);
        entity.setMultiplePools(this.multiplePools);
    }
}