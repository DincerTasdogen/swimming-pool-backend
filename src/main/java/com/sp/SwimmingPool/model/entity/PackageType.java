package com.sp.SwimmingPool.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(
        name = "package_type",
        indexes = {
                @Index(name = "uk_name", columnList = "name", unique = true)
        }
)
@Getter
@Setter
public class PackageType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(nullable = false)
    private int sessionLimit;

    @Column(nullable = false, precision = 10)
    private Double price;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private boolean isEducationPackage;

    @Column(nullable = false)
    private boolean requiresSwimmingAbility;

    @Column(columnDefinition = "boolean default true")
    private boolean isActive;

}
