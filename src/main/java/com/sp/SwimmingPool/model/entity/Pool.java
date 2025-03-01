package com.sp.SwimmingPool.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pool")
@Getter
@Setter
public class Pool {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String city;

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

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String imagePath;

    @Column(name = "features_json", columnDefinition = "TEXT")
    @JsonIgnore
    private String featuresJson;

    @Transient
    private List<String> features;

    @Column(columnDefinition = "boolean default true")
    private boolean isActive;

    @Column(columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    // Convert between JSON string and List when getting features
    public List<String> getFeatures() {
        if (features != null) {
            return features;
        }

        if (featuresJson == null || featuresJson.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            features = objectMapper.readValue(featuresJson, new TypeReference<List<String>>() {});
            return features;
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    // Convert between List and JSON string when setting features
    public void setFeatures(List<String> features) {
        this.features = features;
        try {
            this.featuresJson = (features != null && !features.isEmpty())
                    ? objectMapper.writeValueAsString(features)
                    : "[]";
        } catch (JsonProcessingException e) {
            this.featuresJson = "[]";
        }
    }
}