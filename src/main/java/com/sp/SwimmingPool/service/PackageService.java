package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.MemberPackageDTO;
import com.sp.SwimmingPool.dto.PackageTypeDTO;
import com.sp.SwimmingPool.exception.InvalidOperationException;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.MemberPackage;
import com.sp.SwimmingPool.model.entity.PackageType;
import com.sp.SwimmingPool.model.entity.Pool;
import com.sp.SwimmingPool.model.enums.MemberPackagePaymentStatusEnum;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.repos.MemberPackageRepository;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.repos.PackageTypeRepository;
import com.sp.SwimmingPool.repos.PoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final PackageTypeRepository packageTypeRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final PoolRepository poolRepository;

    private MemberPackageDTO enrichDTO(MemberPackage memberPackage) {
        if (memberPackage == null) {
            return null;
        }

        MemberPackageDTO.MemberPackageDTOBuilder dtoBuilder = MemberPackageDTO.builder()
                .id(memberPackage.getId())
                .memberId(memberPackage.getMemberId())
                .packageTypeId(memberPackage.getPackageTypeId())
                .purchaseDate(memberPackage.getPurchaseDate())
                .sessionsRemaining(memberPackage.getSessionsRemaining())
                .active(memberPackage.isActive())
                .poolId(memberPackage.getPoolId())
                .paymentStatus(memberPackage.getPaymentStatus())
                .paymentDate(memberPackage.getPaymentDate());

        Optional<PackageType> ptOpt = packageTypeRepository.findById(memberPackage.getPackageTypeId());
        if (ptOpt.isPresent()) {
            PackageType pt = ptOpt.get();
            dtoBuilder.packageName(pt.getName())
                    .packageDescription(pt.getDescription())
                    .packageStartTime(pt.getStartTime())
                    .packageEndTime(pt.getEndTime())
                    .isEducationPackage(pt.isEducationPackage())
                    .requiresSwimmingAbility(pt.isRequiresSwimmingAbility())
                    .multiplePools(pt.isMultiplePools())
                    .packageSessionLimit(pt.getSessionLimit());
        }

        if (memberPackage.getPoolId() > 0 && ptOpt.isPresent() && !ptOpt.get().isMultiplePools()) {
            Optional<Pool> poolOpt = poolRepository.findById(memberPackage.getPoolId());
            if (poolOpt.isPresent()) {
                Pool pool = poolOpt.get();
                dtoBuilder.poolName(pool.getName())
                        .poolCity(pool.getCity());
            }
        } else if (ptOpt.isPresent() && ptOpt.get().isMultiplePools()) {
            dtoBuilder.poolName("Birden Fazla Havuzda Geçerli");
        }
        return dtoBuilder.build();
    }


    public List<PackageTypeDTO> listPackageTypes() {
        return packageTypeRepository.findAllByOrderByIsActiveDescNameAsc() // Show active ones first, then by name
                .stream()
                .map(PackageTypeDTO::fromEntity)
                .collect(Collectors.toList());
    }
    public List<PackageTypeDTO> listActivePackageTypes() {
        return packageTypeRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(PackageTypeDTO::fromEntity)
                .collect(Collectors.toList());
    }


    @Transactional
    public PackageTypeDTO createPackage(PackageTypeDTO packageTypeDTO) {
        if (packageTypeRepository.existsByName(packageTypeDTO.getName())) {
            throw new InvalidOperationException("Bu isimde bir paket tipi zaten mevcut: " + packageTypeDTO.getName());
        }
        PackageType packageType = packageTypeDTO.toEntity();
        packageType.setActive(true); // New package types are active by default
        PackageType savedPackage = packageTypeRepository.save(packageType);
        return PackageTypeDTO.fromEntity(savedPackage);
    }

    @Transactional
    public PackageTypeDTO updatePackage(int id, PackageTypeDTO packageTypeDTO) {
        PackageType packageType = packageTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paket tipi bulunamadı: ID " + id));

        if (!packageType.getName().equalsIgnoreCase(packageTypeDTO.getName()) &&
                packageTypeRepository.existsByName(packageTypeDTO.getName())) {
            throw new InvalidOperationException("Bu isimde başka bir paket tipi zaten mevcut: " + packageTypeDTO.getName());
        }

        packageTypeDTO.updateEntity(packageType);
        PackageType updatedPackage = packageTypeRepository.save(packageType);
        return PackageTypeDTO.fromEntity(updatedPackage);
    }

    @Transactional
    public void deletePackage(int id) {
        PackageType packageType = packageTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paket tipi bulunamadı: ID " + id));

        if (memberPackageRepository.existsByPackageTypeIdAndActiveTrue(id)) {
            throw new InvalidOperationException(
                    "Bu paket tipi aktif üye paketlerinde kullanıldığı için silinemez. Önce paket tipini pasif yapın veya ilgili üye paketlerini sonlandırın.");
        }
        packageType.setActive(false);
        packageTypeRepository.save(packageType);
    }

    public List<PackageTypeDTO> listEducationPackages() {
        return packageTypeRepository.findByIsEducationPackageTrueAndIsActiveTrue()
                .stream()
                .map(PackageTypeDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<PackageTypeDTO> listOtherPackages() {
        return packageTypeRepository.findByIsEducationPackageFalseAndIsActiveTrue()
                .stream()
                .map(PackageTypeDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public PackageTypeDTO getPackageById(int id) {
        PackageType packageType = packageTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paket tipi bulunamadı: ID " + id));
        return PackageTypeDTO.fromEntity(packageType);
    }

    // --- MemberPackage Management (Member/System) ---
    @Transactional
    public MemberPackageDTO createMemberPackage(MemberPackageDTO memberPackageDTO) {
        Member member = memberRepository.findById(memberPackageDTO.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Üye bulunamadı: ID " + memberPackageDTO.getMemberId()));

        if (member.getStatus() != StatusEnum.ACTIVE) {
            throw new InvalidOperationException("Paket satın alabilmek için üye durumu AKTİF olmalıdır. Mevcut durum: " + member.getStatus());
        }

        PackageType packageType = packageTypeRepository.findById(memberPackageDTO.getPackageTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Paket tipi bulunamadı: ID " + memberPackageDTO.getPackageTypeId()));

        if (!packageType.isActive()) {
            throw new InvalidOperationException("Bu paket tipi ("+ packageType.getName() +") şu anda aktif değildir ve satın alınamaz.");
        }

        if (packageType.isRequiresSwimmingAbility()) {
            try {
                if (!memberService.hasSwimmingAbility(member.getId())) {
                    throw new InvalidOperationException("Bu paket yüzme bilgisi gerektirir ve üyenin yüzme bilgisi doğrulanmamıştır/yoktur.");
                }
            } catch (Exception e) { // Catch specific exceptions if MemberService throws them
                throw new InvalidOperationException("Üyenin yüzme bilgisi kontrol edilirken hata oluştu: " + e.getMessage());
            }
        }

        // Determine the poolId for the canBuyPackage check and for the new MemberPackage
        Integer effectivePoolIdForCheck;
        int poolIdForNewPackage;

        if (packageType.isMultiplePools()) {
            effectivePoolIdForCheck = null; // For canBuyPackage, null signifies checking for multi-pool packages
            poolIdForNewPackage = 0;     // Store 0 in MemberPackage.poolId for multi-pool
        } else {
            if (memberPackageDTO.getPoolId() <= 0) {
                throw new InvalidOperationException("Bu paket tipi için geçerli bir havuz seçilmelidir.");
            }
            poolRepository.findById(memberPackageDTO.getPoolId()).orElseThrow(() -> new IllegalArgumentException("Belirtilen havuz ID'si geçersiz: " + memberPackageDTO.getPoolId()));
            effectivePoolIdForCheck = memberPackageDTO.getPoolId();
            poolIdForNewPackage = memberPackageDTO.getPoolId();
        }

        if (!canBuyPackage(member.getId(), effectivePoolIdForCheck)) {
            throw new InvalidOperationException("Üye, mevcut aktif paket kısıtlamaları nedeniyle bu paketi satın alamaz.");
        }

        MemberPackage memberPackage = new MemberPackage();
        memberPackage.setMemberId(member.getId());
        memberPackage.setPackageTypeId(packageType.getId());
        memberPackage.setPoolId(poolIdForNewPackage); // Set based on logic above
        memberPackage.setPurchaseDate(memberPackageDTO.getPurchaseDate() != null ? memberPackageDTO.getPurchaseDate() : LocalDateTime.now());
        memberPackage.setSessionsRemaining(packageType.getSessionLimit());

        // Default to PENDING and inactive; activate upon payment.
        MemberPackagePaymentStatusEnum paymentStatus = memberPackageDTO.getPaymentStatus() != null ?
                memberPackageDTO.getPaymentStatus() : MemberPackagePaymentStatusEnum.PENDING;
        memberPackage.setPaymentStatus(paymentStatus);

        if (paymentStatus == MemberPackagePaymentStatusEnum.COMPLETED) {
            memberPackage.setActive(true);
            memberPackage.setPaymentDate(memberPackageDTO.getPaymentDate() != null ? memberPackageDTO.getPaymentDate() : LocalDateTime.now());
        } else {
            memberPackage.setActive(false);
            memberPackage.setPaymentDate(null);
        }

        MemberPackage savedMemberPackage = memberPackageRepository.save(memberPackage);
        return enrichDTO(savedMemberPackage);
    }

    public List<MemberPackageDTO> getActiveMemberPackages(int memberId) {
        // "Active" means payment completed and the active flag is true.
        List<MemberPackage> activePaidPackages = memberPackageRepository.findByMemberIdAndActiveTrueAndPaymentStatus(
                memberId, MemberPackagePaymentStatusEnum.COMPLETED);
        return activePaidPackages.stream()
                .map(this::enrichDTO)
                .collect(Collectors.toList());
    }

    public List<MemberPackageDTO> getPreviousMemberPackages(int memberId) {
        // Previous: Payment was completed, but package is no longer active OR no sessions left.
        List<MemberPackage> packages = memberPackageRepository.findByMemberIdAndPaymentStatus(
                memberId, MemberPackagePaymentStatusEnum.COMPLETED);
        return packages.stream()
                .filter(mp -> !mp.isActive() || mp.getSessionsRemaining() <= 0)
                .map(this::enrichDTO)
                .collect(Collectors.toList());
    }

    public List<MemberPackageDTO> getMemberPackages(int memberId) { // All packages for a member
        List<MemberPackage> packages = memberPackageRepository.findByMemberId(memberId);
        return packages.stream()
                .map(this::enrichDTO)
                .collect(Collectors.toList());
    }

    /**
     * Determines if a member can buy a new package.
     * A member can buy a new package if:
     * 1. They have NO active, paid packages at all.
     * 2. OR, if they are trying to buy a package for a SPECIFIC pool (newPackagePoolId is not null and > 0),
     *    they must NOT have an existing active, paid package for THAT SAME specific pool.
     * 3. OR, if they are trying to buy a MULTI-POOL package (newPackagePoolId is null or 0),
     *    they must NOT have an existing active, paid MULTI-POOL package.
     * (A member can have one active specific-pool package and one active multi-pool package concurrently,
     *  but not two specific-pool packages for the same pool, nor two multi-pool packages).
     */
    public boolean canBuyPackage(int memberId, Integer newPackagePoolId) {
        List<MemberPackage> activePaidPackages = memberPackageRepository
                .findByMemberIdAndActiveTrueAndPaymentStatus(memberId, MemberPackagePaymentStatusEnum.COMPLETED);

        if (activePaidPackages.isEmpty()) {
            return true; // No active paid packages, can always buy.
        }

        if (newPackagePoolId != null && newPackagePoolId > 0) {
            // Trying to buy a package for a specific pool.
            // Check if there's an existing active, paid package for THIS specific pool.
            boolean hasActivePackageForThisPool = activePaidPackages.stream()
                    .anyMatch(pkg -> pkg.getPoolId() == newPackagePoolId); // Compare with the exact poolId
            return !hasActivePackageForThisPool; // Can buy if no active package for THIS specific pool.
        } else {
            // Trying to buy a multi-pool package (newPackagePoolId is null, indicating a check for multi-pool type).
            // Check if there's an existing active, paid multi-pool package.
            // A multi-pool MemberPackage instance has its poolId set to 0.
            boolean hasActiveMultiPoolPackage = activePaidPackages.stream()
                    .anyMatch(pkg -> pkg.getPoolId() == 0);
            return !hasActiveMultiPoolPackage; // Can buy if no active multi-pool package.
        }
    }

}