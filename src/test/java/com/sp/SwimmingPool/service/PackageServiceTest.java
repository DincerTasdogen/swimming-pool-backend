package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.MemberPackageDTO;
import com.sp.SwimmingPool.dto.PackageTypeDTO;
import com.sp.SwimmingPool.exception.InvalidOperationException;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.MemberPackage;
import com.sp.SwimmingPool.model.entity.PackageType;
import com.sp.SwimmingPool.model.entity.Pool;
import com.sp.SwimmingPool.model.enums.MemberGenderEnum;
import com.sp.SwimmingPool.model.enums.MemberPackagePaymentStatusEnum;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.model.enums.SwimmingLevelEnum;
import com.sp.SwimmingPool.repos.MemberPackageRepository;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.repos.PackageTypeRepository;
import com.sp.SwimmingPool.repos.PoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackageServiceTest {

    @Mock
    private PackageTypeRepository packageTypeRepository;
    @Mock
    private MemberPackageRepository memberPackageRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PoolRepository poolRepository;
    @Mock
    private MemberService memberService; // For hasSwimmingAbility

    @InjectMocks
    private PackageService packageService;

    private PackageType packageType1_active_singlePool_requiresSwim;
    private PackageType packageType2_active_multiPool_noSwim;
    private PackageType packageType3_inactive;

    private Member member1_active_canSwim;
    private Member member2_pending_cannotSwim;
    private Member member3_active_cannotSwim;


    private Pool pool1;
    private Pool pool2;


    @BeforeEach
    void setUpPackageTypesAndPools() {
        // Package Types
        packageType1_active_singlePool_requiresSwim = new PackageType();
        packageType1_active_singlePool_requiresSwim.setId(1);
        packageType1_active_singlePool_requiresSwim.setName("Gold Package");
        packageType1_active_singlePool_requiresSwim.setDescription("Premium access, requires swim");
        packageType1_active_singlePool_requiresSwim.setSessionLimit(30);
        packageType1_active_singlePool_requiresSwim.setPrice(100.00);
        packageType1_active_singlePool_requiresSwim.setStartTime(LocalTime.of(9, 0));
        packageType1_active_singlePool_requiresSwim.setEndTime(LocalTime.of(21, 0));
        packageType1_active_singlePool_requiresSwim.setEducationPackage(false);
        packageType1_active_singlePool_requiresSwim.setRequiresSwimmingAbility(true);
        packageType1_active_singlePool_requiresSwim.setMultiplePools(false);
        packageType1_active_singlePool_requiresSwim.setActive(true);

        packageType2_active_multiPool_noSwim = new PackageType();
        packageType2_active_multiPool_noSwim.setId(2);
        packageType2_active_multiPool_noSwim.setName("Multi-Pool Pass");
        packageType2_active_multiPool_noSwim.setDescription("Access to all pools, no swim needed");
        packageType2_active_multiPool_noSwim.setSessionLimit(20);
        packageType2_active_multiPool_noSwim.setPrice(150.00);
        packageType2_active_multiPool_noSwim.setStartTime(LocalTime.of(6, 0));
        packageType2_active_multiPool_noSwim.setEndTime(LocalTime.of(23, 0));
        packageType2_active_multiPool_noSwim.setEducationPackage(false);
        packageType2_active_multiPool_noSwim.setRequiresSwimmingAbility(false);
        packageType2_active_multiPool_noSwim.setMultiplePools(true);
        packageType2_active_multiPool_noSwim.setActive(true);

        packageType3_inactive = new PackageType();
        packageType3_inactive.setId(3);
        packageType3_inactive.setName("Bronze Package (Old)");
        packageType3_inactive.setDescription("Old basic access");
        packageType3_inactive.setSessionLimit(10);
        packageType3_inactive.setPrice(25.00);
        packageType3_inactive.setActive(false); // Inactive

        // Members
        member1_active_canSwim = Member.builder()
                .name("Active")
                .surname("Swimmer")
                .email("active@swimmer.com")
                .status(StatusEnum.ACTIVE)
                .canSwim(true)
                .swimmingLevel(SwimmingLevelEnum.ADVANCED)
                .identityNumber("111")
                .gender(MemberGenderEnum.MALE)
                .birthDate(LocalDate.now())
                .build();

        member2_pending_cannotSwim = Member.builder()
                .name("Pending")
                .surname("NonSwimmer")
                .email("pending@nonswimmer.com")
                .status(StatusEnum.PENDING_HEALTH_REPORT)
                .canSwim(false)
                .swimmingLevel(SwimmingLevelEnum.NONE)
                .identityNumber("222")
                .gender(MemberGenderEnum.FEMALE)
                .birthDate(LocalDate.now())
                .build();

        member3_active_cannotSwim = Member.builder()
                .name("Active")
                .surname("NonSwimmer")
                .email("active@nonswimmer.com")
                .status(StatusEnum.ACTIVE)
                .canSwim(false)
                .swimmingLevel(SwimmingLevelEnum.NONE)
                .identityNumber("333")
                .gender(MemberGenderEnum.OTHER)
                .birthDate(LocalDate.now())
                .build();


        // Pools
        pool1 = new Pool();
        pool1.setId(201);
        pool1.setName("Main City Pool");
        pool1.setCity("Metropolis");

        pool2 = new Pool();
        pool2.setId(202);
        pool2.setName("Suburban Rec Center");
        pool2.setCity("Suburbia");

        // MemberPackageDTOs for creation

        // poolId is 0 for multi-pool in DTO if that's the convention, or not set
    }


    @Nested
    class PackageTypeManagementTests {
        private PackageType packageType1;
        private PackageType packageType2_inactive;
        private PackageTypeDTO packageTypeDTO1;

        @BeforeEach
        void setUp() { // Renamed to avoid conflict with outer BeforeEach
            packageType1 = new PackageType();
            packageType1.setId(1);
            packageType1.setName("Gold Package");
            packageType1.setDescription("Premium access");
            packageType1.setSessionLimit(30);
            packageType1.setPrice(100.00);
            packageType1.setStartTime(LocalTime.of(9, 0));
            packageType1.setEndTime(LocalTime.of(21, 0));
            packageType1.setEducationPackage(false);
            packageType1.setRequiresSwimmingAbility(true);
            packageType1.setMultiplePools(false);
            packageType1.setActive(true);

            packageTypeDTO1 = PackageTypeDTO.fromEntity(packageType1);

            packageType2_inactive = new PackageType();
            packageType2_inactive.setId(2);
            packageType2_inactive.setName("Silver Package - Inactive");
            packageType2_inactive.setDescription("Standard access");
            packageType2_inactive.setSessionLimit(15);
            packageType2_inactive.setPrice(50.00);
            packageType2_inactive.setStartTime(LocalTime.of(10, 0));
            packageType2_inactive.setEndTime(LocalTime.of(20, 0));
            packageType2_inactive.setEducationPackage(false);
            packageType2_inactive.setRequiresSwimmingAbility(false);
            packageType2_inactive.setMultiplePools(true);
            packageType2_inactive.setActive(false);
        }

        @Test
        void listPackageTypes_returnsAllSorted() {
            when(packageTypeRepository.findAllByOrderByIsActiveDescNameAsc())
                    .thenReturn(List.of(packageType1, packageType2_inactive));

            List<PackageTypeDTO> result = packageService.listPackageTypes();

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("Gold Package", result.get(0).getName());
            assertEquals("Silver Package - Inactive", result.get(1).getName());
        }

        @Test
        void listActivePackageTypes_returnsOnlyActive() {
            when(packageTypeRepository.findByIsActiveTrueOrderByNameAsc())
                    .thenReturn(List.of(packageType1));

            List<PackageTypeDTO> result = packageService.listActivePackageTypes();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Gold Package", result.getFirst().getName());
            assertTrue(result.getFirst().isActive());
        }

        @Test
        void createPackage_newName_success() {
            PackageTypeDTO newPackageDTO = new PackageTypeDTO();
            newPackageDTO.setName("Bronze Package");
            // ... (set other fields as in previous test)
            newPackageDTO.setActive(true); // DTO might carry this, service ensures it

            when(packageTypeRepository.existsByName("Bronze Package")).thenReturn(false);
            when(packageTypeRepository.save(any(PackageType.class))).thenAnswer(invocation -> {
                PackageType pt = invocation.getArgument(0);
                pt.setId(3);
                return pt;
            });

            PackageTypeDTO result = packageService.createPackage(newPackageDTO);

            assertNotNull(result);
            assertEquals("Bronze Package", result.getName());
            assertTrue(result.isActive());
            assertEquals(3, result.getId());

            ArgumentCaptor<PackageType> captor = ArgumentCaptor.forClass(PackageType.class);
            verify(packageTypeRepository).save(captor.capture());
            assertTrue(captor.getValue().isActive());
        }

        @Test
        void createPackage_nameExists_throwsInvalidOperationException() {
            when(packageTypeRepository.existsByName(packageTypeDTO1.getName())).thenReturn(true);

            InvalidOperationException exception = assertThrows(InvalidOperationException.class, () -> packageService.createPackage(packageTypeDTO1));
            assertEquals("Bu isimde bir paket tipi zaten mevcut: " + packageTypeDTO1.getName(), exception.getMessage());
        }

        @Test
        void updatePackage_packageExists_success() {
            int packageId = packageType1.getId();
            PackageTypeDTO updatedDTO = PackageTypeDTO.fromEntity(packageType1);
            updatedDTO.setDescription("Updated Premium Access");
            updatedDTO.setActive(false);

            when(packageTypeRepository.findById(packageId)).thenReturn(Optional.of(packageType1));
            when(packageTypeRepository.save(any(PackageType.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PackageTypeDTO result = packageService.updatePackage(packageId, updatedDTO);

            assertNotNull(result);
            assertEquals("Updated Premium Access", result.getDescription());
            assertFalse(result.isActive());
        }

        @Test
        void updatePackage_packageNotFound_throwsIllegalArgumentException() {
            int nonExistentId = 99;
            when(packageTypeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> packageService.updatePackage(nonExistentId, packageTypeDTO1));
        }

        @Test
        void updatePackage_nameChangeToExisting_throwsInvalidOperationException() {
            int packageId = packageType1.getId();
            PackageTypeDTO updatedDTO = PackageTypeDTO.fromEntity(packageType1);
            updatedDTO.setName(packageType2_inactive.getName()); // Change to existing name

            when(packageTypeRepository.findById(packageId)).thenReturn(Optional.of(packageType1));
            when(packageTypeRepository.existsByName(packageType2_inactive.getName())).thenReturn(true);

            assertThrows(InvalidOperationException.class, () -> packageService.updatePackage(packageId, updatedDTO));
        }

        @Test
        void deletePackage_packageExists_noActiveMemberPackages_setsInactive() {
            int packageId = packageType1.getId();
            when(packageTypeRepository.findById(packageId)).thenReturn(Optional.of(packageType1));
            when(memberPackageRepository.existsByPackageTypeIdAndActiveTrue(packageId)).thenReturn(false);
            when(packageTypeRepository.save(any(PackageType.class))).thenAnswer(invocation -> invocation.getArgument(0));

            packageService.deletePackage(packageId);

            ArgumentCaptor<PackageType> captor = ArgumentCaptor.forClass(PackageType.class);
            verify(packageTypeRepository).save(captor.capture());
            assertFalse(captor.getValue().isActive());
        }

        @Test
        void deletePackage_packageNotFound_throwsIllegalArgumentException() {
            int nonExistentId = 99;
            when(packageTypeRepository.findById(nonExistentId)).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class, () -> packageService.deletePackage(nonExistentId));
        }

        @Test
        void deletePackage_activeMemberPackagesExist_throwsInvalidOperationException() {
            int packageId = packageType1.getId();
            when(packageTypeRepository.findById(packageId)).thenReturn(Optional.of(packageType1));
            when(memberPackageRepository.existsByPackageTypeIdAndActiveTrue(packageId)).thenReturn(true);

            assertThrows(InvalidOperationException.class, () -> packageService.deletePackage(packageId));
            verify(packageTypeRepository, never()).save(any(PackageType.class));
        }

        @Test
        void listEducationPackages_returnsOnlyActiveEducationPackages() {
            PackageType eduActive = new PackageType(); eduActive.setEducationPackage(true); eduActive.setActive(true); eduActive.setName("Edu1");
            when(packageTypeRepository.findByIsEducationPackageTrueAndIsActiveTrue()).thenReturn(List.of(eduActive));
            List<PackageTypeDTO> result = packageService.listEducationPackages();
            assertEquals(1, result.size());
            assertEquals("Edu1", result.getFirst().getName());
        }

        @Test
        void listOtherPackages_returnsOnlyActiveNonEducationPackages() {
            PackageType otherActive = new PackageType(); otherActive.setEducationPackage(false); otherActive.setActive(true); otherActive.setName("Other1");
            when(packageTypeRepository.findByIsEducationPackageFalseAndIsActiveTrue()).thenReturn(List.of(otherActive));
            List<PackageTypeDTO> result = packageService.listOtherPackages();
            assertEquals(1, result.size());
            assertEquals("Other1", result.getFirst().getName());
        }

        @Test
        void getPackageById_packageExists_returnsDTO() {
            when(packageTypeRepository.findById(packageType1.getId())).thenReturn(Optional.of(packageType1));
            PackageTypeDTO result = packageService.getPackageById(packageType1.getId());
            assertNotNull(result);
            assertEquals(packageType1.getName(), result.getName());
        }

        @Test
        void getPackageById_packageNotFound_throwsIllegalArgumentException() {
            int nonExistentId = 99;
            when(packageTypeRepository.findById(nonExistentId)).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class, () -> packageService.getPackageById(nonExistentId));
        }
    }


    @Nested
    class MemberPackageManagementTests {

        @Test
        void createMemberPackage_success_singlePool_paymentCompleted() throws Exception {
            when(memberRepository.findById(member1_active_canSwim.getId())).thenReturn(Optional.of(member1_active_canSwim));
            when(packageTypeRepository.findById(packageType1_active_singlePool_requiresSwim.getId()))
                    .thenReturn(Optional.of(packageType1_active_singlePool_requiresSwim));
            when(memberService.hasSwimmingAbility(member1_active_canSwim.getId())).thenReturn(true);
            when(poolRepository.findById(pool1.getId())).thenReturn(Optional.of(pool1));
            // Assume canBuyPackage returns true (no conflicting active packages)
            when(memberPackageRepository.findByMemberIdAndActiveTrueAndPaymentStatus(
                    member1_active_canSwim.getId(), MemberPackagePaymentStatusEnum.COMPLETED))
                    .thenReturn(Collections.emptyList()); // No active packages initially
            when(memberPackageRepository.save(any(MemberPackage.class))).thenAnswer(invocation -> {
                MemberPackage mp = invocation.getArgument(0);
                mp.setId(301); // Simulate ID generation
                return mp;
            });

            MemberPackageDTO dtoToCreate = MemberPackageDTO.builder()
                    .memberId(member1_active_canSwim.getId())
                    .packageTypeId(packageType1_active_singlePool_requiresSwim.getId())
                    .poolId(pool1.getId())
                    .paymentStatus(MemberPackagePaymentStatusEnum.COMPLETED)
                    .paymentDate(LocalDateTime.now())
                    .build();

            MemberPackageDTO result = packageService.createMemberPackage(dtoToCreate);

            assertNotNull(result);
            assertEquals(301, result.getId());
            assertEquals(member1_active_canSwim.getId(), result.getMemberId());
            assertEquals(packageType1_active_singlePool_requiresSwim.getId(), result.getPackageTypeId());
            assertEquals(pool1.getId(), result.getPoolId());
            assertEquals(MemberPackagePaymentStatusEnum.COMPLETED, result.getPaymentStatus());
            assertTrue(result.isActive());
            assertEquals(packageType1_active_singlePool_requiresSwim.getSessionLimit(), result.getSessionsRemaining());
            assertEquals(packageType1_active_singlePool_requiresSwim.getName(), result.getPackageName());
            assertEquals(pool1.getName(), result.getPoolName());

            ArgumentCaptor<MemberPackage> captor = ArgumentCaptor.forClass(MemberPackage.class);
            verify(memberPackageRepository).save(captor.capture());
            MemberPackage saved = captor.getValue();
            assertTrue(saved.isActive());
            assertNotNull(saved.getPaymentDate());
        }

        @Test
        void createMemberPackage_success_multiPool_paymentPending() {
            when(memberRepository.findById(member1_active_canSwim.getId())).thenReturn(Optional.of(member1_active_canSwim));
            when(packageTypeRepository.findById(packageType2_active_multiPool_noSwim.getId()))
                    .thenReturn(Optional.of(packageType2_active_multiPool_noSwim));
            // No swim check for packageType2
            // No pool check for multi-pool package
            when(memberPackageRepository.findByMemberIdAndActiveTrueAndPaymentStatus(
                    member1_active_canSwim.getId(), MemberPackagePaymentStatusEnum.COMPLETED))
                    .thenReturn(Collections.emptyList());
            when(memberPackageRepository.save(any(MemberPackage.class))).thenAnswer(invocation -> {
                MemberPackage mp = invocation.getArgument(0);
                mp.setId(302);
                return mp;
            });

            MemberPackageDTO dtoToCreate = MemberPackageDTO.builder()
                    .memberId(member1_active_canSwim.getId())
                    .packageTypeId(packageType2_active_multiPool_noSwim.getId())
                    // poolId should be 0 or not set for multi-pool
                    .paymentStatus(MemberPackagePaymentStatusEnum.PENDING)
                    .build();

            MemberPackageDTO result = packageService.createMemberPackage(dtoToCreate);

            assertNotNull(result);
            assertEquals(302, result.getId());
            assertEquals(0, result.getPoolId()); // PoolId should be 0 for multi-pool
            assertEquals(MemberPackagePaymentStatusEnum.PENDING, result.getPaymentStatus());
            assertFalse(result.isActive());
            assertEquals(packageType2_active_multiPool_noSwim.getSessionLimit(), result.getSessionsRemaining());
            assertEquals(packageType2_active_multiPool_noSwim.getName(), result.getPackageName());
            assertEquals("Birden Fazla Havuzda Geçerli", result.getPoolName());


            ArgumentCaptor<MemberPackage> captor = ArgumentCaptor.forClass(MemberPackage.class);
            verify(memberPackageRepository).save(captor.capture());
            MemberPackage saved = captor.getValue();
            assertFalse(saved.isActive());
            assertNull(saved.getPaymentDate());
        }

        @Test
        void createMemberPackage_memberNotActive_throwsInvalidOperationException() {
            when(memberRepository.findById(member2_pending_cannotSwim.getId())).thenReturn(Optional.of(member2_pending_cannotSwim));
            // No need to mock packageTypeRepo if member check fails first

            MemberPackageDTO dto = MemberPackageDTO.builder().memberId(member2_pending_cannotSwim.getId()).packageTypeId(1).build();
            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> packageService.createMemberPackage(dto));
            assertTrue(ex.getMessage().contains("üye durumu AKTİF olmalıdır"));
        }

        @Test
        void createMemberPackage_packageTypeInactive_throwsInvalidOperationException() {
            when(memberRepository.findById(member1_active_canSwim.getId())).thenReturn(Optional.of(member1_active_canSwim));
            when(packageTypeRepository.findById(packageType3_inactive.getId())).thenReturn(Optional.of(packageType3_inactive));

            MemberPackageDTO dto = MemberPackageDTO.builder().memberId(member1_active_canSwim.getId()).packageTypeId(packageType3_inactive.getId()).build();
            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> packageService.createMemberPackage(dto));
            assertTrue(ex.getMessage().contains("şu anda aktif değildir"));
        }

        @Test
        void createMemberPackage_requiresSwim_memberCannotSwim_throwsInvalidOperationException() throws Exception {
            when(memberRepository.findById(member3_active_cannotSwim.getId())).thenReturn(Optional.of(member3_active_cannotSwim));
            when(packageTypeRepository.findById(packageType1_active_singlePool_requiresSwim.getId()))
                    .thenReturn(Optional.of(packageType1_active_singlePool_requiresSwim));
            when(memberService.hasSwimmingAbility(member3_active_cannotSwim.getId())).thenReturn(false); // Member cannot swim

            MemberPackageDTO dto = MemberPackageDTO.builder()
                    .memberId(member3_active_cannotSwim.getId())
                    .packageTypeId(packageType1_active_singlePool_requiresSwim.getId())
                    .poolId(pool1.getId())
                    .build();
            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> packageService.createMemberPackage(dto));
            assertTrue(ex.getMessage().contains("yüzme bilgisi gerektirir"));
        }

        @Test
        void createMemberPackage_singlePoolPackage_noPoolIdProvided_throwsInvalidOperationException() throws Exception {
            when(memberRepository.findById(member1_active_canSwim.getId())).thenReturn(Optional.of(member1_active_canSwim));
            when(packageTypeRepository.findById(packageType1_active_singlePool_requiresSwim.getId()))
                    .thenReturn(Optional.of(packageType1_active_singlePool_requiresSwim));
            when(memberService.hasSwimmingAbility(member1_active_canSwim.getId())).thenReturn(true);


            MemberPackageDTO dtoToCreate = MemberPackageDTO.builder()
                    .memberId(member1_active_canSwim.getId())
                    .packageTypeId(packageType1_active_singlePool_requiresSwim.getId())
                    .poolId(0)
                    .paymentStatus(MemberPackagePaymentStatusEnum.COMPLETED)
                    .build();

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> packageService.createMemberPackage(dtoToCreate));
            assertTrue(ex.getMessage().contains("geçerli bir havuz seçilmelidir"));
        }


        @Test
        void createMemberPackage_cannotBuyPackage_throwsInvalidOperationException() throws Exception {
            when(memberRepository.findById(member1_active_canSwim.getId())).thenReturn(Optional.of(member1_active_canSwim));
            when(packageTypeRepository.findById(packageType1_active_singlePool_requiresSwim.getId()))
                    .thenReturn(Optional.of(packageType1_active_singlePool_requiresSwim));
            when(memberService.hasSwimmingAbility(member1_active_canSwim.getId())).thenReturn(true);
            when(poolRepository.findById(pool1.getId())).thenReturn(Optional.of(pool1));

            MemberPackage existingActivePackage = new MemberPackage();
            existingActivePackage.setPoolId(pool1.getId());
            when(memberPackageRepository.findByMemberIdAndActiveTrueAndPaymentStatus(
                    member1_active_canSwim.getId(), MemberPackagePaymentStatusEnum.COMPLETED))
                    .thenReturn(List.of(existingActivePackage));

            MemberPackageDTO dto = MemberPackageDTO.builder()
                    .memberId(member1_active_canSwim.getId())
                    .packageTypeId(packageType1_active_singlePool_requiresSwim.getId())
                    .poolId(pool1.getId())
                    .build();
            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> packageService.createMemberPackage(dto));
            assertTrue(ex.getMessage().contains("mevcut aktif paket kısıtlamaları nedeniyle bu paketi satın alamaz"));
        }


        @Test
        void getActiveMemberPackages_returnsCorrectlyEnrichedDTOs() {
            MemberPackage mpActive = new MemberPackage();
            mpActive.setId(1);
            mpActive.setMemberId(member1_active_canSwim.getId());
            mpActive.setPackageTypeId(packageType1_active_singlePool_requiresSwim.getId());
            mpActive.setPoolId(pool1.getId());
            mpActive.setActive(true);
            mpActive.setPaymentStatus(MemberPackagePaymentStatusEnum.COMPLETED);
            mpActive.setSessionsRemaining(10);

            when(memberPackageRepository.findByMemberIdAndActiveTrueAndPaymentStatus(
                    member1_active_canSwim.getId(), MemberPackagePaymentStatusEnum.COMPLETED))
                    .thenReturn(List.of(mpActive));
            when(packageTypeRepository.findById(packageType1_active_singlePool_requiresSwim.getId()))
                    .thenReturn(Optional.of(packageType1_active_singlePool_requiresSwim));
            when(poolRepository.findById(pool1.getId())).thenReturn(Optional.of(pool1));


            List<MemberPackageDTO> result = packageService.getActiveMemberPackages(member1_active_canSwim.getId());

            assertEquals(1, result.size());
            MemberPackageDTO dto = result.getFirst();
            assertEquals(mpActive.getId(), dto.getId());
            assertEquals(packageType1_active_singlePool_requiresSwim.getName(), dto.getPackageName());
            assertEquals(pool1.getName(), dto.getPoolName());
            assertTrue(dto.isActive());
        }

        @Test
        void getPreviousMemberPackages_returnsCorrectlyEnrichedDTOs() {
            MemberPackage mpPreviousInactive = new MemberPackage(); // Inactive but paid
            mpPreviousInactive.setId(1);
            mpPreviousInactive.setMemberId(member1_active_canSwim.getId());
            mpPreviousInactive.setPackageTypeId(packageType1_active_singlePool_requiresSwim.getId());
            mpPreviousInactive.setPoolId(pool1.getId());
            mpPreviousInactive.setActive(false);
            mpPreviousInactive.setPaymentStatus(MemberPackagePaymentStatusEnum.COMPLETED);
            mpPreviousInactive.setSessionsRemaining(5);

            MemberPackage mpPreviousNoSessions = new MemberPackage(); // Active, paid, but no sessions
            mpPreviousNoSessions.setId(2);
            mpPreviousNoSessions.setMemberId(member1_active_canSwim.getId());
            mpPreviousNoSessions.setPackageTypeId(packageType2_active_multiPool_noSwim.getId());
            mpPreviousNoSessions.setPoolId(0);
            mpPreviousNoSessions.setActive(true);
            mpPreviousNoSessions.setPaymentStatus(MemberPackagePaymentStatusEnum.COMPLETED);
            mpPreviousNoSessions.setSessionsRemaining(0);


            when(memberPackageRepository.findByMemberIdAndPaymentStatus(
                    member1_active_canSwim.getId(), MemberPackagePaymentStatusEnum.COMPLETED))
                    .thenReturn(List.of(mpPreviousInactive, mpPreviousNoSessions));
            // Mock enrichDTO dependencies
            when(packageTypeRepository.findById(packageType1_active_singlePool_requiresSwim.getId()))
                    .thenReturn(Optional.of(packageType1_active_singlePool_requiresSwim));
            when(poolRepository.findById(pool1.getId())).thenReturn(Optional.of(pool1));
            when(packageTypeRepository.findById(packageType2_active_multiPool_noSwim.getId()))
                    .thenReturn(Optional.of(packageType2_active_multiPool_noSwim));


            List<MemberPackageDTO> result = packageService.getPreviousMemberPackages(member1_active_canSwim.getId());

            assertEquals(2, result.size());
            assertTrue(result.stream().anyMatch(dto -> dto.getId() == mpPreviousInactive.getId() && !dto.isActive()));
            assertTrue(result.stream().anyMatch(dto -> dto.getId() == mpPreviousNoSessions.getId() && dto.getSessionsRemaining() == 0));
        }

        @Test
        void getMemberPackages_returnsAllPackagesForMember() {
            MemberPackage mp1 = new MemberPackage(); mp1.setId(1); mp1.setMemberId(101); mp1.setPackageTypeId(1);
            MemberPackage mp2 = new MemberPackage(); mp2.setId(2); mp2.setMemberId(101); mp2.setPackageTypeId(2);
            when(memberPackageRepository.findByMemberId(101)).thenReturn(List.of(mp1, mp2));
            // Mock enrichDTO dependencies as needed
            when(packageTypeRepository.findById(1)).thenReturn(Optional.of(packageType1_active_singlePool_requiresSwim));
            when(packageTypeRepository.findById(2)).thenReturn(Optional.of(packageType2_active_multiPool_noSwim));


            List<MemberPackageDTO> result = packageService.getMemberPackages(101);
            assertEquals(2, result.size());
        }

        // --- canBuyPackage Tests ---
        @Test
        void canBuyPackage_noActivePaidPackages_returnsTrue() {
            when(memberPackageRepository.findByMemberIdAndActiveTrueAndPaymentStatus(101, MemberPackagePaymentStatusEnum.COMPLETED))
                    .thenReturn(Collections.emptyList());
            assertTrue(packageService.canBuyPackage(101, pool1.getId())); // For specific pool
            assertTrue(packageService.canBuyPackage(101, null));         // For multi-pool
        }

        @Test
        void canBuyPackage_hasActiveSpecificPool_tryBuySameSpecificPool_returnsFalse() {
            MemberPackage activeSpecific = new MemberPackage();
            activeSpecific.setPoolId(pool1.getId()); // Active for pool1
            when(memberPackageRepository.findByMemberIdAndActiveTrueAndPaymentStatus(101, MemberPackagePaymentStatusEnum.COMPLETED))
                    .thenReturn(List.of(activeSpecific));
            assertFalse(packageService.canBuyPackage(101, pool1.getId()));
        }

        @Test
        void canBuyPackage_hasActiveSpecificPool_tryBuyDifferentSpecificPool_returnsTrue() {
            MemberPackage activeSpecific = new MemberPackage();
            activeSpecific.setPoolId(pool1.getId()); // Active for pool1
            when(memberPackageRepository.findByMemberIdAndActiveTrueAndPaymentStatus(101, MemberPackagePaymentStatusEnum.COMPLETED))
                    .thenReturn(List.of(activeSpecific));
            assertTrue(packageService.canBuyPackage(101, pool2.getId())); // Trying for pool2
        }

        @Test
        void canBuyPackage_hasActiveMultiPool_tryBuySpecificPool_returnsTrue() {
            MemberPackage activeMulti = new MemberPackage();
            activeMulti.setPoolId(0); // Active multi-pool
            when(memberPackageRepository.findByMemberIdAndActiveTrueAndPaymentStatus(101, MemberPackagePaymentStatusEnum.COMPLETED))
                    .thenReturn(List.of(activeMulti));
            assertTrue(packageService.canBuyPackage(101, pool1.getId())); // Trying for specific pool1
        }

        @Test
        void canBuyPackage_hasActiveMultiPool_tryBuyAnotherMultiPool_returnsFalse() {
            MemberPackage activeMulti = new MemberPackage();
            activeMulti.setPoolId(0); // Active multi-pool
            when(memberPackageRepository.findByMemberIdAndActiveTrueAndPaymentStatus(101, MemberPackagePaymentStatusEnum.COMPLETED))
                    .thenReturn(List.of(activeMulti));
            assertFalse(packageService.canBuyPackage(101, null)); // Trying for another multi-pool
            assertFalse(packageService.canBuyPackage(101, 0));    // Also indicates multi-pool
        }

        @Test
        void canBuyPackage_hasActiveSpecificPool_tryBuyMultiPool_returnsTrue() {
            MemberPackage activeSpecific = new MemberPackage();
            activeSpecific.setPoolId(pool1.getId()); // Active for pool1
            when(memberPackageRepository.findByMemberIdAndActiveTrueAndPaymentStatus(101, MemberPackagePaymentStatusEnum.COMPLETED))
                    .thenReturn(List.of(activeSpecific));
            assertTrue(packageService.canBuyPackage(101, null)); // Trying for multi-pool
        }

        @Test
        void canBuyPackage_hasBothActiveSpecificAndMulti_tryBuySpecificForExistingPool_returnsFalse() {
            MemberPackage activeSpecific = new MemberPackage(); activeSpecific.setPoolId(pool1.getId());
            MemberPackage activeMulti = new MemberPackage(); activeMulti.setPoolId(0);
            when(memberPackageRepository.findByMemberIdAndActiveTrueAndPaymentStatus(101, MemberPackagePaymentStatusEnum.COMPLETED))
                    .thenReturn(List.of(activeSpecific, activeMulti));
            assertFalse(packageService.canBuyPackage(101, pool1.getId())); // Try same specific pool
        }

        @Test
        void canBuyPackage_hasBothActiveSpecificAndMulti_tryBuySpecificForNewPool_returnsTrue() {
            MemberPackage activeSpecific = new MemberPackage(); activeSpecific.setPoolId(pool1.getId());
            MemberPackage activeMulti = new MemberPackage(); activeMulti.setPoolId(0);
            when(memberPackageRepository.findByMemberIdAndActiveTrueAndPaymentStatus(101, MemberPackagePaymentStatusEnum.COMPLETED))
                    .thenReturn(List.of(activeSpecific, activeMulti));
            assertTrue(packageService.canBuyPackage(101, pool2.getId())); // Try new specific pool
        }

        @Test
        void canBuyPackage_hasBothActiveSpecificAndMulti_tryBuyMultiPool_returnsFalse() {
            MemberPackage activeSpecific = new MemberPackage(); activeSpecific.setPoolId(pool1.getId());
            MemberPackage activeMulti = new MemberPackage(); activeMulti.setPoolId(0);
            when(memberPackageRepository.findByMemberIdAndActiveTrueAndPaymentStatus(101, MemberPackagePaymentStatusEnum.COMPLETED))
                    .thenReturn(List.of(activeSpecific, activeMulti));
            assertFalse(packageService.canBuyPackage(101, null));
        }
    }
}