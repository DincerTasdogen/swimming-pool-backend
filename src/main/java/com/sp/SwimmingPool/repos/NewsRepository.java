package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRepository extends JpaRepository<News, Integer> {
}
