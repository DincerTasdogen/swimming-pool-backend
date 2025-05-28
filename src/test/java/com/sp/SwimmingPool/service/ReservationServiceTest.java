package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.ReservationResponse;
import com.sp.SwimmingPool.exception.EntityNotFoundException;
import com.sp.SwimmingPool.exception.InvalidOperationException;
import com.sp.SwimmingPool.model.entity.MemberPackage;
import com.sp.SwimmingPool.model.entity.PackageType;
import com.sp.SwimmingPool.model.entity.Reservation;
import com.sp.SwimmingPool.model.entity.Session;
import com.sp.SwimmingPool.model.enums.MemberPackagePaymentStatusEnum;
import com.sp.SwimmingPool.model.enums.ReservationStatusEnum;
import com.sp.SwimmingPool.repos.MemberPackageRepository;
import com.sp.SwimmingPool.repos.PackageTypeRepository;
import com.sp.SwimmingPool.repos.ReservationRepository;
import com.sp.SwimmingPool.repos.SessionRepository;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private MemberPackageRepository memberPackageRepository;
    @Mock
    private PackageTypeRepository packageTypeRepository;
    @Mock
    private MemberService memberService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private ReservationService reservationService;

    private Session session1_available;
    private Session session2_full;
    private Session session3_past;
    private Session session4_tooFarFuture;
    private Session session5_education;
    private Session session6_wrongTimeForPackage;


    private MemberPackage memberPackage1_active_enoughSessions_correctPool;
    private MemberPackage memberPackage2_inactive;
    private MemberPackage memberPackage3_noSessionsLeft;
    private MemberPackage memberPackage4_paymentPending;
    private MemberPackage memberPackage5_wrongPool;


    private PackageType packageType1_general;
    private PackageType packageType2_education_requiresSwim;


    private Reservation reservation1_confirmed;
    private final int memberId = 101;
    private final int poolId = 201;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();

        // Sessions
        session1_available = new Session();
        session1_available.setId(1);
        session1_available.setPoolId(poolId);
        session1_available.setSessionDate(today.plusDays(1)); // Tomorrow
        session1_available.setStartTime(LocalTime.of(10, 0));
        session1_available.setEndTime(LocalTime.of(11, 0));
        session1_available.setCapacity(10);
        session1_available.setCurrentBookings(5); // Initial bookings
        session1_available.setEducationSession(false);

        session2_full = new Session();
        session2_full.setId(2);
        session2_full.setPoolId(poolId);
        session2_full.setSessionDate(today.plusDays(1));
        session2_full.setStartTime(LocalTime.of(11, 0));
        session2_full.setEndTime(LocalTime.of(12, 0));
        session2_full.setCapacity(5);
        session2_full.setCurrentBookings(5); // Full
        session2_full.setEducationSession(false);

        session3_past = new Session();
        session3_past.setId(3);
        session3_past.setPoolId(poolId);
        session3_past.setSessionDate(today.minusDays(1)); // Yesterday
        session3_past.setStartTime(LocalTime.of(10, 0));
        session3_past.setEndTime(LocalTime.of(11, 0));
        session3_past.setCapacity(10);
        session3_past.setCurrentBookings(0);

        session4_tooFarFuture = new Session(); // More than 72 hours away
        session4_tooFarFuture.setId(4);
        session4_tooFarFuture.setPoolId(poolId);
        session4_tooFarFuture.setSessionDate(today.plusDays(4)); // 4 days from now
        session4_tooFarFuture.setStartTime(LocalTime.of(10, 0));
        session4_tooFarFuture.setEndTime(LocalTime.of(11, 0));
        session4_tooFarFuture.setCapacity(10);
        session4_tooFarFuture.setCurrentBookings(0);

        session5_education = new Session();
        session5_education.setId(5);
        session5_education.setPoolId(poolId);
        session5_education.setSessionDate(today.plusDays(1));
        session5_education.setStartTime(LocalTime.of(14, 0));
        session5_education.setEndTime(LocalTime.of(15, 0));
        session5_education.setCapacity(10);
        session5_education.setCurrentBookings(0);
        session5_education.setEducationSession(true);

        session6_wrongTimeForPackage = new Session();
        session6_wrongTimeForPackage.setId(6);
        session6_wrongTimeForPackage.setPoolId(poolId);
        session6_wrongTimeForPackage.setSessionDate(today.plusDays(1));
        session6_wrongTimeForPackage.setStartTime(LocalTime.of(7, 0)); // Too early for packageType1
        session6_wrongTimeForPackage.setEndTime(LocalTime.of(8, 0));
        session6_wrongTimeForPackage.setCapacity(10);
        session6_wrongTimeForPackage.setCurrentBookings(0);
        session6_wrongTimeForPackage.setEducationSession(false);


        // Package Types
        packageType1_general = new PackageType();
        packageType1_general.setId(11);
        packageType1_general.setName("General Access");
        packageType1_general.setStartTime(LocalTime.of(9, 0));
        packageType1_general.setEndTime(LocalTime.of(22, 0));
        packageType1_general.setEducationPackage(false);
        packageType1_general.setRequiresSwimmingAbility(false);

        packageType2_education_requiresSwim = new PackageType();
        packageType2_education_requiresSwim.setId(12);
        packageType2_education_requiresSwim.setName("Swim Lessons");
        packageType2_education_requiresSwim.setStartTime(LocalTime.of(13, 0));
        packageType2_education_requiresSwim.setEndTime(LocalTime.of(17, 0));
        packageType2_education_requiresSwim.setEducationPackage(true);
        packageType2_education_requiresSwim.setRequiresSwimmingAbility(true);

        // Member Packages
        memberPackage1_active_enoughSessions_correctPool = new MemberPackage();
        memberPackage1_active_enoughSessions_correctPool.setId(1001);
        memberPackage1_active_enoughSessions_correctPool.setMemberId(memberId);
        memberPackage1_active_enoughSessions_correctPool.setPackageTypeId(packageType1_general.getId());
        memberPackage1_active_enoughSessions_correctPool.setActive(true);
        memberPackage1_active_enoughSessions_correctPool.setSessionsRemaining(5); // Initial sessions
        memberPackage1_active_enoughSessions_correctPool.setPaymentStatus(MemberPackagePaymentStatusEnum.COMPLETED);
        memberPackage1_active_enoughSessions_correctPool.setPoolId(poolId); // Correct pool

        memberPackage2_inactive = new MemberPackage();
        memberPackage2_inactive.setId(1002);
        memberPackage2_inactive.setMemberId(memberId);
        memberPackage2_inactive.setPackageTypeId(packageType1_general.getId());
        memberPackage2_inactive.setActive(false); // Inactive
        memberPackage2_inactive.setSessionsRemaining(5);
        memberPackage2_inactive.setPaymentStatus(MemberPackagePaymentStatusEnum.COMPLETED);
        memberPackage2_inactive.setPoolId(poolId);

        memberPackage3_noSessionsLeft = new MemberPackage();
        memberPackage3_noSessionsLeft.setId(1003);
        memberPackage3_noSessionsLeft.setMemberId(memberId);
        memberPackage3_noSessionsLeft.setPackageTypeId(packageType1_general.getId());
        memberPackage3_noSessionsLeft.setActive(true);
        memberPackage3_noSessionsLeft.setSessionsRemaining(0); // No sessions
        memberPackage3_noSessionsLeft.setPaymentStatus(MemberPackagePaymentStatusEnum.COMPLETED);
        memberPackage3_noSessionsLeft.setPoolId(poolId);

        memberPackage4_paymentPending = new MemberPackage();
        memberPackage4_paymentPending.setId(1004);
        memberPackage4_paymentPending.setMemberId(memberId);
        memberPackage4_paymentPending.setPackageTypeId(packageType1_general.getId());
        memberPackage4_paymentPending.setActive(true);
        memberPackage4_paymentPending.setSessionsRemaining(5);
        memberPackage4_paymentPending.setPaymentStatus(MemberPackagePaymentStatusEnum.PENDING);
        memberPackage4_paymentPending.setPoolId(poolId);

        memberPackage5_wrongPool = new MemberPackage();
        memberPackage5_wrongPool.setId(1005);
        memberPackage5_wrongPool.setMemberId(memberId);
        memberPackage5_wrongPool.setPackageTypeId(packageType1_general.getId());
        memberPackage5_wrongPool.setActive(true);
        memberPackage5_wrongPool.setSessionsRemaining(5);
        memberPackage5_wrongPool.setPaymentStatus(MemberPackagePaymentStatusEnum.COMPLETED);
        memberPackage5_wrongPool.setPoolId(poolId + 1); // Different pool

        // Reservation
        reservation1_confirmed = new Reservation();
        reservation1_confirmed.setId(10001);
        reservation1_confirmed.setMemberId(memberId);
        reservation1_confirmed.setSessionId(session1_available.getId());
        reservation1_confirmed.setMemberPackageId(memberPackage1_active_enoughSessions_correctPool.getId());
        reservation1_confirmed.setStatus(ReservationStatusEnum.CONFIRMED);
        reservation1_confirmed.setCreatedAt(LocalDateTime.now().minusHours(1));
        reservation1_confirmed.setUpdatedAt(LocalDateTime.now().minusHours(1));
    }

    @Nested
    class CreateReservationTests {

        @Test
        void createReservation_success() {
            when(sessionRepository.findById(session1_available.getId())).thenReturn(Optional.of(session1_available));
            when(memberPackageRepository.findById(memberPackage1_active_enoughSessions_correctPool.getId()))
                    .thenReturn(Optional.of(memberPackage1_active_enoughSessions_correctPool));
            when(packageTypeRepository.findById(memberPackage1_active_enoughSessions_correctPool.getPackageTypeId()))
                    .thenReturn(Optional.of(packageType1_general));
            when(reservationRepository.findByMemberIdAndStatusAndSessionDate(eq(memberId), eq(ReservationStatusEnum.CONFIRMED), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(reservationRepository.findByMemberIdAndSessionId(memberId, session1_available.getId()))
                    .thenReturn(Optional.empty());
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
                Reservation r = invocation.getArgument(0);
                r.setId(999);
                return r;
            });
            // Make copies for verifying changes, as the original objects might be modified by the service
            Session sessionBeforeSave = new Session(); // Create a new instance or clone
            sessionBeforeSave.setCurrentBookings(session1_available.getCurrentBookings());

            MemberPackage packageBeforeSave = new MemberPackage();
            packageBeforeSave.setSessionsRemaining(memberPackage1_active_enoughSessions_correctPool.getSessionsRemaining());


            when(sessionRepository.save(any(Session.class))).thenReturn(session1_available); // or return a new modified instance
            when(memberPackageRepository.save(any(MemberPackage.class))).thenReturn(memberPackage1_active_enoughSessions_correctPool);


            Reservation result = reservationService.createReservation(memberId, session1_available.getId(), memberPackage1_active_enoughSessions_correctPool.getId());

            assertNotNull(result);
            assertEquals(999, result.getId());
            assertEquals(ReservationStatusEnum.CONFIRMED, result.getStatus());

            ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
            verify(sessionRepository).save(sessionCaptor.capture());
            assertEquals(sessionBeforeSave.getCurrentBookings() + 1, sessionCaptor.getValue().getCurrentBookings());

            ArgumentCaptor<MemberPackage> packageCaptor = ArgumentCaptor.forClass(MemberPackage.class);
            verify(memberPackageRepository).save(packageCaptor.capture());
            assertEquals(packageBeforeSave.getSessionsRemaining() - 1, packageCaptor.getValue().getSessionsRemaining());
        }

        @Test
        void createReservation_sessionNotFound_throwsEntityNotFoundException() {
            when(sessionRepository.findById(999)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () ->
                    reservationService.createReservation(memberId, 999, memberPackage1_active_enoughSessions_correctPool.getId()));
        }

        @Test
        void createReservation_sessionTooFarInFuture_throwsInvalidOperationException() {
            when(sessionRepository.findById(session4_tooFarFuture.getId())).thenReturn(Optional.of(session4_tooFarFuture));
            InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                    reservationService.createReservation(memberId, session4_tooFarFuture.getId(), memberPackage1_active_enoughSessions_correctPool.getId()));
            assertTrue(ex.getMessage().contains("önümüzdeki 72 saat için"));
        }

        @Test
        void createReservation_sessionInPast_throwsInvalidOperationException() {
            when(sessionRepository.findById(session3_past.getId())).thenReturn(Optional.of(session3_past));
            InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                    reservationService.createReservation(memberId, session3_past.getId(), memberPackage1_active_enoughSessions_correctPool.getId()));
            assertTrue(ex.getMessage().contains("Geçmişteki bir seans için"));
        }


        @Test
        void createReservation_sessionFull_throwsInvalidOperationException() {
            when(sessionRepository.findById(session2_full.getId())).thenReturn(Optional.of(session2_full));
            InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                    reservationService.createReservation(memberId, session2_full.getId(), memberPackage1_active_enoughSessions_correctPool.getId()));
            assertEquals("Seans dolu.", ex.getMessage());
        }

        @Test
        void createReservation_memberPackageNotFound_throwsEntityNotFoundException() {
            when(sessionRepository.findById(session1_available.getId())).thenReturn(Optional.of(session1_available));
            when(memberPackageRepository.findById(9999)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () ->
                    reservationService.createReservation(memberId, session1_available.getId(), 9999));
        }

        @Test
        void createReservation_memberPackageBelongsToAnotherMember_throwsInvalidOperationException() {
            MemberPackage otherMemberPackage = new MemberPackage();
            otherMemberPackage.setId(memberPackage1_active_enoughSessions_correctPool.getId());
            otherMemberPackage.setMemberId(memberId + 1); // Different member
            otherMemberPackage.setActive(true);
            otherMemberPackage.setSessionsRemaining(5);
            otherMemberPackage.setPaymentStatus(MemberPackagePaymentStatusEnum.COMPLETED);
            otherMemberPackage.setPoolId(poolId);

            when(sessionRepository.findById(session1_available.getId())).thenReturn(Optional.of(session1_available));
            when(memberPackageRepository.findById(otherMemberPackage.getId())).thenReturn(Optional.of(otherMemberPackage));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                    reservationService.createReservation(memberId, session1_available.getId(), otherMemberPackage.getId()));
            assertEquals("Sadece kendi paketinizi kullanabilirsiniz.", ex.getMessage());
        }


        @Test
        void createReservation_memberPackageInactive_throwsInvalidOperationException() {
            when(sessionRepository.findById(session1_available.getId())).thenReturn(Optional.of(session1_available));
            when(memberPackageRepository.findById(memberPackage2_inactive.getId())).thenReturn(Optional.of(memberPackage2_inactive));
            InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                    reservationService.createReservation(memberId, session1_available.getId(), memberPackage2_inactive.getId()));
            assertEquals("Üye paketiniz aktif değil.", ex.getMessage());
        }

        @Test
        void createReservation_memberPackageNoSessionsLeft_throwsInvalidOperationException() {
            when(sessionRepository.findById(session1_available.getId())).thenReturn(Optional.of(session1_available));
            when(memberPackageRepository.findById(memberPackage3_noSessionsLeft.getId())).thenReturn(Optional.of(memberPackage3_noSessionsLeft));
            InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                    reservationService.createReservation(memberId, session1_available.getId(), memberPackage3_noSessionsLeft.getId()));
            assertEquals("Seans hakkınız kalmadı.", ex.getMessage());
        }

        @Test
        void createReservation_memberPackagePaymentPending_throwsInvalidOperationException() {
            when(sessionRepository.findById(session1_available.getId())).thenReturn(Optional.of(session1_available));
            when(memberPackageRepository.findById(memberPackage4_paymentPending.getId())).thenReturn(Optional.of(memberPackage4_paymentPending));
            InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                    reservationService.createReservation(memberId, session1_available.getId(), memberPackage4_paymentPending.getId()));
            assertTrue(ex.getMessage().contains("Paketinizin ödemesi henüz tamamlanmamış"));
        }

        @Test
        void createReservation_memberPackageForDifferentPool_throwsInvalidOperationException() {
            when(sessionRepository.findById(session1_available.getId())).thenReturn(Optional.of(session1_available));
            when(memberPackageRepository.findById(memberPackage5_wrongPool.getId())).thenReturn(Optional.of(memberPackage5_wrongPool));
            InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                    reservationService.createReservation(memberId, session1_available.getId(), memberPackage5_wrongPool.getId()));
            assertEquals("Üyeliğiniz bu havuz için geçerli değil.", ex.getMessage());
        }

        @Test
        void createReservation_packageTypeNotFound_throwsEntityNotFoundException() {
            when(sessionRepository.findById(session1_available.getId())).thenReturn(Optional.of(session1_available));
            when(memberPackageRepository.findById(memberPackage1_active_enoughSessions_correctPool.getId()))
                    .thenReturn(Optional.of(memberPackage1_active_enoughSessions_correctPool));
            when(packageTypeRepository.findById(memberPackage1_active_enoughSessions_correctPool.getPackageTypeId()))
                    .thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () ->
                    reservationService.createReservation(memberId, session1_available.getId(), memberPackage1_active_enoughSessions_correctPool.getId()));
        }

        @Test
        void createReservation_sessionTimeOutsidePackageTime_throwsInvalidOperationException() {
            when(sessionRepository.findById(session6_wrongTimeForPackage.getId())).thenReturn(Optional.of(session6_wrongTimeForPackage));
            when(memberPackageRepository.findById(memberPackage1_active_enoughSessions_correctPool.getId()))
                    .thenReturn(Optional.of(memberPackage1_active_enoughSessions_correctPool));
            when(packageTypeRepository.findById(memberPackage1_active_enoughSessions_correctPool.getPackageTypeId()))
                    .thenReturn(Optional.of(packageType1_general));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                    reservationService.createReservation(memberId, session6_wrongTimeForPackage.getId(), memberPackage1_active_enoughSessions_correctPool.getId()));
            assertTrue(ex.getMessage().contains("sizin üyelik paketinizde bulunan seans saatleri"));
        }

        @Test
        void createReservation_educationPackageForNonEducationSession_throwsInvalidOperationException() {
            MemberPackage eduMemberPackage = new MemberPackage();
            eduMemberPackage.setId(1006);
            eduMemberPackage.setMemberId(memberId);
            eduMemberPackage.setPackageTypeId(packageType2_education_requiresSwim.getId());
            eduMemberPackage.setActive(true);
            eduMemberPackage.setSessionsRemaining(5);
            eduMemberPackage.setPaymentStatus(MemberPackagePaymentStatusEnum.COMPLETED);
            eduMemberPackage.setPoolId(poolId);

            Session nonEduSessionInEduTimeSlot = new Session();
            nonEduSessionInEduTimeSlot.setId(7);
            nonEduSessionInEduTimeSlot.setPoolId(poolId);
            nonEduSessionInEduTimeSlot.setSessionDate(LocalDate.now().plusDays(1));
            nonEduSessionInEduTimeSlot.setStartTime(LocalTime.of(14, 0)); // Within packageType2 time
            nonEduSessionInEduTimeSlot.setEndTime(LocalTime.of(15, 0));   // Within packageType2 time
            nonEduSessionInEduTimeSlot.setCapacity(10);
            nonEduSessionInEduTimeSlot.setCurrentBookings(0);
            nonEduSessionInEduTimeSlot.setEducationSession(false); // Session is NOT education

            when(sessionRepository.findById(nonEduSessionInEduTimeSlot.getId())).thenReturn(Optional.of(nonEduSessionInEduTimeSlot));
            when(memberPackageRepository.findById(eduMemberPackage.getId())).thenReturn(Optional.of(eduMemberPackage));
            when(packageTypeRepository.findById(packageType2_education_requiresSwim.getId())).thenReturn(Optional.of(packageType2_education_requiresSwim));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                    reservationService.createReservation(memberId, nonEduSessionInEduTimeSlot.getId(), eduMemberPackage.getId()));
            assertEquals("Bu seans bir eğitim seansı değil.", ex.getMessage());
        }

        @Test
        void createReservation_packageRequiresSwim_memberCannotSwim_throwsRuntimeException() throws Exception {
            MemberPackage swimRequiredPackage = new MemberPackage();
            swimRequiredPackage.setId(1007);
            swimRequiredPackage.setMemberId(memberId);
            swimRequiredPackage.setPackageTypeId(packageType2_education_requiresSwim.getId());
            swimRequiredPackage.setActive(true);
            swimRequiredPackage.setSessionsRemaining(5);
            swimRequiredPackage.setPaymentStatus(MemberPackagePaymentStatusEnum.COMPLETED);
            swimRequiredPackage.setPoolId(poolId);

            when(sessionRepository.findById(session5_education.getId())).thenReturn(Optional.of(session5_education));
            when(memberPackageRepository.findById(swimRequiredPackage.getId())).thenReturn(Optional.of(swimRequiredPackage));
            when(packageTypeRepository.findById(packageType2_education_requiresSwim.getId())).thenReturn(Optional.of(packageType2_education_requiresSwim));
            when(memberService.hasSwimmingAbility(memberId)).thenReturn(false);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> // Expect RuntimeException
                    reservationService.createReservation(memberId, session5_education.getId(), swimRequiredPackage.getId()));
            assertTrue(ex.getMessage().contains("Yüzme bilgisi doğrulanamadı"));
            assertNotNull(ex.getCause());
            assertInstanceOf(InvalidOperationException.class, ex.getCause());
            assertEquals("Bu seansa rezervasyon yapabilmeniz için yüzme bilmeniz gerekiyor.", ex.getCause().getMessage());
        }

        @Test
        void createReservation_timeConflictWithExistingReservation_throwsInvalidOperationException() {
            Reservation existingReservation = new Reservation();
            existingReservation.setSessionId(session2_full.getId());
            existingReservation.setStatus(ReservationStatusEnum.CONFIRMED);

            Session conflictingExistingSession = new Session();
            conflictingExistingSession.setId(session2_full.getId());
            conflictingExistingSession.setSessionDate(session1_available.getSessionDate());
            conflictingExistingSession.setStartTime(LocalTime.of(10, 30));
            conflictingExistingSession.setEndTime(LocalTime.of(11, 30));

            when(sessionRepository.findById(session1_available.getId())).thenReturn(Optional.of(session1_available));
            when(memberPackageRepository.findById(memberPackage1_active_enoughSessions_correctPool.getId()))
                    .thenReturn(Optional.of(memberPackage1_active_enoughSessions_correctPool));
            when(packageTypeRepository.findById(memberPackage1_active_enoughSessions_correctPool.getPackageTypeId()))
                    .thenReturn(Optional.of(packageType1_general));
            when(reservationRepository.findByMemberIdAndStatusAndSessionDate(memberId, ReservationStatusEnum.CONFIRMED, session1_available.getSessionDate()))
                    .thenReturn(List.of(existingReservation));
            when(sessionRepository.findById(existingReservation.getSessionId())).thenReturn(Optional.of(conflictingExistingSession));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                    reservationService.createReservation(memberId, session1_available.getId(), memberPackage1_active_enoughSessions_correctPool.getId()));
            assertEquals("Bu saatler arasında başka bir rezervasyon yapılmış.", ex.getMessage());
        }

        @Test
        void createReservation_alreadyReservedThisSpecificSession_throwsInvalidOperationException() {
            when(sessionRepository.findById(session1_available.getId())).thenReturn(Optional.of(session1_available));
            when(memberPackageRepository.findById(memberPackage1_active_enoughSessions_correctPool.getId()))
                    .thenReturn(Optional.of(memberPackage1_active_enoughSessions_correctPool));
            when(packageTypeRepository.findById(memberPackage1_active_enoughSessions_correctPool.getPackageTypeId()))
                    .thenReturn(Optional.of(packageType1_general));
            when(reservationRepository.findByMemberIdAndStatusAndSessionDate(anyInt(), any(), any())).thenReturn(Collections.emptyList());
            when(reservationRepository.findByMemberIdAndSessionId(memberId, session1_available.getId()))
                    .thenReturn(Optional.of(reservation1_confirmed));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                    reservationService.createReservation(memberId, session1_available.getId(), memberPackage1_active_enoughSessions_correctPool.getId()));
            assertEquals("Bu seansa halihazırda rezervasyonunuz bulunmaktadır.", ex.getMessage());
        }
    }

    @Nested
    class CancelReservationTests {
        @Test
        void cancelReservation_success() {
            // Make copies for verifying changes
            Session sessionBeforeSave = new Session();
            sessionBeforeSave.setCurrentBookings(session1_available.getCurrentBookings());

            MemberPackage packageBeforeSave = new MemberPackage();
            packageBeforeSave.setSessionsRemaining(memberPackage1_active_enoughSessions_correctPool.getSessionsRemaining());
            packageBeforeSave.setActive(memberPackage1_active_enoughSessions_correctPool.isActive());


            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));
            when(sessionRepository.findById(reservation1_confirmed.getSessionId())).thenReturn(Optional.of(session1_available));
            when(memberPackageRepository.findById(reservation1_confirmed.getMemberPackageId()))
                    .thenReturn(Optional.of(memberPackage1_active_enoughSessions_correctPool));

            reservationService.cancelReservation(reservation1_confirmed.getId(), memberId);

            ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(reservationCaptor.capture());
            assertEquals(ReservationStatusEnum.CANCELLED, reservationCaptor.getValue().getStatus());

            ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
            verify(sessionRepository).save(sessionCaptor.capture());
            assertEquals(sessionBeforeSave.getCurrentBookings() - 1, sessionCaptor.getValue().getCurrentBookings());

            ArgumentCaptor<MemberPackage> packageCaptor = ArgumentCaptor.forClass(MemberPackage.class);
            verify(memberPackageRepository).save(packageCaptor.capture());
            assertEquals(packageBeforeSave.getSessionsRemaining() + 1, packageCaptor.getValue().getSessionsRemaining());
            // If sessionsRemaining was 0 and became 1, active should be true
            if (packageBeforeSave.getSessionsRemaining() == 0) {
                assertTrue(packageCaptor.getValue().isActive());
            }
        }

        @Test
        void cancelReservation_notFound_throwsEntityNotFoundException() {
            when(reservationRepository.findById(999)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> reservationService.cancelReservation(999, memberId));
        }

        @Test
        void cancelReservation_notBelongingToMember_throwsInvalidOperationException() {
            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));
            InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                    reservationService.cancelReservation(reservation1_confirmed.getId(), memberId + 1));
            assertEquals("Sadece kendinize ait rezervasyonu iptal edebilirsiniz.", ex.getMessage());
        }

        @Test
        void cancelReservation_notConfirmedStatus_throwsInvalidOperationException() {
            reservation1_confirmed.setStatus(ReservationStatusEnum.CANCELLED);
            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));
            InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                    reservationService.cancelReservation(reservation1_confirmed.getId(), memberId));
            assertTrue(ex.getMessage().contains("Sadece onaylanmış rezervasyonlar iptal edilebilir"));
        }

        @Test
        void cancelReservation_sessionForReservationNotFound_throwsEntityNotFoundException() {
            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));
            when(sessionRepository.findById(reservation1_confirmed.getSessionId())).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () ->
                    reservationService.cancelReservation(reservation1_confirmed.getId(), memberId));
        }


        @Test
        void cancelReservation_pastCancellationDeadline_throwsInvalidOperationException() {
            Session sessionTodaySoon = new Session();
            sessionTodaySoon.setId(session1_available.getId());
            sessionTodaySoon.setSessionDate(LocalDate.now());
            sessionTodaySoon.setStartTime(LocalTime.now().plusHours(1));
            sessionTodaySoon.setEndTime(LocalTime.now().plusHours(2));

            reservation1_confirmed.setSessionId(sessionTodaySoon.getId());

            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));
            when(sessionRepository.findById(sessionTodaySoon.getId())).thenReturn(Optional.of(sessionTodaySoon));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                    reservationService.cancelReservation(reservation1_confirmed.getId(), memberId));
            assertTrue(ex.getMessage().contains("sadece 3 saat öncesine kadar iptal edebilirsiniz"));
        }

        @Test
        void cancelReservation_memberPackageForReservationNotFound_throwsEntityNotFoundException() {
            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));
            when(sessionRepository.findById(reservation1_confirmed.getSessionId())).thenReturn(Optional.of(session1_available));
            when(memberPackageRepository.findById(reservation1_confirmed.getMemberPackageId())).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () ->
                    reservationService.cancelReservation(reservation1_confirmed.getId(), memberId));
        }
    }

    @Nested
    class MarkReservationTests {
        @Test
        void markReservationAsCompleted_success() {
            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));
            reservationService.markReservationAsCompleted(reservation1_confirmed.getId());
            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());
            assertEquals(ReservationStatusEnum.COMPLETED, captor.getValue().getStatus());
        }

        @Test
        void markReservationAsCompleted_notConfirmed_throwsInvalidOperationException() {
            reservation1_confirmed.setStatus(ReservationStatusEnum.CANCELLED);
            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));
            assertThrows(InvalidOperationException.class, () -> reservationService.markReservationAsCompleted(reservation1_confirmed.getId()));
        }

        @Test
        void markReservationAsNoShow_success() {
            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));
            reservationService.markReservationAsNoShow(reservation1_confirmed.getId());
            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());
            assertEquals(ReservationStatusEnum.NO_SHOW, captor.getValue().getStatus());
        }
    }

    @Nested
    class QrTokenTests {
        private final String testQrToken = "test-qr-token";
        @Mock
        private Claims mockedClaims;

        @Test
        void completeReservationByQrToken_success() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime validSessionStart = now.minusMinutes(30);
            LocalDateTime validSessionEnd = now.plusMinutes(30);

            when(mockedClaims.get("reservationId")).thenReturn(reservation1_confirmed.getId());
            when(mockedClaims.get("memberId")).thenReturn(memberId);
            when(mockedClaims.get("sessionStart")).thenReturn(validSessionStart.toString());
            when(mockedClaims.get("sessionEnd")).thenReturn(validSessionEnd.toString());

            when(jwtTokenProvider.parseReservationQrToken(testQrToken)).thenReturn(mockedClaims);
            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));

            reservationService.completeReservationByQrToken(testQrToken);

            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());
            assertEquals(ReservationStatusEnum.COMPLETED, captor.getValue().getStatus());
        }

        @Test
        void completeReservationByQrToken_invalidToken_throwsInvalidOperationException() {
            when(jwtTokenProvider.parseReservationQrToken("invalid-token")).thenThrow(new RuntimeException("Token parse error"));
            assertThrows(InvalidOperationException.class, () -> reservationService.completeReservationByQrToken("invalid-token"));
        }

        @Test
        void completeReservationByQrToken_reservationNotFound_throwsEntityNotFoundException() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime validSessionStart = now.minusMinutes(10);
            LocalDateTime validSessionEnd = now.plusMinutes(10);

            // Stub all claims that will be accessed before findById
            when(mockedClaims.get("reservationId")).thenReturn(reservation1_confirmed.getId());
            when(mockedClaims.get("memberId")).thenReturn(memberId);
            when(mockedClaims.get("sessionStart")).thenReturn(validSessionStart.toString());
            when(mockedClaims.get("sessionEnd")).thenReturn(validSessionEnd.toString());

            when(jwtTokenProvider.parseReservationQrToken(testQrToken)).thenReturn(mockedClaims);
            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> reservationService.completeReservationByQrToken(testQrToken));
        }

        @Test
        void completeReservationByQrToken_memberIdMismatch_throwsInvalidOperationException() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime validSessionStart = now.minusMinutes(10);
            LocalDateTime validSessionEnd = now.plusMinutes(10);

            when(mockedClaims.get("reservationId")).thenReturn(reservation1_confirmed.getId());
            when(mockedClaims.get("memberId")).thenReturn(memberId + 1); // Mismatched memberId
            when(mockedClaims.get("sessionStart")).thenReturn(validSessionStart.toString());
            when(mockedClaims.get("sessionEnd")).thenReturn(validSessionEnd.toString());

            when(jwtTokenProvider.parseReservationQrToken(testQrToken)).thenReturn(mockedClaims);
            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));
            assertThrows(InvalidOperationException.class, () -> reservationService.completeReservationByQrToken(testQrToken));
        }

        @Test
        void completeReservationByQrToken_notConfirmedStatus_throwsInvalidOperationException() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime validSessionStart = now.minusMinutes(10);
            LocalDateTime validSessionEnd = now.plusMinutes(10);

            when(mockedClaims.get("reservationId")).thenReturn(reservation1_confirmed.getId());
            when(mockedClaims.get("memberId")).thenReturn(memberId);
            when(mockedClaims.get("sessionStart")).thenReturn(validSessionStart.toString());
            when(mockedClaims.get("sessionEnd")).thenReturn(validSessionEnd.toString());

            reservation1_confirmed.setStatus(ReservationStatusEnum.CANCELLED);
            when(jwtTokenProvider.parseReservationQrToken(testQrToken)).thenReturn(mockedClaims);
            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));
            assertThrows(InvalidOperationException.class, () -> reservationService.completeReservationByQrToken(testQrToken));
        }

        @Test
        void completeReservationByQrToken_timeOutsideWindow_beforeStart_throwsInvalidOperationException() {
            when(mockedClaims.get("reservationId")).thenReturn(reservation1_confirmed.getId());
            when(mockedClaims.get("memberId")).thenReturn(memberId);
            when(mockedClaims.get("sessionStart")).thenReturn(LocalDateTime.now().plusHours(2).toString());
            when(mockedClaims.get("sessionEnd")).thenReturn(LocalDateTime.now().plusHours(3).toString());

            when(jwtTokenProvider.parseReservationQrToken(testQrToken)).thenReturn(mockedClaims);

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> reservationService.completeReservationByQrToken(testQrToken));
            assertEquals("Rezervasyon saat aralığı dışında giriş yapılamaz.", ex.getMessage());
            verify(reservationRepository, never()).findById(anyInt()); // Confirm findById is not called
        }

        @Test
        void completeReservationByQrToken_timeOutsideWindow_afterEnd_throwsInvalidOperationException() {
            // Stub all claims that are accessed before the time check
            when(mockedClaims.get("reservationId")).thenReturn(reservation1_confirmed.getId());
            when(mockedClaims.get("memberId")).thenReturn(memberId);
            when(mockedClaims.get("sessionStart")).thenReturn(LocalDateTime.now().minusHours(3).toString());
            when(mockedClaims.get("sessionEnd")).thenReturn(LocalDateTime.now().minusHours(2).toString());

            when(jwtTokenProvider.parseReservationQrToken(testQrToken)).thenReturn(mockedClaims);

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> reservationService.completeReservationByQrToken(testQrToken));
            assertEquals("Rezervasyon saat aralığı dışında giriş yapılamaz.", ex.getMessage());
            verify(reservationRepository, never()).findById(anyInt()); // Confirm findById is not called
        }

        @Test
        void generateReservationQrTokenForMember_success() {
            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));
            when(sessionRepository.findById(reservation1_confirmed.getSessionId())).thenReturn(Optional.of(session1_available));
            when(jwtTokenProvider.generateReservationQrToken(
                    eq(reservation1_confirmed.getId()),
                    eq(memberId),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)
            )).thenReturn("generated-qr-token");

            String token = reservationService.generateReservationQrTokenForMember(reservation1_confirmed.getId(), memberId);
            assertEquals("generated-qr-token", token);
        }

        @Test
        void generateReservationQrTokenForMember_reservationNotFound_throwsEntityNotFoundException() {
            when(reservationRepository.findById(999)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> reservationService.generateReservationQrTokenForMember(999, memberId));
        }

        @Test
        void generateReservationQrTokenForMember_notBelongingToMember_throwsInvalidOperationException() {
            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));
            assertThrows(InvalidOperationException.class, () -> reservationService.generateReservationQrTokenForMember(reservation1_confirmed.getId(), memberId + 1));
        }

        @Test
        void generateReservationQrTokenForMember_notConfirmed_throwsInvalidOperationException() {
            reservation1_confirmed.setStatus(ReservationStatusEnum.CANCELLED);
            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));
            assertThrows(InvalidOperationException.class, () -> reservationService.generateReservationQrTokenForMember(reservation1_confirmed.getId(), memberId));
        }

        @Test
        void generateReservationQrTokenForMember_sessionNotFound_throwsEntityNotFoundException() {
            when(reservationRepository.findById(reservation1_confirmed.getId())).thenReturn(Optional.of(reservation1_confirmed));
            when(sessionRepository.findById(reservation1_confirmed.getSessionId())).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> reservationService.generateReservationQrTokenForMember(reservation1_confirmed.getId(), memberId));
        }
    }

    @Test
    void getReservationsByMember_returnsPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        ReservationResponse rr = new ReservationResponse(
                reservation1_confirmed.getId(),
                memberId,
                session1_available.getId(),
                memberPackage1_active_enoughSessions_correctPool.getId(),
                ReservationStatusEnum.CONFIRMED,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                session1_available.getSessionDate(),
                session1_available.getStartTime(),
                session1_available.getEndTime(),
                "Test Pool Name",
                session1_available.isEducationSession(),
                session1_available.getCapacity() - session1_available.getCurrentBookings()
        );
        Page<ReservationResponse> expectedPage = new PageImpl<>(List.of(rr), pageable, 1);
        when(reservationRepository.findReservationResponsesByMemberId(memberId, pageable)).thenReturn(expectedPage);

        Page<ReservationResponse> actualPage = reservationService.getReservationsByMember(memberId, 0, 10);

        assertNotNull(actualPage);
        assertEquals(1, actualPage.getTotalElements());
        ReservationResponse actualResponse = actualPage.getContent().getFirst();
        assertEquals(rr.getId(), actualResponse.getId());
        assertEquals(rr.getPoolName(), actualResponse.getPoolName());
    }

    @Test
    void getMemberConfirmedReservationsOnDate_returnsList() {
        LocalDate date = LocalDate.now();
        when(reservationRepository.findByMemberIdAndStatusAndSessionDate(memberId, ReservationStatusEnum.CONFIRMED, date))
                .thenReturn(List.of(reservation1_confirmed));
        List<Reservation> result = reservationService.getMemberConfirmedReservationsOnDate(memberId, date);
        assertEquals(1, result.size());
    }

    @Test
    void hasTimeConflict_noConflict() {
        Session newSession = new Session();
        newSession.setStartTime(LocalTime.of(12,0));
        newSession.setEndTime(LocalTime.of(13,0));
        when(sessionRepository.findById(reservation1_confirmed.getSessionId())).thenReturn(Optional.of(session1_available));
        assertFalse(reservationService.hasTimeConflict(newSession, List.of(reservation1_confirmed)));
    }

    @Test
    void hasTimeConflict_withConflict() {
        Session newSession = new Session();
        newSession.setStartTime(LocalTime.of(10,30));
        newSession.setEndTime(LocalTime.of(11,30));
        when(sessionRepository.findById(reservation1_confirmed.getSessionId())).thenReturn(Optional.of(session1_available));
        assertTrue(reservationService.hasTimeConflict(newSession, List.of(reservation1_confirmed)));
    }

    @Test
    void hasTimeConflict_withSessionResponse_noConflict() {
        com.sp.SwimmingPool.dto.SessionResponse newSessionResponse = new com.sp.SwimmingPool.dto.SessionResponse(0,0,"",LocalDate.now(), LocalTime.of(12,0), LocalTime.of(13,0),0,0,0,false,true,null);
        when(sessionRepository.findById(reservation1_confirmed.getSessionId())).thenReturn(Optional.of(session1_available));
        assertFalse(reservationService.hasTimeConflict(newSessionResponse, List.of(reservation1_confirmed)));
    }


    @Test
    void processMissedReservations_marksConfirmedAsNoShow() {
        Reservation expiredConfirmedRes = new Reservation();
        expiredConfirmedRes.setId(501);
        expiredConfirmedRes.setStatus(ReservationStatusEnum.CONFIRMED);

        when(reservationRepository.findExpiredReservations(
                eq(List.of(ReservationStatusEnum.CONFIRMED)),
                any(LocalDate.class),
                any(LocalTime.class))
        ).thenReturn(List.of(expiredConfirmedRes));

        reservationService.processMissedReservations();

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertEquals(ReservationStatusEnum.NO_SHOW, captor.getValue().getStatus());
    }
}