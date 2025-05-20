package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.DoctorReviewRequest;
import com.sp.SwimmingPool.dto.MemberDTO;
import com.sp.SwimmingPool.dto.MemberReportStatusDTO;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.MemberHealthAssessment;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.repos.MemberHealthAssessmentRepository;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.service.MemberService;
import com.sp.SwimmingPool.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Slf4j
public class MedicalReportController {

    private final MemberRepository memberRepository;
    private final MemberHealthAssessmentRepository assessmentRepository;
    private final StorageService storageService;
    private final MemberService memberService;

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    @PostMapping("/{memberId}/medical-report")
    @PreAuthorize("hasRole('MEMBER') and #memberId == principal.id")
    public ResponseEntity<?> uploadMedicalReport(
            @PathVariable int memberId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        log.info("Member ID {} (Principal ID: {}) attempting to upload medical report.", memberId, principal.getId());

        if (file == null || file.isEmpty()) {
            log.warn("Upload attempt by member {} failed: No file provided.", memberId);
            return ResponseEntity.badRequest().body("Medical report file is required.");
        }
        if (!PDF_CONTENT_TYPE.equals(file.getContentType())) {
            log.warn("Upload attempt by member {} failed: Invalid file type '{}'. Expected PDF.", memberId, file.getContentType());
            return ResponseEntity.badRequest().body("Invalid file type. Only PDF files are accepted.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            log.warn("Upload attempt by member {} failed: File size {} exceeds limit of {}MB.", memberId, file.getSize(), MAX_FILE_SIZE_BYTES / (1024 * 1024));
            return ResponseEntity.badRequest().body("File size exceeds the 10MB limit.");
        }

        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
            log.warn("Upload attempt for non-existent member ID: {}", memberId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Üye bulunamadı.");
        }
        Member member = memberOpt.get();

        if (member.getStatus() != StatusEnum.PENDING_HEALTH_REPORT) {
            log.warn("Member {} attempted to upload report with status {}, but PENDING_HEALTH_REPORT is required.", memberId, member.getStatus());
            return ResponseEntity.badRequest().body("Cannot upload medical report at this time. Current status: " + member.getStatus().name());
        }

        MemberHealthAssessment assessment = assessmentRepository
                .findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .orElseGet(() -> {
                    log.info("No existing health assessment found for member {}. Creating a new one for medical report.", memberId);
                    MemberHealthAssessment newAssessment = new MemberHealthAssessment();
                    newAssessment.setMemberId(memberId);
                    newAssessment.setCreatedAt(LocalDateTime.now());
                    newAssessment.setRequiresMedicalReport(true);
                    return newAssessment;
                });

        try {
            if (assessment.getMedicalReportPath() != null && !assessment.getMedicalReportPath().isBlank()) {
                log.info("Member {} is uploading a new medical report, attempting to delete old report: {}", memberId, assessment.getMedicalReportPath());
                try {
                    storageService.deleteFile(assessment.getMedicalReportPath());
                    log.info("Successfully deleted old medical report {} for member {}", assessment.getMedicalReportPath(), memberId);
                } catch (IOException e) {
                    log.error("Could not delete previous medical report file: {} for member {}. Error: {}",
                            assessment.getMedicalReportPath(), memberId, e.getMessage(), e);
                }
            }

            String directory = "members/" + memberId + "/medical-report";
            String filePath = storageService.storeFile(file, directory);
            log.info("Medical report for member {} stored at path: {}", memberId, filePath);

            assessment.setMedicalReportPath(filePath);
            assessment.setUpdatedAt(LocalDateTime.now());
            assessment.setRequiresMedicalReport(false);
            assessment.setDoctorApproved(false);
            assessment.setDoctorNotes(null);
            assessmentRepository.save(assessment);

            member.setStatus(StatusEnum.PENDING_DOCTOR_APPROVAL);
            memberRepository.save(member);

            log.info("Medical report for member {} uploaded successfully. Status set to PENDING_DOCTOR_APPROVAL.", memberId);
            return ResponseEntity.ok().body("Medical report uploaded successfully. It is now pending doctor review.");
        } catch (IOException e) {
            log.error("Failed to store medical report for member {}. Error: {}", memberId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload medical report due to a server error.");
        }
    }

    @GetMapping("/{memberId}/medical-report")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadMedicalReport(@PathVariable int memberId, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        log.info("User {} (Role: {}) attempting to download medical report for member ID: {}", principal.getUsername(), principal.getAuthorities(), memberId);

        MemberHealthAssessment assessment = assessmentRepository
                .findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .orElse(null);

        if (assessment == null || assessment.getMedicalReportPath() == null || assessment.getMedicalReportPath().isBlank()) {
            log.warn("Medical report not found for member ID: {} during download attempt by user {}.", memberId, principal.getUsername());
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = storageService.loadFileAsResource(assessment.getMedicalReportPath());
            log.info("Successfully loaded medical report resource {} for member {} for download by user {}.", assessment.getMedicalReportPath(), memberId, principal.getUsername());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"medical-report-" + memberId + ".pdf\"")
                    .body(resource);
        } catch (IOException e) {
            log.error("Error loading medical report file {} for member {}. Requested by user {}. Error: {}",
                    assessment.getMedicalReportPath(), memberId, principal.getUsername(), e.getMessage(), e);
            // Check if the IOException is due to "Access denied" from S3StorageService
            if (e.getMessage() != null && e.getMessage().startsWith("Access denied to file:")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/{memberId}/doctor/review-report")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> doctorReviewMedicalReport(
            @PathVariable int memberId,
            @Valid @RequestBody DoctorReviewRequest reviewRequest,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        int doctorId = principal.getId(); // Assuming UserPrincipal's ID is the staff ID
        log.info("Doctor ID {} (User: {}) submitting review for member ID: {}. Request: {}",
                doctorId, principal.getUsername(), memberId, reviewRequest);

        try {
            MemberDTO updatedMember = memberService.processDoctorMedicalReportReview(
                    memberId,
                    doctorId,
                    reviewRequest.getEligibleForPool(),
                    reviewRequest.getDocumentInvalid(),
                    reviewRequest.getDoctorNotes()
            );
            log.info("Doctor ID {} successfully processed review for member ID: {}. New status: {}",
                    doctorId, memberId, updatedMember.getStatus());
            return ResponseEntity.ok(updatedMember);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument during doctor review for member {}. Doctor ID: {}. Error: {}",
                    memberId, doctorId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during doctor review for member {}. Doctor ID: {}. Error: {}",
                    memberId, doctorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while processing the review.");
        }
    }

    @GetMapping("/{memberId}/report-status")
    @PreAuthorize("(hasRole('MEMBER') and #memberId == principal.id) or hasAnyRole('DOCTOR')")
    public ResponseEntity<MemberReportStatusDTO> getMemberReportStatusInfo(@PathVariable int memberId) {
        log.info("Fetching report status for member ID: {}", memberId);
        try {
            MemberReportStatusDTO statusDTO = memberService.getMemberReportStatus(memberId);
            return ResponseEntity.ok(statusDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Could not fetch report status for member {}: {}", memberId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching report status for member {}: {}", memberId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}