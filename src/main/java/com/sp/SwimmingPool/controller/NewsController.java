package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.NewsDTO;
import com.sp.SwimmingPool.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public ResponseEntity<List<NewsDTO>> getAllNews() {
        return ResponseEntity.ok(newsService.getAllNews());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsDTO> getNews(@PathVariable("id") int id) {return ResponseEntity.ok(newsService.getNewsById(id));}

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsDTO> createNews(@RequestBody NewsDTO dto) {
        return ResponseEntity.ok(newsService.createNews(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsDTO> updateNews(@PathVariable Integer id, @RequestBody NewsDTO dto) {
        return ResponseEntity.ok(newsService.updateNews(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNews(@PathVariable Integer id) {
        newsService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }
}
