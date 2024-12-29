package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.MemberPackageDTO;
import com.sp.SwimmingPool.dto.PackageTypeDTO;
import com.sp.SwimmingPool.model.entity.MemberPackage;
import com.sp.SwimmingPool.model.entity.PackageType;
import com.sp.SwimmingPool.repos.MemberPackageRepository;
import com.sp.SwimmingPool.repos.PackageTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PackageService {

    @Autowired
    private PackageTypeRepository packageTypeRepository;

    @Autowired
    private MemberPackageRepository memberPackageRepository;

    private PackageTypeDTO convertToDTO(PackageType packageType) {
        return PackageTypeDTO.builder()
                .id(packageType.getId())
                .name(packageType.getName())
                .description(packageType.getDescription())
                .sessionLimit(packageType.getSessionLimit())
                .price(packageType.getPrice())
                .startTime(packageType.getStartTime())
                .endTime(packageType.getEndTime())
                .isEducationPackage(packageType.isEducationPackage())
                .requiresSwimmingAbility(packageType.isRequiresSwimmingAbility())
                .build();
    }

    private PackageType convertToPackageType(PackageTypeDTO dto) {
        PackageType packageType = new PackageType();
        packageType.setName(dto.getName());
        packageType.setDescription(dto.getDescription());
        packageType.setSessionLimit(dto.getSessionLimit());
        packageType.setPrice(dto.getPrice());
        packageType.setStartTime(dto.getStartTime());
        packageType.setEndTime(dto.getEndTime());
        packageType.setEducationPackage(dto.isEducationPackage());
        packageType.setRequiresSwimmingAbility(dto.isRequiresSwimmingAbility());
        return packageType;
    }

    public List<PackageTypeDTO> listPackageTypes() {
        return packageTypeRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PackageTypeDTO createPackage(PackageTypeDTO packageTypeDTO) {
        PackageType packageType = convertToPackageType(packageTypeDTO);
        PackageType savedPackage = packageTypeRepository.save(packageType);
        return convertToDTO(savedPackage);
    }

    public PackageTypeDTO updatePackage(int id, PackageTypeDTO packageTypeDTO) {
        PackageType packageType = packageTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Package not found with id: " + id));

        // Update the existing entity with new values
        packageType.setName(packageTypeDTO.getName());
        packageType.setDescription(packageTypeDTO.getDescription());
        packageType.setSessionLimit(packageTypeDTO.getSessionLimit());
        packageType.setPrice(packageTypeDTO.getPrice());
        packageType.setStartTime(packageTypeDTO.getStartTime());
        packageType.setEndTime(packageTypeDTO.getEndTime());
        packageType.setEducationPackage(packageTypeDTO.isEducationPackage());
        packageType.setRequiresSwimmingAbility(packageTypeDTO.isRequiresSwimmingAbility());

        PackageType updatedPackage = packageTypeRepository.save(packageType);
        return convertToDTO(updatedPackage);
    }

    public void deletePackage(int id) {
        if (!packageTypeRepository.existsById(id)) {
            throw new IllegalArgumentException("Package with id " + id + " not found");
        }
        packageTypeRepository.deleteById(id);
    }

    public List<PackageTypeDTO> listEducationPackages() {
        return packageTypeRepository.findByIsEducationPackageTrue()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<PackageTypeDTO> listOtherPackages() {
        return packageTypeRepository.findByIsEducationPackageFalse()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PackageTypeDTO getPackageById(int id) {
        PackageType packageType = packageTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Package not found with id: " + id));
        return convertToDTO(packageType);
    }


    public MemberPackageDTO createMemberPackage(MemberPackageDTO memberPackageDTO) {
        if (!canBuyPackage(memberPackageDTO.getMemberId(), memberPackageDTO.getPoolId())) {
            throw new InvalidOperationException("Member cannot buy this package due to existing active package restrictions");
        }

        MemberPackage memberPackage = MemberPackageDTO.convertToMemberPackage(memberPackageDTO);
        memberPackage = memberPackageRepository.save(memberPackage);
        return MemberPackageDTO.createFromMemberPackage(memberPackage);
    }

    public List<MemberPackageDTO> getActiveMemberPackages(int memberId) {
        List<MemberPackage> activePackages = memberPackageRepository.findByMemberIdAndActiveTrue(memberId);
        return activePackages.stream()
                .map(MemberPackageDTO::createFromMemberPackage)
                .collect(Collectors.toList());
    }

    public List<MemberPackageDTO> getPreviousMemberPackages(int memberId) {
        List<MemberPackage> previousPackages = memberPackageRepository.findByMemberIdAndActiveFalse(memberId);
        return previousPackages.stream()
                .map(MemberPackageDTO::createFromMemberPackage)
                .collect(Collectors.toList());
    }

    public boolean canBuyPackage(int memberId, Integer newPoolId) {
        if(memberPackageRepository.existsByMemberIdAndActiveTrueAndPoolIdIsNull(memberId)) {
            return false;
        }

        if (newPoolId == null) {
            return !memberPackageRepository.existsByMemberIdAndActiveTrue(memberId);
        }

        return !memberPackageRepository.existsByMemberIdAndActiveTrueAndPoolId(memberId, newPoolId);
    }

    public boolean hasActiveMemberPackages(int memberId) {
        return memberPackageRepository.existsByMemberIdAndActiveTrue(memberId);
    }

}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}