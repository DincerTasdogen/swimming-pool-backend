package com.sp.SwimmingPool.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "health_questions")
@Getter
@Setter
public class HealthQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String questionText;

    @Column(nullable = false)
    private int weight; // Importance level: 1-5

    @Column(nullable = false)
    private boolean criticalQuestion; // Questions that automatically require doctor review

    @Column(nullable = false)
    private String category; // e.g., CARDIAC, NEUROLOGICAL, MUSCULAR, etc.

    private String additionalInfo; // Any extra info about the question
}
