package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.PoolDTO;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.Pool;
import com.sp.SwimmingPool.repos.PoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PoolService {
    @Autowired
    private PoolRepository poolRepository;

    public PoolDTO createPool(PoolDTO poolDTO) {
        Pool pool = new Pool();
        pool.setName(poolDTO.getName());
        pool.setLocation(poolDTO.getLocation());
        pool.setLatitude(poolDTO.getLatitude());
        pool.setLongitude(poolDTO.getLongitude());
        pool.setDepth(poolDTO.getDepth());
        pool.setCapacity(poolDTO.getCapacity());
        pool.setOpenAt(poolDTO.getOpenAt());
        pool.setCloseAt(poolDTO.getCloseAt());
        pool.setActive(poolDTO.isActive());

        poolRepository.save(pool);
        return poolDTO;
    }
    public List<PoolDTO> listAllPools() {
        List<Pool> pools = poolRepository.findAll();
        List<PoolDTO> poolDTOs = new ArrayList<>();
        for (Pool pool : pools) {
            PoolDTO poolDTO = new PoolDTO();
            poolDTO.setId(pool.getId());
            poolDTO.setName(pool.getName());
            poolDTO.setLocation(pool.getLocation());
            poolDTO.setLatitude(pool.getLatitude());
            poolDTO.setLongitude(pool.getLongitude());
            poolDTO.setDepth(pool.getDepth());
            poolDTO.setCapacity(pool.getCapacity());
            poolDTO.setOpenAt(pool.getOpenAt());
            poolDTO.setCloseAt(pool.getCloseAt());
            poolDTO.setActive(pool.isActive());
            poolDTOs.add(poolDTO);
        }
        return poolDTOs;
    }
    public PoolDTO updatePool(int id,PoolDTO poolDTO){
        Pool pool = poolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + id));
        pool.setName(poolDTO.getName());
        pool.setLocation(poolDTO.getLocation());
        pool.setLatitude(poolDTO.getLatitude());
        pool.setLongitude(poolDTO.getLongitude());
        pool.setDepth(poolDTO.getDepth());
        pool.setCapacity(poolDTO.getCapacity());
        pool.setOpenAt(poolDTO.getOpenAt());
        pool.setCloseAt(poolDTO.getCloseAt());
        pool.setActive(poolDTO.isActive());

        poolRepository.save(pool);
        return poolDTO;
    }

public void deletePool(int id){
    if (poolRepository.existsById(id)) {
        poolRepository.deleteById(id);
    } else {
        throw new RuntimeException("Member with id " + id + " not found");
    }
}
public PoolDTO getPoolDetails(int id){
    Pool pool = poolRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + id));
    PoolDTO poolDTO = new PoolDTO();
    poolDTO.setId(pool.getId());
    poolDTO.setName(pool.getName());
    poolDTO.setLocation(pool.getLocation());
    poolDTO.setLatitude(pool.getLatitude());
    poolDTO.setLongitude(pool.getLongitude());
    poolDTO.setDepth(pool.getDepth());
    poolDTO.setCapacity(pool.getCapacity());
    poolDTO.setOpenAt(pool.getOpenAt());
    poolDTO.setCloseAt(pool.getCloseAt());
    poolDTO.setActive(pool.isActive());

    return poolDTO;
}
}

