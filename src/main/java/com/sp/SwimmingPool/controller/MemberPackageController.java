package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.MemberPackageDTO;
import com.sp.SwimmingPool.service.PackageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/MemberPackages")
public class MemberPackageController {
    private final PackageService packageService;

    public MemberPackageController(PackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping("/all/{memberId}")
    public ResponseEntity<List<MemberPackageDTO>> getAllMemberPackages(@PathVariable int memberId) {
        List<MemberPackageDTO> packages = packageService.getMemberPackages(memberId);
        return ResponseEntity.ok(packages);
    }

    @GetMapping("/active/{memberId}")
    public ResponseEntity<List<MemberPackageDTO>> getActiveMemberPackages(@PathVariable int memberId) {
        List<MemberPackageDTO> activePackages = packageService.getActiveMemberPackages(memberId);
        return ResponseEntity.ok(activePackages);
    }

    @GetMapping("/previous/{memberId}")
    public ResponseEntity<List<MemberPackageDTO>> getPreviousMemberPackages(@PathVariable int memberId) {
        List<MemberPackageDTO> previousPackages = packageService.getPreviousMemberPackages(memberId);
        return ResponseEntity.ok(previousPackages);
    }

    @GetMapping("/can-buy/{memberId}")
    public ResponseEntity<Boolean> canBuyPackage(
            @PathVariable int memberId,
            @RequestParam(required = false) Integer poolId) {
        boolean canBuy = packageService.canBuyPackage(memberId, poolId);
        return ResponseEntity.ok(canBuy);
    }

    @PostMapping
    public ResponseEntity<MemberPackageDTO> createMemberPackage(@RequestBody MemberPackageDTO memberPackageDTO) {
        MemberPackageDTO createdPackage = packageService.createMemberPackage(memberPackageDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPackage);
    }
}
