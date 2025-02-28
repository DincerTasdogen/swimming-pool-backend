package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.Pool;
import com.sp.SwimmingPool.repos.PoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PoolService {

    private final PoolRepository poolRepository;

    @Autowired
    public PoolService(PoolRepository poolRepository) {
        this.poolRepository = poolRepository;
    }

    public Pool save(Pool pool) {
        return poolRepository.save(pool);
    }
    public List<Pool> findAll() {
        return poolRepository.findAll();
    }

    public void delete(int id) {
        poolRepository.deleteById(id);
    }

    public Optional<Pool> findById(int id) {
        return poolRepository.findById(id);
    }

    public List<Pool> filterPools(String city, Boolean isActive) {
        List<Pool> allPools = poolRepository.findAll();

        return allPools.stream()
                .filter(pool -> city == null || city.isEmpty() || pool.getCity().equalsIgnoreCase(city))
                .filter(pool -> isActive == null || pool.isActive() == isActive)
                .collect(Collectors.toList());
    }
}

