// src/main/java/com/sp/SwimmingPool/controller/HealthQuestionController.java
package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.HealthQuestionDTO;
import com.sp.SwimmingPool.service.HealthQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health-questions")
@RequiredArgsConstructor
@Slf4j
public class HealthQuestionController {

    private final HealthQuestionService healthQuestionService;

    @GetMapping
    public ResponseEntity<?> getHealthQuestionsForRegistration() {
        log.debug("Request received for health questions");
        try {
            List<HealthQuestionDTO> questions = healthQuestionService.getAllActiveQuestions();
            if (questions.isEmpty()) {
                log.info("No active health questions found.");
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            log.debug("Returning {} health questions", questions.size());
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            log.error("Error fetching health questions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to retrieve health questions."));
        }
    }
}
