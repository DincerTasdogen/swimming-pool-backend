package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.PackageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;

public interface PackageTypeRepository extends JpaRepository<PackageType, Integer> {
    List<PackageType> findByIsEducationPackageTrue();
    List<PackageType> findByIsEducationPackageFalse();
}
