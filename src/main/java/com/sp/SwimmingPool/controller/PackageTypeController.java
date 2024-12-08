package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.PackageTypeDTO;
import com.sp.SwimmingPool.service.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class PackageTypeController {

    @Autowired
    private PackageService packageService;

    @GetMapping
    public ResponseEntity<List<PackageTypeDTO>> getAllPackageTypes() {
        try {
            List<PackageTypeDTO> packages = packageService.listPackageTypes();
            return new ResponseEntity<>(packages, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PackageTypeDTO> createPackageType(@RequestBody PackageTypeDTO packageTypeDTO) {
        try {
            PackageTypeDTO createdPackage = packageService.createPackage(packageTypeDTO);
            return new ResponseEntity<>(createdPackage, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PackageTypeDTO> updatePackageType(@PathVariable int id, @RequestBody PackageTypeDTO packageTypeDTO) {
        try {
            PackageTypeDTO updatedPackage = packageService.updatePackage(id, packageTypeDTO);
            return new ResponseEntity<>(updatedPackage, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HttpStatus> deletePackageType(@PathVariable int id) {
        try {
            packageService.deletePackage(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/education")
    public ResponseEntity<List<PackageTypeDTO>> getEducationPackages() {
        try {
            List<PackageTypeDTO> educationPackages = packageService.listEducationPackages();
            if (educationPackages.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(educationPackages, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/other")
    public ResponseEntity<List<PackageTypeDTO>> getOtherPackages() {
        try {
            List<PackageTypeDTO> nonEducationPackages = packageService.listOtherPackages();
            if (nonEducationPackages.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(nonEducationPackages, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}