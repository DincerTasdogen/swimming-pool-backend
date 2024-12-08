package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.PackageTypeDTO;
import com.sp.SwimmingPool.model.entity.PackageType;
import com.sp.SwimmingPool.repos.PackageTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PackageService {

    @Autowired
    private PackageTypeRepository packageTypeRepository;

    public List<PackageTypeDTO> listPackageTypes() {
        List<PackageType> packageTypes = packageTypeRepository.findAll();
        List<PackageTypeDTO> packageTypeDTOs = new ArrayList<>();
        for (PackageType packageType : packageTypes) {
            PackageTypeDTO packageTypeDTO = new PackageTypeDTO();
            packageTypeDTO.setId(packageType.getId());
            packageTypeDTO.setName(packageType.getName());
            packageTypeDTO.setDescription(packageType.getDescription());
            packageTypeDTO.setSessionLimit(packageType.getSessionLimit());
            packageTypeDTO.setPrice(packageType.getPrice());
            packageTypeDTO.setStartTime(packageType.getStartTime());
            packageTypeDTO.setEndTime(packageType.getEndTime());
            packageTypeDTO.setEducationPackage(packageType.isEducationPackage());
            packageTypeDTO.setRequiresSwimmingAbility(packageType.isRequiresSwimmingAbility());
            packageTypeDTOs.add(packageTypeDTO);
        }
        return packageTypeDTOs;
    }
    public PackageTypeDTO createPackage(PackageTypeDTO packageTypeDTO) {
        PackageType packageType = new PackageType();
        packageType.setName(packageTypeDTO.getName());
        packageType.setDescription(packageTypeDTO.getDescription());
        packageType.setSessionLimit(packageTypeDTO.getSessionLimit());
        packageType.setPrice(packageTypeDTO.getPrice());
        packageType.setStartTime(packageTypeDTO.getStartTime());
        packageType.setEndTime(packageTypeDTO.getEndTime());
        packageType.setEducationPackage(packageTypeDTO.isEducationPackage());
        packageType.setRequiresSwimmingAbility(packageTypeDTO.isRequiresSwimmingAbility());

        packageTypeRepository.save(packageType);
        return packageTypeDTO;
    }

    public PackageTypeDTO updatePackage(int id, PackageTypeDTO packageTypeDTO) {
        PackageType packageType = packageTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Package not found with id: " + id));
        packageType.setName(packageTypeDTO.getName());
        packageType.setDescription(packageTypeDTO.getDescription());
        packageType.setSessionLimit(packageTypeDTO.getSessionLimit());
        packageType.setPrice(packageTypeDTO.getPrice());
        packageType.setStartTime(packageTypeDTO.getStartTime());
        packageType.setEndTime(packageTypeDTO.getEndTime());
        packageType.setEducationPackage(packageTypeDTO.isEducationPackage());
        packageType.setRequiresSwimmingAbility(packageTypeDTO.isRequiresSwimmingAbility());

        packageTypeRepository.save(packageType);
        return packageTypeDTO;
    }
    public void deletePackage(int id) {
        if (packageTypeRepository.existsById(id)) {
            packageTypeRepository.deleteById(id);
        } else {
            throw new RuntimeException("Package with id " + id + " not found");
        }
    }

}
