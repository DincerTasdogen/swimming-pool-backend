package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.MemberDTO;
import com.sp.SwimmingPool.dto.MemberHealthAssessmentDTO;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MemberDTO>> getMemberList() {
        List<MemberDTO> members = memberService.listAllMembers();
        return members.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(members);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<MemberDTO> getMember(@PathVariable int id) {
        try {
            return ResponseEntity.ok(memberService.getMemberDetails(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/coach/{coachId}")
    @PreAuthorize("hasRole('COACH')")
    public ResponseEntity<List<MemberDTO>> getMembersOfCoach(@PathVariable int coachId) {
        List<MemberDTO> members = memberService.listMembersOfCoach(coachId);
        return members.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(members);
    }

    @PutMapping("/{memberId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MemberDTO> editMember(@PathVariable int memberId, @RequestBody MemberDTO memberDTO) {
        try {
            return ResponseEntity.ok(memberService.updateMember(memberId, memberDTO));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{memberId}/swim-status")
    @PreAuthorize("hasRole('COACH')")
    public ResponseEntity<MemberDTO> editMemberSwimStatus(@PathVariable int memberId, @RequestBody MemberDTO memberDTO) {
        try {
            MemberDTO member = memberService.getMemberDetails(memberId);
            member.setCanSwim(memberDTO.isCanSwim());
            return ResponseEntity.ok(memberService.updateMember(memberId, member));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{memberId}/swimming-level")
    @PreAuthorize("hasRole('COACH')")
    public ResponseEntity<MemberDTO> updateSwimmingLevel(@PathVariable int memberId, @RequestBody Map<String, String> payload) {
        String level = payload.get("level");
        if (level == null || level.isEmpty()) return ResponseEntity.badRequest().build();
        try {
            return ResponseEntity.ok(memberService.updateSwimmingLevel(memberId, level));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 7. Edit swimming notes (coach)
    @PutMapping("/{memberId}/swimming-notes")
    @PreAuthorize("hasRole('COACH')")
    public ResponseEntity<MemberDTO> updateSwimmingNotes(@PathVariable int memberId, @RequestBody Map<String, String> payload) {
        String notes = payload.get("notes");
        if (notes == null) return ResponseEntity.badRequest().build();
        try {
            MemberDTO member = memberService.getMemberDetails(memberId);
            member.setSwimmingNotes(notes);
            return ResponseEntity.ok(memberService.updateMember(memberId, member));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMember(@PathVariable int id) {
        try {
            memberService.deleteMember(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<MemberDTO>> getMembersByStatus(@PathVariable StatusEnum status) {
        List<MemberDTO> members = memberService.getMembersByStatus(status);
        return members.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(members);
    }

    @GetMapping("/status/{status}/count")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Integer> getCountByStatus(@PathVariable StatusEnum status) {
        return ResponseEntity.ok(memberService.countMembersByStatus(status));
    }

    @GetMapping("/by-email")
    public ResponseEntity<?> getMemberByEmail(@RequestParam String email) {
        MemberDTO member = memberService.findByEmail(email);
        return ResponseEntity.ok(Map.of("id", member.getId()));
    }

    @GetMapping("/{memberId}/health-assessment")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<MemberHealthAssessmentDTO> getHealthAssessmentReview(@PathVariable int memberId) {
        try {
            return ResponseEntity.ok(memberService.getHealthAssessmentReviewForMember(memberId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{memberId}/doctor/review-health-form")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<MemberDTO> reviewHealthForm(
            @PathVariable int memberId,
            @RequestParam boolean requiresMedicalReport) {
        return ResponseEntity.ok(memberService.reviewHealthForm(memberId, requiresMedicalReport));
    }

    @PutMapping("/{memberId}/doctor/review-medical-report")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<MemberDTO> reviewMedicalReport(
            @PathVariable int memberId,
            @RequestParam boolean isEligibleForPool) {
        return ResponseEntity.ok(memberService.reviewMedicalReport(memberId, isEligibleForPool));
    }

    @PutMapping("/{memberId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MemberDTO> updateMemberStatus(@PathVariable int memberId, @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        if (status == null) return ResponseEntity.badRequest().build();
        try {
            return ResponseEntity.ok(memberService.updateMemberStatus(memberId, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MemberDTO>> getPendingApplications() {
        List<MemberDTO> pendingMembers = memberService.getMembersByStatuses(List.of(
                StatusEnum.PENDING_ID_CARD_VERIFICATION,
                StatusEnum.PENDING_PHOTO_VERIFICATION,
                StatusEnum.PENDING_HEALTH_REPORT,
                StatusEnum.PENDING_HEALTH_FORM_APPROVAL,
                StatusEnum.PENDING_DOCTOR_APPROVAL
        ));
        return pendingMembers.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pendingMembers);
    }

    // List all active members
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MemberDTO>> getActiveMembers() {
        List<MemberDTO> activeMembers = memberService.getMembersByStatus(StatusEnum.ACTIVE);
        return activeMembers.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(activeMembers);
    }

    // List all rejected members
    @GetMapping("/rejected")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MemberDTO>> getRejectedMembers() {
        List<MemberDTO> rejectedMembers = memberService.getMembersByStatuses(List.of(
                StatusEnum.REJECTED_ID_CARD,
                StatusEnum.REJECTED_PHOTO,
                StatusEnum.REJECTED_HEALTH_REPORT
        ));
        return rejectedMembers.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(rejectedMembers);
    }

    // List all disabled members
    @GetMapping("/disabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MemberDTO>> getDisabledMembers() {
        List<MemberDTO> disabledMembers = memberService.getMembersByStatus(StatusEnum.DISABLED);
        return disabledMembers.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(disabledMembers);
    }
}