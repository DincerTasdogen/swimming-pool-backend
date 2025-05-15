package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.EducationTimeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EducationTimeConfigRepository extends JpaRepository<EducationTimeConfig, Long> {

    List<EducationTimeConfig> findByActiveTrue();

    default List<EducationTimeConfig> findAllActive() {
        return findByActiveTrue();
    }

}
