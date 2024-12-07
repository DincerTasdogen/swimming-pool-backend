package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.PoolDTO;
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

    public void addPool(PoolDTO poolDTO) {
        Pool pool = new Pool();
        pool.setId(poolDTO.getId());
        pool.setName(poolDTO.getName());
        pool.setLocation(poolDTO.getLocation());
        pool.setLatitude(poolDTO.getLatitude());
        pool.setLongitude(poolDTO.getLongitude());
        pool.setDepth(poolDTO.getDepth());
        pool.setCapacity(poolDTO.getCapacity());
        pool.setOpenAt(poolDTO.getOpenAt());
        pool.setCloseAt(poolDTO.getCloseAt());
        pool.setActive(poolDTO.isActive());
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


}

