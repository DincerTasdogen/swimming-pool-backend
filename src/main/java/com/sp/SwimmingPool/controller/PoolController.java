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
import java.util.List;
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

    @GetMapping("/{id}")
    public ResponseEntity<Pool> getPoolById(@PathVariable int id) {
        Optional<Pool> pool = poolService.findById(id);
        return pool.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Pool> createPool(@RequestBody PoolDTO poolDTO) {
        Pool newPool = convertToEntity(poolDTO);
        newPool.setCreatedAt(LocalDateTime.now());
        newPool.setUpdatedAt(LocalDateTime.now());

        Pool savedPool = poolService.save(newPool);
        return new ResponseEntity<>(savedPool, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Pool> updatePool(@PathVariable int id, @RequestBody PoolDTO poolDTO) {
        Optional<Pool> existingPool = poolService.findById(id);

        if (existingPool.isPresent()) {
            Pool pool = existingPool.get();

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
            if (poolDTO.getFeatures() != null) {
                pool.setFeatures(poolDTO.getFeatures());
            }

            pool.setUpdatedAt(LocalDateTime.now());

            Pool updatedPool = poolService.save(pool);
            return ResponseEntity.ok(updatedPool);
        } else {
            return ResponseEntity.notFound().build();
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
        if (poolDTO.getFeatures() != null) {
            pool.setFeatures(poolDTO.getFeatures());
        }

        return pool;
    }
}