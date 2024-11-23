package com.sp.SwimmingPool.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "health_questions_reference")
@Getter
@Setter
public class HealthQuestionReference {

    @Id
    private int questionNumber;

    @Column(nullable = false, length = 500)
    private String questionText;

}
