package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.PackageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageTypeRepository extends JpaRepository<PackageType, Integer> {
    List<PackageType> findByIsEducationPackageTrue();
    List<PackageType> findByIsEducationPackageFalse();
    boolean existsByName(String name);
    PackageType findByName(String name);
    List<PackageType> findByIsActiveTrue();
    List<PackageType> findByIsEducationPackageTrueAndIsActiveTrue();
    List<PackageType> findByIsEducationPackageFalseAndIsActiveTrue();

    List<PackageType> findAllByOrderByIsActiveDescNameAsc();

    List<PackageType> findByIsActiveTrueOrderByNameAsc();
}
