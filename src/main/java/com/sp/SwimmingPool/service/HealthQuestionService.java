package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.HealthQuestionDTO;
import com.sp.SwimmingPool.model.entity.HealthQuestion;
import com.sp.SwimmingPool.repos.HealthQuestionRepository; // Assuming you have this repository
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthQuestionService {

    private final HealthQuestionRepository healthQuestionRepository;

    public List<HealthQuestionDTO> getAllActiveQuestions() {
        List<HealthQuestion> questions = healthQuestionRepository.findAll();
        return questions.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private HealthQuestionDTO mapToDto(HealthQuestion question) {
        return new HealthQuestionDTO(question.getId(), question.getQuestionText());
    }
}
