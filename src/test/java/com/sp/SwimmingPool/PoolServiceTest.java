package com.sp.SwimmingPool;

import com.sp.SwimmingPool.model.entity.Pool;
import com.sp.SwimmingPool.repos.PoolRepository;
import com.sp.SwimmingPool.service.PoolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PoolServiceTest {

    private PoolRepository poolRepository;
    private PoolService poolService;

    @BeforeEach
    void setUp() {
        poolRepository = mock(PoolRepository.class);
        poolService = new PoolService(poolRepository);
    }

    @Test
    void testSavePool() {
        Pool pool = new Pool();
        pool.setName("Olympic Pool");

        when(poolRepository.save(pool)).thenReturn(pool);

        Pool saved = poolService.save(pool);

        assertEquals("Olympic Pool", saved.getName());
        verify(poolRepository).save(pool);
    }

    @Test
    void testFindAllPools() {
        Pool pool1 = new Pool();
        Pool pool2 = new Pool();
        when(poolRepository.findAll()).thenReturn(List.of(pool1, pool2));

        List<Pool> result = poolService.findAll();

        assertEquals(2, result.size());
        verify(poolRepository).findAll();
    }

    @Test
    void testDeletePool() {
        poolService.delete(1);
        verify(poolRepository).deleteById(1);
    }

    @Test
    void testFindById_Existing() {
        Pool pool = new Pool();
        pool.setId(42);
        when(poolRepository.findById(42)).thenReturn(Optional.of(pool));

        Optional<Pool> result = poolService.findById(42);

        assertTrue(result.isPresent());
        assertEquals(42, result.get().getId());
    }

    @Test
    void testFindById_NotFound() {
        when(poolRepository.findById(999)).thenReturn(Optional.empty());

        Optional<Pool> result = poolService.findById(999);

        assertFalse(result.isPresent());
    }

    @Test
    void testFilterPools_ByCityOnly() {
        Pool pool1 = createPool("Istanbul", true);
        Pool pool2 = createPool("Ankara", true);

        when(poolRepository.findAll()).thenReturn(List.of(pool1, pool2));

        List<Pool> filtered = poolService.filterPools("Istanbul", null);

        assertEquals(1, filtered.size());
        assertEquals("Istanbul", filtered.get(0).getCity());
    }

    @Test
    void testFilterPools_ByIsActiveOnly() {
        Pool active = createPool("Izmir", true);
        Pool inactive = createPool("Izmir", false);

        when(poolRepository.findAll()).thenReturn(List.of(active, inactive));

        List<Pool> filtered = poolService.filterPools(null, true);

        assertEquals(1, filtered.size());
        assertTrue(filtered.get(0).isActive());
    }

    @Test
    void testFilterPools_ByCityAndIsActive() {
        Pool p1 = createPool("Antalya", true);
        Pool p2 = createPool("Antalya", false);
        Pool p3 = createPool("Bursa", true);

        when(poolRepository.findAll()).thenReturn(List.of(p1, p2, p3));

        List<Pool> filtered = poolService.filterPools("Antalya", true);

        assertEquals(1, filtered.size());
        assertEquals("Antalya", filtered.get(0).getCity());
        assertTrue(filtered.get(0).isActive());
    }

    @Test
    void testFilterPools_NullParametersReturnsAll() {
        Pool p1 = createPool("A", true);
        Pool p2 = createPool("B", false);

        when(poolRepository.findAll()).thenReturn(List.of(p1, p2));

        List<Pool> filtered = poolService.filterPools(null, null);

        assertEquals(2, filtered.size());
    }

    @Test
    void testGetRandomPools() {
        Pool p1 = new Pool();
        Pool p2 = new Pool();
        when(poolRepository.findRandomPools(2)).thenReturn(List.of(p1, p2));

        List<Pool> result = poolService.getRandomPools(2);

        assertEquals(2, result.size());
        verify(poolRepository).findRandomPools(2);
    }

    @Test
    void testFeatureJsonConversion_Get() {
        Pool pool = new Pool();
        pool.setFeatures(List.of("Sauna", "Jacuzzi"));

        String json = pool.getFeaturesJson();
        assertTrue(json.contains("Sauna"));
        assertTrue(json.contains("Jacuzzi"));
    }

    @Test
    void testFeatureJsonConversion_Set() {
        Pool pool = new Pool();
        pool.setFeaturesJson("[\"Steam Room\", \"Heated\"]");

        List<String> features = pool.getFeatures();
        assertEquals(2, features.size());
        assertTrue(features.contains("Steam Room"));
        assertTrue(features.contains("Heated"));
    }

    private Pool createPool(String city, boolean isActive) {
        Pool pool = new Pool();
        pool.setCity(city);
        pool.setActive(isActive);
        return pool;
    }
}
