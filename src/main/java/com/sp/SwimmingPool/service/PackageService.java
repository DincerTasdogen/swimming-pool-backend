package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.MemberPackageDTO;
import com.sp.SwimmingPool.dto.PackageTypeDTO;
import com.sp.SwimmingPool.model.entity.MemberPackage;
import com.sp.SwimmingPool.model.entity.PackageType;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.repos.MemberPackageRepository;
import com.sp.SwimmingPool.repos.PackageTypeRepository;
import com.sp.SwimmingPool.repos.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final PackageTypeRepository packageTypeRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    public List<PackageTypeDTO> listPackageTypes() {
        return packageTypeRepository.findAll()
                .stream()
                .map(PackageTypeDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public PackageTypeDTO createPackage(PackageTypeDTO packageTypeDTO) {
        PackageType packageType = packageTypeDTO.toEntity();
        PackageType savedPackage = packageTypeRepository.save(packageType);
        return PackageTypeDTO.fromEntity(savedPackage);
    }

    public PackageTypeDTO updatePackage(int id, PackageTypeDTO packageTypeDTO) {
        PackageType packageType = packageTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Package not found with id: " + id));

        packageTypeDTO.updateEntity(packageType);
        PackageType updatedPackage = packageTypeRepository.save(packageType);
        return PackageTypeDTO.fromEntity(updatedPackage);
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
                .map(PackageTypeDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<PackageTypeDTO> listOtherPackages() {
        return packageTypeRepository.findByIsEducationPackageFalse()
                .stream()
                .map(PackageTypeDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public PackageTypeDTO getPackageById(int id) {
        PackageType packageType = packageTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Package not found with id: " + id));
        return PackageTypeDTO.fromEntity(packageType);
    }


    public MemberPackageDTO createMemberPackage(MemberPackageDTO memberPackageDTO) {
        // 1. Check if member status is ACTIVE
        Member member = memberRepository.findById(memberPackageDTO.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + memberPackageDTO.getMemberId()));

        if (member.getStatus() != StatusEnum.ACTIVE) {
            throw new InvalidOperationException("Member status must be ACTIVE to purchase a package. Current status: " + member.getStatus());
        }

        // 2. Get package type to validate requirements
        PackageType packageType = packageTypeRepository.findById(memberPackageDTO.getPackageTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Package type not found with id: " + memberPackageDTO.getPackageTypeId()));

        // 3. Check swimming ability requirement
        if (packageType.isRequiresSwimmingAbility()) {
            try {
                boolean canSwim = memberService.hasSwimmingAbility(memberPackageDTO.getMemberId());
                if (!canSwim) {
                    throw new InvalidOperationException("This package requires swimming ability which the member does not have");
                }
            } catch (Exception e) {
                throw new InvalidOperationException("Error checking member swimming ability: " + e.getMessage());
            }
        }

        // 4. Check if member can buy this package
        if (!canBuyPackage(memberPackageDTO.getMemberId(), memberPackageDTO.getPoolId())) {
            throw new InvalidOperationException("Member cannot buy this package due to existing active package restrictions");
        }

        // 5. Create and save the member package
        MemberPackage memberPackage = MemberPackageDTO.convertToMemberPackage(memberPackageDTO);

        // Set sessions remaining based on the package type
        memberPackage.setSessionsRemaining(packageType.getSessionLimit());

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

    public List<MemberPackageDTO> getMemberPackages(int memberId) {
        List<MemberPackage> packages = memberPackageRepository.findByMemberId(memberId);
        return packages.stream()
                .map(MemberPackageDTO::createFromMemberPackage)
                .collect(Collectors.toList());
    }

    public boolean canBuyPackage(int memberId, Integer newPoolId) {
        if (memberPackageRepository.existsByMemberIdAndActiveTrueAndPoolIdIsNull(memberId)) {
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