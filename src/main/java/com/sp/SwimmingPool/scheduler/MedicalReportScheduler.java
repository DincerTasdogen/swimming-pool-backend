// src/main/java/com/sp/SwimmingPool/scheduler/MedicalReportScheduler.java
package com.sp.SwimmingPool.scheduler;

import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MedicalReportScheduler {

    private final MemberRepository memberRepository;
    private final EmailService emailService;
    
    @Scheduled(cron = "0 0 2 * * *")
    public void checkPendingMedicalReports() {
        List<Member> pendingMembers = memberRepository.findByStatus(StatusEnum.PENDING_HEALTH_REPORT);
        LocalDateTime now = LocalDateTime.now();

        for (Member member : pendingMembers) {
            if (member.getUpdatedAt() == null) continue;
            long days = java.time.Duration.between(member.getUpdatedAt(), now).toDays();

            if (days == 5 || days == 10 || days == 15) {
                emailService.sendHealthReportReminder(member.getEmail());
            }
            if (days >= 20) {
                member.setStatus(StatusEnum.DISABLED);
                memberRepository.save(member);
                emailService.sendRegistrationRejection(member.getEmail(), "20 gün içinde sağlık raporu yüklenmediği için hesabınız devre dışı bırakıldı.");
            }
        }
    }
}
