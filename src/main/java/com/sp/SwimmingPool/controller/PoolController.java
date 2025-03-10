package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.model.entity.Pool;
import com.sp.SwimmingPool.dto.PoolDTO;
import com.sp.SwimmingPool.service.PoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pools")
public class PoolController {

    private final PoolService poolService;

    @Autowired
    public PoolController(PoolService poolService) {
        this.poolService = poolService;
    }

    @GetMapping
    public ResponseEntity<List<Pool>> getAllPools() {
        List<Pool> pools = poolService.findAll();
        return ResponseEntity.ok(pools);
    }

    @GetMapping("/random/{count}")
    public ResponseEntity<List<Pool>> getRandomPools(@PathVariable int count) {
        List<Pool> pools = poolService.getRandomPools(count);
        return ResponseEntity.ok(pools);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pool> getPoolById(@PathVariable int id) {
        Optional<Pool> pool = poolService.findById(id);
        return pool.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPool(@RequestBody PoolDTO poolDTO) {
        try {
            Pool newPool = convertToEntity(poolDTO);
            newPool.setCreatedAt(LocalDateTime.now());
            newPool.setUpdatedAt(LocalDateTime.now());

            Pool savedPool = poolService.save(newPool);
            return new ResponseEntity<>(savedPool, HttpStatus.CREATED);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to create pool: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePool(@PathVariable int id, @RequestBody PoolDTO poolDTO) {
        try {
            Optional<Pool> existingPool = poolService.findById(id);

            if (existingPool.isPresent()) {
                Pool pool = existingPool.get();

                // Log before update for debugging
                System.out.println("Before update - Features JSON: " + pool.getFeaturesJson());

                pool.setName(poolDTO.getName());
                pool.setLocation(poolDTO.getLocation());
                pool.setCity(poolDTO.getCity());
                pool.setLatitude(poolDTO.getLatitude());
                pool.setLongitude(poolDTO.getLongitude());
                pool.setDepth(poolDTO.getDepth());
                pool.setCapacity(poolDTO.getCapacity());
                pool.setOpenAt(poolDTO.getOpenAt());
                pool.setCloseAt(poolDTO.getCloseAt());
                pool.setActive(poolDTO.isActive());

                // Optional fields
                if (poolDTO.getDescription() != null) {
                    pool.setDescription(poolDTO.getDescription());
                }

                // Handle image path update if provided
                if (poolDTO.getImagePath() != null && !poolDTO.getImagePath().isEmpty()) {
                    pool.setImagePath(poolDTO.getImagePath());
                }

                // Update features - ensure we're always explicitly setting features
                pool.setFeatures(poolDTO.getFeatures() != null ? poolDTO.getFeatures() : new ArrayList<>());

                pool.setUpdatedAt(LocalDateTime.now());

                // Log after update for debugging
                System.out.println("After update - Features JSON: " + pool.getFeaturesJson());

                Pool updatedPool = poolService.save(pool);
                return ResponseEntity.ok(updatedPool);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to update pool: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePool(@PathVariable int id) {
        Optional<Pool> pool = poolService.findById(id);

        if (pool.isPresent()) {
            poolService.delete(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Pool>> filterPools(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean isActive) {

        List<Pool> filteredPools = poolService.filterPools(city, isActive);
        return ResponseEntity.ok(filteredPools);
    }

    private Pool convertToEntity(PoolDTO poolDTO) {
        Pool pool = new Pool();
        pool.setName(poolDTO.getName());
        pool.setLocation(poolDTO.getLocation());
        pool.setCity(poolDTO.getCity());
        pool.setLatitude(poolDTO.getLatitude());
        pool.setLongitude(poolDTO.getLongitude());
        pool.setDepth(poolDTO.getDepth());
        pool.setCapacity(poolDTO.getCapacity());
        pool.setOpenAt(poolDTO.getOpenAt());
        pool.setCloseAt(poolDTO.getCloseAt());
        pool.setActive(poolDTO.isActive());

        // Optional fields
        if (poolDTO.getDescription() != null) {
            pool.setDescription(poolDTO.getDescription());
        }

        // Handle image path if provided
        if (poolDTO.getImagePath() != null && !poolDTO.getImagePath().isEmpty()) {
            pool.setImagePath(poolDTO.getImagePath());
        }

        // Always explicitly set features, even if null
        pool.setFeatures(poolDTO.getFeatures() != null ? poolDTO.getFeatures() : new ArrayList<>());

        return pool;
    }
}