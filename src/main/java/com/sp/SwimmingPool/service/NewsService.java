package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.NewsDTO;
import com.sp.SwimmingPool.exception.EntityNotFoundException;
import com.sp.SwimmingPool.model.entity.News;
import com.sp.SwimmingPool.repos.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;

    public List<NewsDTO> getAllNews() {
        return newsRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public NewsDTO getNewsById(int id) {
        News news = newsRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException(id + " ID'li haber bulunamadı.")
        );
        return convertToDTO(news);
    }

    public NewsDTO createNews(NewsDTO dto) {
        News news = News.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .imageUrl(dto.getImageUrl())
                .author(dto.getAuthor())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return convertToDTO(newsRepository.save(news));
    }

    public NewsDTO updateNews(Integer id, NewsDTO dto) {
        Optional<News> optionalNews = newsRepository.findById(id);
        if (optionalNews.isEmpty()) {
            throw new RuntimeException("News not found");
        }

        News news = optionalNews.get();
        news.setTitle(dto.getTitle());
        news.setContent(dto.getContent());
        news.setImageUrl(dto.getImageUrl());
        news.setAuthor(dto.getAuthor());
        news.setUpdatedAt(LocalDateTime.now());
        return convertToDTO(newsRepository.save(news));
    }

    public void deleteNews(Integer id) {
        newsRepository.deleteById(id);
    }

    private NewsDTO convertToDTO(News news) {
        return NewsDTO.builder()
                .id(news.getId())
                .title(news.getTitle())
                .content(news.getContent())
                .imageUrl(news.getImageUrl())
                .author(news.getAuthor())
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .build();
    }
}
