package com.sp.SwimmingPool;

import com.sp.SwimmingPool.dto.PackageTypeDTO;
import com.sp.SwimmingPool.repos.PackageTypeRepository;
import com.sp.SwimmingPool.service.PackageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalTime;

@SpringBootTest
public class PackageTypeTests {

    @Autowired
    private PackageTypeRepository packageTypeRepository;
    @Autowired
    private PackageService packageService;


    @Test
    public void CreatePackageType() {
        PackageTypeDTO pack = PackageTypeDTO.builder()
                .id(2)
                .name("Çoklu Tesis Serbest Yüzme Paketi")
                .description("Bu paket, eğitim veya sağlık amaçlı periyodik olarak yüzmek isteyen üyeler içindir.")
                .price(800.0)
                .isEducationPackage(false)
                .sessionLimit(18)
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(0, 0))
                .requiresSwimmingAbility(true)
                .build();

        packageService.updatePackage(2, pack);
    }
}
