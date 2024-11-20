package com.swimmingpool.repos;

import com.swimmingpool.model.entity.PackageType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PackageTypeRepository extends JpaRepository<PackageType, Integer> {
}
