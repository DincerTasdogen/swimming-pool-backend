package com.swimmingpool.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pool")
@Getter
@Setter
public class Pool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(precision = 10)
    private Double latitude;

    @Column(precision = 11)
    private Double longitude;

    @Column(nullable = false, precision = 3)
    private Double depth;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private String openAt;

    @Column(nullable = false)
    private String closeAt;

    @Column(columnDefinition = "boolean default true")
    private boolean isActive;

    @Column(columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

}
