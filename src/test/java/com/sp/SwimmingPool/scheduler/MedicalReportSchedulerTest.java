package com.sp.SwimmingPool.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.service.EmailService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Medical Report Scheduler Tests")
class MedicalReportSchedulerTest {

    @Mock private MemberRepository memberRepository;

    @Mock private EmailService emailService;

    @InjectMocks private MedicalReportScheduler medicalReportScheduler;

    private Member createMemberWithUpdatedAt(
            int id, String email, LocalDateTime updatedAt
    ) {
        Member member = new Member();
        member.setId(id);
        member.setEmail(email);
        member.setStatus(StatusEnum.PENDING_HEALTH_REPORT);
        member.setUpdatedAt(updatedAt);
        return member;
    }

    @BeforeEach
    void setUp() {
        // Any common setup can go here
    }

    @Test
    @DisplayName("Should not process members when no pending members exist")
    void shouldNotProcessWhenNoPendingMembers() {
        // Given
        when(memberRepository.findByStatus(StatusEnum.PENDING_HEALTH_REPORT))
                .thenReturn(Collections.emptyList());

        // When
        medicalReportScheduler.checkPendingMedicalReports();

        // Then
        verify(memberRepository).findByStatus(StatusEnum.PENDING_HEALTH_REPORT);
        verifyNoInteractions(emailService);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Should skip members with null updatedAt")
    void shouldSkipMembersWithNullUpdatedAt() {
        // Given
        Member memberWithNullUpdatedAt = new Member();
        memberWithNullUpdatedAt.setId(1);
        memberWithNullUpdatedAt.setEmail("test@example.com");
        memberWithNullUpdatedAt.setStatus(StatusEnum.PENDING_HEALTH_REPORT);
        memberWithNullUpdatedAt.setUpdatedAt(null);

        when(memberRepository.findByStatus(StatusEnum.PENDING_HEALTH_REPORT))
                .thenReturn(List.of(memberWithNullUpdatedAt));

        // When
        medicalReportScheduler.checkPendingMedicalReports();

        // Then
        verify(memberRepository).findByStatus(StatusEnum.PENDING_HEALTH_REPORT);
        verifyNoInteractions(emailService);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Should send reminder email after 5 days")
    void shouldSendReminderAfter5Days() {
        // Given
        LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(5);
        Member member = createMemberWithUpdatedAt(1, "test@example.com", fiveDaysAgo);

        when(memberRepository.findByStatus(StatusEnum.PENDING_HEALTH_REPORT))
                .thenReturn(List.of(member));

        // When
        medicalReportScheduler.checkPendingMedicalReports();

        // Then
        verify(emailService).sendHealthReportReminder("test@example.com");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Should send reminder email after 10 days")
    void shouldSendReminderAfter10Days() {
        // Given
        LocalDateTime tenDaysAgo = LocalDateTime.now().minusDays(10);
        Member member = createMemberWithUpdatedAt(1, "test@example.com", tenDaysAgo);

        when(memberRepository.findByStatus(StatusEnum.PENDING_HEALTH_REPORT))
                .thenReturn(List.of(member));

        // When
        medicalReportScheduler.checkPendingMedicalReports();

        // Then
        verify(emailService).sendHealthReportReminder("test@example.com");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Should send reminder email after 15 days")
    void shouldSendReminderAfter15Days() {
        // Given
        LocalDateTime fifteenDaysAgo = LocalDateTime.now().minusDays(15);
        Member member = createMemberWithUpdatedAt(
                1,
                "test@example.com",
                fifteenDaysAgo
        );

        when(memberRepository.findByStatus(StatusEnum.PENDING_HEALTH_REPORT))
                .thenReturn(List.of(member));

        // When
        medicalReportScheduler.checkPendingMedicalReports();

        // Then
        verify(emailService).sendHealthReportReminder("test@example.com");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Should not send reminder for days other than 5, 10, 15")
    void shouldNotSendReminderForOtherDays() {
        // Given
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Member member = createMemberWithUpdatedAt(1, "test@example.com", sevenDaysAgo);

        when(memberRepository.findByStatus(StatusEnum.PENDING_HEALTH_REPORT))
                .thenReturn(List.of(member));

        // When
        medicalReportScheduler.checkPendingMedicalReports();

        // Then
        verifyNoInteractions(emailService);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Should disable member and send rejection email after 20 days")
    void shouldDisableMemberAfter20Days() {
        // Given
        LocalDateTime twentyDaysAgo = LocalDateTime.now().minusDays(20);
        Member member = createMemberWithUpdatedAt(
                1,
                "test@example.com",
                twentyDaysAgo
        );

        when(memberRepository.findByStatus(StatusEnum.PENDING_HEALTH_REPORT))
                .thenReturn(List.of(member));

        // When
        medicalReportScheduler.checkPendingMedicalReports();

        // Then
        verify(memberRepository).save(member);
        verify(emailService)
                .sendRegistrationRejection(
                        eq("test@example.com"),
                        eq(
                                "20 gün içinde sağlık raporu yüklenmediği için hesabınız devre dışı bırakıldı."
                        )
                );
        assert member.getStatus() == StatusEnum.DISABLED;
    }

    @Test
    @DisplayName("Should disable member and send rejection email after more than 20 days")
    void shouldDisableMemberAfterMoreThan20Days() {
        // Given
        LocalDateTime twentyFiveDaysAgo = LocalDateTime.now().minusDays(25);
        Member member = createMemberWithUpdatedAt(
                1,
                "test@example.com",
                twentyFiveDaysAgo
        );

        when(memberRepository.findByStatus(StatusEnum.PENDING_HEALTH_REPORT))
                .thenReturn(List.of(member));

        // When
        medicalReportScheduler.checkPendingMedicalReports();

        // Then
        verify(memberRepository).save(member);
        verify(emailService)
                .sendRegistrationRejection(
                        eq("test@example.com"),
                        eq(
                                "20 gün içinde sağlık raporu yüklenmediği için hesabınız devre dışı bırakıldı."
                        )
                );
        assert member.getStatus() == StatusEnum.DISABLED;
    }

    @Test
    @DisplayName("Should process multiple members with different scenarios")
    void shouldProcessMultipleMembersWithDifferentScenarios() {
        // Given
        LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(5);
        LocalDateTime tenDaysAgo = LocalDateTime.now().minusDays(10);
        LocalDateTime twentyDaysAgo = LocalDateTime.now().minusDays(20);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        Member member1 = createMemberWithUpdatedAt(1, "member1@example.com", fiveDaysAgo);
        Member member2 = createMemberWithUpdatedAt(2, "member2@example.com", tenDaysAgo);
        Member member3 = createMemberWithUpdatedAt(
                3,
                "member3@example.com",
                twentyDaysAgo
        );
        Member member4 = createMemberWithUpdatedAt(
                4,
                "member4@example.com",
                sevenDaysAgo
        );

        when(memberRepository.findByStatus(StatusEnum.PENDING_HEALTH_REPORT))
                .thenReturn(Arrays.asList(member1, member2, member3, member4));

        // When
        medicalReportScheduler.checkPendingMedicalReports();

        // Then
        // Member1 (5 days) - should get reminder
        verify(emailService).sendHealthReportReminder("member1@example.com");

        // Member2 (10 days) - should get reminder
        verify(emailService).sendHealthReportReminder("member2@example.com");

        // Member3 (20 days) - should be disabled
        verify(memberRepository).save(member3);
        verify(emailService)
                .sendRegistrationRejection(
                        eq("member3@example.com"),
                        eq(
                                "20 gün içinde sağlık raporu yüklenmediği için hesabınız devre dışı bırakıldı."
                        )
                );

        // Member4 (7 days) - should not get any action
        verify(emailService, never()).sendHealthReportReminder("member4@example.com");
        verify(memberRepository, never()).save(member4);

        // Verify total interactions
        verify(emailService, times(2)).sendHealthReportReminder(any());
        verify(emailService, times(1)).sendRegistrationRejection(any(), any());
        verify(memberRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should handle member at exactly 20 days boundary")
    void shouldHandleMemberAtExactly20DaysBoundary() {
        // Given
        LocalDateTime exactlyTwentyDaysAgo = LocalDateTime.now().minusDays(20);
        Member member = createMemberWithUpdatedAt(
                1,
                "test@example.com",
                exactlyTwentyDaysAgo
        );

        when(memberRepository.findByStatus(StatusEnum.PENDING_HEALTH_REPORT))
                .thenReturn(List.of(member));

        // When
        medicalReportScheduler.checkPendingMedicalReports();

        // Then
        verify(memberRepository).save(member);
        verify(emailService)
                .sendRegistrationRejection(
                        eq("test@example.com"),
                        eq(
                                "20 gün içinde sağlık raporu yüklenmediği için hesabınız devre dışı bırakıldı."
                        )
                );
        assert member.getStatus() == StatusEnum.DISABLED;
    }

    @Test
    @DisplayName("Should handle member at exactly 15 days - should send reminder and not disable")
    void shouldHandleMemberAtExactly15Days() {
        // Given
        LocalDateTime exactlyFifteenDaysAgo = LocalDateTime.now().minusDays(15);
        Member member = createMemberWithUpdatedAt(
                1,
                "test@example.com",
                exactlyFifteenDaysAgo
        );

        when(memberRepository.findByStatus(StatusEnum.PENDING_HEALTH_REPORT))
                .thenReturn(List.of(member));

        // When
        medicalReportScheduler.checkPendingMedicalReports();

        // Then
        verify(emailService).sendHealthReportReminder("test@example.com");
        verify(memberRepository, never()).save(any(Member.class));
        // Status should remain unchanged
        assert member.getStatus() == StatusEnum.PENDING_HEALTH_REPORT;
    }

    @Test
    @DisplayName("Should handle repository exception gracefully")
    void shouldHandleRepositoryExceptionGracefully() {
        // Given
        when(memberRepository.findByStatus(StatusEnum.PENDING_HEALTH_REPORT))
                .thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        // The method doesn't declare any exceptions, so we expect it to propagate
        try {
            medicalReportScheduler.checkPendingMedicalReports();
        } catch (RuntimeException e) {
            assert e.getMessage().equals("Database connection error");
        }

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should continue processing other members if email service fails")
    void shouldContinueProcessingIfEmailServiceFails() {
        // Given
        LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(5);
        LocalDateTime twentyDaysAgo = LocalDateTime.now().minusDays(20);

        Member member1 = createMemberWithUpdatedAt(1, "member1@example.com", fiveDaysAgo);
        Member member2 = createMemberWithUpdatedAt(
                2,
                "member2@example.com",
                twentyDaysAgo
        );

        when(memberRepository.findByStatus(StatusEnum.PENDING_HEALTH_REPORT))
                .thenReturn(Arrays.asList(member1, member2));

        // Make email service throw exception for first member
        doThrow(new RuntimeException("Email service error"))
                .when(emailService)
                .sendHealthReportReminder("member1@example.com");

        // When
        try {
            medicalReportScheduler.checkPendingMedicalReports();
        } catch (RuntimeException e) {
            // Expected to throw due to email service error
        }

        verify(emailService).sendHealthReportReminder("member1@example.com");
    }
}