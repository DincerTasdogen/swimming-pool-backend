package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.MemberHealthAssessment;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.repos.MemberHealthAssessmentRepository;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.service.EmailService;
import com.sp.SwimmingPool.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MedicalReportController {

    private final MemberRepository memberRepository;
    private final MemberHealthAssessmentRepository assessmentRepository;
    private final StorageService storageService;
    private final EmailService emailService;

    @PostMapping("/{memberId}/medical-report")
    @PreAuthorize("hasRole('MEMBER') and #memberId == principal.id")
    public ResponseEntity<?> uploadMedicalReport(
            @PathVariable int memberId,
            @RequestParam("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Dosya yüklenmedi.");
        }
        if (!file.getContentType().equals("application/pdf")) {
            return ResponseEntity.badRequest().body("Sadece PDF dosyası yükleyebilirsiniz.");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body("Dosya boyutu 10MB'dan büyük olamaz.");
        }

        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Üye bulunamadı.");
        }
        Member member = memberOpt.get();

        MemberHealthAssessment assessment = assessmentRepository
                .findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .orElse(null);
        if (assessment == null) {
            return ResponseEntity.badRequest().body("Sağlık değerlendirmesi bulunamadı.");
        }

        try {
            String filePath = storageService.storeFile(file, "members/" + memberId + "/medical-report");

            assessment.setMedicalReportPath(filePath);
            assessment.setUpdatedAt(LocalDateTime.now());
            assessment.setRequiresMedicalReport(false);
            assessment.setDoctorApproved(false);
            assessment.setDoctorNotes(null);
            assessmentRepository.save(assessment);

            member.setStatus(StatusEnum.PENDING_DOCTOR_APPROVAL);
            memberRepository.save(member);

            return ResponseEntity.ok().body("Sağlık raporu başarıyla yüklendi.");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Dosya yüklenemedi: " + e.getMessage());
        }
    }

    @GetMapping("/{memberId}/medical-report")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadMedicalReport(@PathVariable int memberId) {
        MemberHealthAssessment assessment = assessmentRepository
                .findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .orElse(null);
        if (assessment == null || assessment.getMedicalReportPath() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Resource resource = storageService.loadFileAsResource(assessment.getMedicalReportPath());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"medical-report.pdf\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
