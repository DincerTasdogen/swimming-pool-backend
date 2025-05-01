package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.MemberHealthAssessment;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.repos.MemberHealthAssessmentRepository;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorReviewController {

    private final MemberRepository memberRepository;
    private final MemberHealthAssessmentRepository assessmentRepository;
    private final EmailService emailService;

    @PostMapping("/review-medical-report/{memberId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> reviewMedicalReport(
            @PathVariable int memberId,
            @RequestBody Map<String, Object> request
    ) {
        Boolean approved = (Boolean) request.get("approved");
        String doctorNotes = (String) request.get("doctorNotes");

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Üye bulunamadı."));

        MemberHealthAssessment assessment = assessmentRepository
                .findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .orElseThrow(() -> new RuntimeException("Sağlık değerlendirmesi bulunamadı."));

        assessment.setDoctorNotes(doctorNotes);
        assessment.setDoctorApproved(Boolean.TRUE.equals(approved));
        assessment.setUpdatedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(approved)) {
            member.setStatus(StatusEnum.ACTIVE);
            emailService.sendRegistrationApproval(member.getEmail());
        } else {
            member.setStatus(StatusEnum.REJECTED_HEALTH_REPORT);
            emailService.sendRegistrationRejection(member.getEmail(), doctorNotes);
        }

        assessmentRepository.save(assessment);
        memberRepository.save(member);

        return ResponseEntity.ok().body("Değerlendirme kaydedildi.");
    }
}

