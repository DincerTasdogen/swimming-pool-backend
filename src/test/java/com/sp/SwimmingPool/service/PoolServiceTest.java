package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.Pool;
import com.sp.SwimmingPool.repos.PoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Nested;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PoolServiceTest {

    @Mock
    private PoolRepository poolRepository;

    @InjectMocks
    private PoolService poolService;

    private Pool pool1;
    private Pool pool2_inactive;
    private Pool pool3_differentCity;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        pool1 = new Pool();
        pool1.setId(1);
        pool1.setName("City Central Pool");
        pool1.setLocation("123 Main St");
        pool1.setCity("Metropolis");
        pool1.setLatitude(34.0522);
        pool1.setLongitude(-118.2437);
        pool1.setDepth(2.5);
        pool1.setCapacity(100);
        pool1.setOpenAt("08:00");
        pool1.setCloseAt("20:00");
        pool1.setDescription("Main public pool");
        pool1.setImagePath("/images/pool1.jpg");
        pool1.setFeatures(List.of("Lockers", "Showers", "Cafe"));
        pool1.setActive(true);
        pool1.setCreatedAt(now.minusDays(10));
        pool1.setUpdatedAt(now.minusDays(1));

        pool2_inactive = new Pool();
        pool2_inactive.setId(2);
        pool2_inactive.setName("Old Town Pool");
        pool2_inactive.setLocation("456 Old Rd");
        pool2_inactive.setCity("Metropolis"); // Same city as pool1
        pool2_inactive.setDepth(1.8);
        pool2_inactive.setCapacity(50);
        pool2_inactive.setOpenAt("10:00");
        pool2_inactive.setCloseAt("18:00");
        pool2_inactive.setActive(false); // Inactive
        pool2_inactive.setCreatedAt(now.minusMonths(6));
        pool2_inactive.setUpdatedAt(now.minusMonths(1));

        pool3_differentCity = new Pool();
        pool3_differentCity.setId(3);
        pool3_differentCity.setName("Suburb Wellness Center");
        pool3_differentCity.setLocation("789 Suburb Ave");
        pool3_differentCity.setCity("Suburbia"); // Different city
        pool3_differentCity.setDepth(2.0);
        pool3_differentCity.setCapacity(75);
        pool3_differentCity.setOpenAt("07:00");
        pool3_differentCity.setCloseAt("21:00");
        pool3_differentCity.setActive(true);
        pool3_differentCity.setCreatedAt(now.minusWeeks(2));
        pool3_differentCity.setUpdatedAt(now.minusDays(3));
    }

    @Test
    void getRandomPools_callsRepositoryWithCount() {
        int count = 3;
        List<Pool> expectedPools = List.of(pool1, pool3_differentCity); // Example
        when(poolRepository.findRandomPools(count)).thenReturn(expectedPools);

        List<Pool> actualPools = poolService.getRandomPools(count);

        assertEquals(expectedPools, actualPools);
        verify(poolRepository).findRandomPools(count);
    }

    @Test
    void save_callsRepositorySaveAndReturnsPool() {
        Pool newPool = new Pool();
        newPool.setName("New Community Pool");
        newPool.setCity("Newville");
        newPool.setDepth(2.0);
        newPool.setCapacity(75);
        newPool.setOpenAt("07:00");
        newPool.setCloseAt("21:00");
        newPool.setActive(true);


        when(poolRepository.save(any(Pool.class))).thenAnswer(invocation -> {
            Pool p = invocation.getArgument(0);
            p.setId(4); // Simulate ID generation
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            return p;
        });

        Pool savedPool = poolService.save(newPool);

        assertNotNull(savedPool);
        assertEquals(4, savedPool.getId());
        assertEquals("New Community Pool", savedPool.getName());
        assertNotNull(savedPool.getCreatedAt());
        assertNotNull(savedPool.getUpdatedAt());

        ArgumentCaptor<Pool> poolCaptor = ArgumentCaptor.forClass(Pool.class);
        verify(poolRepository).save(poolCaptor.capture());
        assertEquals("New Community Pool", poolCaptor.getValue().getName());
    }

    @Test
    void findAll_returnsListOfPools() {
        when(poolRepository.findAll()).thenReturn(List.of(pool1, pool2_inactive, pool3_differentCity));
        List<Pool> result = poolService.findAll();
        assertEquals(3, result.size());
        assertTrue(result.contains(pool1));
    }

    @Test
    void findAll_noPools_returnsEmptyList() {
        when(poolRepository.findAll()).thenReturn(Collections.emptyList());
        List<Pool> result = poolService.findAll();
        assertTrue(result.isEmpty());
    }

    @Test
    void delete_callsRepositoryDeleteById() {
        int poolIdToDelete = pool1.getId();
        doNothing().when(poolRepository).deleteById(poolIdToDelete);
        poolService.delete(poolIdToDelete);
        verify(poolRepository).deleteById(poolIdToDelete);
    }

    @Test
    void findById_poolExists_returnsOptionalOfPool() {
        when(poolRepository.findById(pool1.getId())).thenReturn(Optional.of(pool1));
        Optional<Pool> result = poolService.findById(pool1.getId());
        assertTrue(result.isPresent());
        assertEquals(pool1.getName(), result.get().getName());
    }

    @Test
    void findById_poolNotExists_returnsEmptyOptional() {
        int nonExistentId = 99;
        when(poolRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        Optional<Pool> result = poolService.findById(nonExistentId);
        assertTrue(result.isEmpty());
    }

    @Nested
    class FilterPoolsTests {
        private List<Pool> allPoolsList;

        @BeforeEach
        void setUpFilter() {
            allPoolsList = new ArrayList<>(List.of(pool1, pool2_inactive, pool3_differentCity));
            when(poolRepository.findAll()).thenReturn(allPoolsList);
        }

        @Test
        void filterPools_noFilters_returnsAllPools() {
            List<Pool> result = poolService.filterPools(null, null);
            assertEquals(3, result.size());
            assertTrue(result.containsAll(allPoolsList));
        }

        @Test
        void filterPools_byCityOnly_returnsMatchingCityPools() {
            List<Pool> result = poolService.filterPools("Metropolis", null);
            assertEquals(2, result.size());
            assertTrue(result.contains(pool1));
            assertTrue(result.contains(pool2_inactive));
            assertFalse(result.contains(pool3_differentCity));
        }

        @Test
        void filterPools_byEmptyCity_returnsAllPools() {
            List<Pool> result = poolService.filterPools("", null);
            assertEquals(3, result.size());
        }

        @Test
        void filterPools_byCity_caseInsensitive() {
            List<Pool> result = poolService.filterPools("mEtRoPoLiS", null);
            assertEquals(2, result.size());
            assertTrue(result.contains(pool1));
            assertTrue(result.contains(pool2_inactive));
        }

        @Test
        void filterPools_byIsActiveTrueOnly_returnsActivePools() {
            List<Pool> result = poolService.filterPools(null, true);
            assertEquals(2, result.size());
            assertTrue(result.contains(pool1));
            assertTrue(result.contains(pool3_differentCity));
            assertFalse(result.contains(pool2_inactive));
        }

        @Test
        void filterPools_byIsActiveFalseOnly_returnsInactivePools() {
            List<Pool> result = poolService.filterPools(null, false);
            assertEquals(1, result.size());
            assertTrue(result.contains(pool2_inactive));
            assertFalse(result.contains(pool1));
            assertFalse(result.contains(pool3_differentCity));
        }

        @Test
        void filterPools_byCityAndIsActiveTrue_returnsMatchingActivePoolsInCity() {
            List<Pool> result = poolService.filterPools("Metropolis", true);
            assertEquals(1, result.size());
            assertTrue(result.contains(pool1));
            assertFalse(result.contains(pool2_inactive)); // Inactive
            assertFalse(result.contains(pool3_differentCity)); // Different city
        }

        @Test
        void filterPools_byCityAndIsActiveFalse_returnsMatchingInactivePoolsInCity() {
            List<Pool> result = poolService.filterPools("Metropolis", false);
            assertEquals(1, result.size());
            assertTrue(result.contains(pool2_inactive));
            assertFalse(result.contains(pool1)); // Active
        }

        @Test
        void filterPools_noMatchingCity_returnsEmptyList() {
            List<Pool> result = poolService.filterPools("NonExistentCity", null);
            assertTrue(result.isEmpty());
        }

        @Test
        void filterPools_noMatchingActiveStatusInCity_returnsEmptyList() {
            // Metropolis has one active (pool1) and one inactive (pool2_inactive)
            // If we filter Metropolis and active=false, we get pool2_inactive
            // If we filter Suburbia and active=false, we should get empty
            List<Pool> result = poolService.filterPools("Suburbia", false);
            assertTrue(result.isEmpty());
        }
    }
}