package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.NewsDTO;
import com.sp.SwimmingPool.exception.EntityNotFoundException;
import com.sp.SwimmingPool.model.entity.News;
import com.sp.SwimmingPool.repos.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @InjectMocks
    private NewsService newsService;

    private News news1;
    private News news2;

    @BeforeEach
    void setUp() {
        LocalDateTime fixedTime = LocalDateTime.now();

        news1 = News.builder()
                .id(1)
                .title("Pool Reopening Soon!")
                .content("The main pool will reopen next Monday after maintenance.")
                .imageUrl("http://example.com/pool.jpg")
                .author("Admin")
                .createdAt(fixedTime.minusDays(1))
                .updatedAt(fixedTime.minusDays(1))
                .build();

        news2 = News.builder()
                .id(2)
                .title("New Swimming Classes")
                .content("Beginner classes start next month. Sign up now!")
                .imageUrl("http://example.com/classes.jpg")
                .author("Coach Alice")
                .createdAt(fixedTime.minusHours(5))
                .updatedAt(fixedTime.minusHours(5))
                .build();

    }

    @Test
    void getAllNews_returnsListOfNewsDTOs() {
        when(newsRepository.findAll()).thenReturn(List.of(news1, news2));

        List<NewsDTO> result = newsService.getAllNews();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(news1.getTitle(), result.get(0).getTitle());
        assertEquals(news2.getTitle(), result.get(1).getTitle());
    }

    @Test
    void getAllNews_noNews_returnsEmptyList() {
        when(newsRepository.findAll()).thenReturn(Collections.emptyList());
        List<NewsDTO> result = newsService.getAllNews();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getNewsById_newsExists_returnsNewsDTO() {
        when(newsRepository.findById(news1.getId())).thenReturn(Optional.of(news1));
        NewsDTO result = newsService.getNewsById(news1.getId());
        assertNotNull(result);
        assertEquals(news1.getTitle(), result.getTitle());
        assertEquals(news1.getId(), result.getId());
    }

    @Test
    void getNewsById_newsNotExists_throwsEntityNotFoundException() {
        int nonExistentId = 99;
        when(newsRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> newsService.getNewsById(nonExistentId));
        assertEquals(nonExistentId + " ID'li haber bulunamadı.", exception.getMessage());
    }

    @Test
    void createNews_success_returnsCreatedNewsDTO() {
        NewsDTO newNewsToCreate = NewsDTO.builder()
                .title("Important Announcement")
                .content("Details about the upcoming event.")
                .imageUrl("http://example.com/event.png")
                .author("Event Coordinator")
                .build(); // createdAt and updatedAt will be set by service

        // Mock the save operation
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News newsToSave = invocation.getArgument(0);
            // Simulate ID generation and timestamp setting by the service/JPA
            return News.builder()
                    .id(3) // Simulate generated ID
                    .title(newsToSave.getTitle())
                    .content(newsToSave.getContent())
                    .imageUrl(newsToSave.getImageUrl())
                    .author(newsToSave.getAuthor())
                    .createdAt(LocalDateTime.now()) // Simulate @PrePersist or service logic
                    .updatedAt(LocalDateTime.now()) // Simulate @PrePersist or service logic
                    .build();
        });

        NewsDTO result = newsService.createNews(newNewsToCreate);

        assertNotNull(result);
        assertEquals(3, result.getId());
        assertEquals(newNewsToCreate.getTitle(), result.getTitle());
        assertEquals(newNewsToCreate.getAuthor(), result.getAuthor());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(newsCaptor.capture());
        News capturedNews = newsCaptor.getValue();
        assertEquals(newNewsToCreate.getTitle(), capturedNews.getTitle());
        assertNotNull(capturedNews.getCreatedAt()); // Service sets this
        assertNotNull(capturedNews.getUpdatedAt()); // Service sets this
    }

    @Test
    void updateNews_newsExists_updatesAndReturnsNewsDTO() {
        int existingId = news1.getId();
        NewsDTO updatedInfoDTO = NewsDTO.builder()
                .id(existingId) // ID is usually part of the DTO for updates
                .title("UPDATED: Pool Reopening!")
                .content(news1.getContent() + " More details added.")
                .imageUrl(news1.getImageUrl())
                .author("Senior Admin")
                // createdAt should not be changed by update
                .build();

        LocalDateTime originalUpdatedAt = news1.getUpdatedAt();


        when(newsRepository.findById(existingId)).thenReturn(Optional.of(news1));
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News newsToSave = invocation.getArgument(0);
            // Simulate timestamp update
            newsToSave.setUpdatedAt(LocalDateTime.now());
            return newsToSave;
        });

        NewsDTO result = newsService.updateNews(existingId, updatedInfoDTO);

        assertNotNull(result);
        assertEquals(existingId, result.getId());
        assertEquals(updatedInfoDTO.getTitle(), result.getTitle());
        assertEquals(updatedInfoDTO.getContent(), result.getContent());
        assertEquals(updatedInfoDTO.getAuthor(), result.getAuthor());
        assertNotNull(result.getUpdatedAt());
        assertTrue(result.getUpdatedAt().isAfter(originalUpdatedAt) || result.getUpdatedAt().isEqual(originalUpdatedAt));
        assertEquals(news1.getCreatedAt(), result.getCreatedAt()); // CreatedAt should remain the same

        ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(newsCaptor.capture());
        News capturedNews = newsCaptor.getValue();
        assertEquals(updatedInfoDTO.getTitle(), capturedNews.getTitle());
        assertEquals(news1.getCreatedAt(), capturedNews.getCreatedAt()); // Ensure createdAt wasn't touched
    }

    @Test
    void updateNews_newsNotExists_throwsRuntimeException() {
        int nonExistentId = 99;
        NewsDTO dtoForUpdate = NewsDTO.builder().id(nonExistentId).title("Non Existent").build();
        when(newsRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            newsService.updateNews(nonExistentId, dtoForUpdate);
        });
        assertEquals("News not found", exception.getMessage());
        verify(newsRepository, never()).save(any(News.class));
    }

    @Test
    void deleteNews_newsExists_deletesNews() {
        int existingId = news1.getId();
        doNothing().when(newsRepository).deleteById(existingId);

        newsService.deleteNews(existingId);

        verify(newsRepository).deleteById(existingId);
    }

    @Test
    void deleteNews_newsNotExists_deleteAttempted() {
        // As above, deleteById usually doesn't care if the ID exists.
        int nonExistentId = 99;
        doNothing().when(newsRepository).deleteById(nonExistentId);

        newsService.deleteNews(nonExistentId);

        verify(newsRepository).deleteById(nonExistentId);
    }
}