package com.sp.SwimmingPool.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsDTO {
    private Integer id;
    private String title;
    private String content;
    private String imageUrl;
    private String author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
