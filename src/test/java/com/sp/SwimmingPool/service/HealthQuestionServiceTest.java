package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.HealthQuestionDTO;
import com.sp.SwimmingPool.model.entity.HealthQuestion;
import com.sp.SwimmingPool.repos.HealthQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthQuestionServiceTest {

    @Mock
    private HealthQuestionRepository healthQuestionRepository;

    @InjectMocks
    private HealthQuestionService healthQuestionService;

    private HealthQuestion question1;
    private HealthQuestion question2;
    private HealthQuestionDTO questionDTO1;

    @BeforeEach
    void setUp() {
        question1 = new HealthQuestion();
        question1.setId(1L);
        question1.setQuestionText("Do you have any heart conditions?");
        // Assuming other fields like weight, criticalQuestion, category are set if relevant for DTO mapping
        // For the current DTO (id, questionText), these are not strictly needed for mapping.

        question2 = new HealthQuestion();
        question2.setId(2L);
        question2.setQuestionText("Have you had surgery in the last 6 months?");

        questionDTO1 = new HealthQuestionDTO(question1.getId(), question1.getQuestionText());
    }

    @Test
    void getAllActiveQuestions_returnsListOfDTOs() {
        // The service method is named getAllActiveQuestions, but the implementation fetches all.
        // If there was an 'active' flag on HealthQuestion, this test would need to reflect that.
        // Based on current service code: healthQuestionRepository.findAll()
        when(healthQuestionRepository.findAll()).thenReturn(List.of(question1, question2));

        List<HealthQuestionDTO> result = healthQuestionService.getAllActiveQuestions();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(question1.getQuestionText(), result.get(0).getQuestionText());
        assertEquals(question1.getId(), result.get(0).getId());
        assertEquals(question2.getQuestionText(), result.get(1).getQuestionText());
        assertEquals(question2.getId(), result.get(1).getId());
    }

    @Test
    void getAllActiveQuestions_noQuestions_returnsEmptyList() {
        when(healthQuestionRepository.findAll()).thenReturn(Collections.emptyList());
        List<HealthQuestionDTO> result = healthQuestionService.getAllActiveQuestions();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void addQuestion_success_returnsSavedQuestionDTO() {
        HealthQuestionDTO newQuestionDTO = new HealthQuestionDTO(null, "New test question?");

        // Mock the save operation
        when(healthQuestionRepository.save(any(HealthQuestion.class))).thenAnswer(invocation -> {
            HealthQuestion hqToSave = invocation.getArgument(0);
            // Simulate ID generation and return the saved entity
            HealthQuestion savedHq = new HealthQuestion();
            savedHq.setId(3L); // Simulate generated ID
            savedHq.setQuestionText(hqToSave.getQuestionText());
            // Set other fields if they were part of the DTO or entity logic
            return savedHq;
        });

        HealthQuestionDTO result = healthQuestionService.addQuestion(newQuestionDTO);

        assertNotNull(result);
        assertEquals(3L, result.getId()); // Check for the generated ID
        assertEquals("New test question?", result.getQuestionText());

        ArgumentCaptor<HealthQuestion> captor = ArgumentCaptor.forClass(HealthQuestion.class);
        verify(healthQuestionRepository).save(captor.capture());
        assertEquals("New test question?", captor.getValue().getQuestionText());
        assertNull(captor.getValue().getId()); // ID should be null before save if DTO passes null
    }

    @Test
    void updateQuestion_questionExists_updatesAndReturnsDTO() {
        Long existingId = question1.getId();
        HealthQuestionDTO updatedInfoDTO = new HealthQuestionDTO(existingId, "Updated: Any heart conditions?");

        // Mock findById to return the existing question
        when(healthQuestionRepository.findById(existingId)).thenReturn(Optional.of(question1));
        // Mock save to return the updated question (or the argument passed to it)
        when(healthQuestionRepository.save(any(HealthQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HealthQuestionDTO result = healthQuestionService.updateQuestion(existingId, updatedInfoDTO);

        assertNotNull(result);
        assertEquals(existingId, result.getId());
        assertEquals("Updated: Any heart conditions?", result.getQuestionText());

        ArgumentCaptor<HealthQuestion> captor = ArgumentCaptor.forClass(HealthQuestion.class);
        verify(healthQuestionRepository).save(captor.capture());
        assertEquals("Updated: Any heart conditions?", captor.getValue().getQuestionText());
        assertEquals(existingId, captor.getValue().getId());
    }

    @Test
    void updateQuestion_questionNotExists_throwsRuntimeException() {
        Long nonExistentId = 99L;
        HealthQuestionDTO dtoForUpdate = new HealthQuestionDTO(nonExistentId, "Attempt to update");
        when(healthQuestionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            healthQuestionService.updateQuestion(nonExistentId, dtoForUpdate);
        });
        assertEquals("Question not found", exception.getMessage()); // Or whatever message your service throws
        verify(healthQuestionRepository, never()).save(any(HealthQuestion.class));
    }

    @Test
    void deleteQuestion_questionExists_deletesQuestion() {
        Long existingId = question1.getId();
        doNothing().when(healthQuestionRepository).deleteById(existingId);

        healthQuestionService.deleteQuestion(existingId);

        verify(healthQuestionRepository).deleteById(existingId);
    }

    @Test
    void deleteQuestion_questionNotExists_deleteAttempted() {
        Long nonExistentId = 99L;
        doNothing().when(healthQuestionRepository).deleteById(nonExistentId);

        healthQuestionService.deleteQuestion(nonExistentId);

        verify(healthQuestionRepository).deleteById(nonExistentId); // Verify the attempt was made
    }
}