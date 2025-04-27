package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.MemberDTO;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

    private MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MemberDTO>> getMemberList() {
        try {
            List<MemberDTO> members = memberService.listAllMembers();
            if (members.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(members, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<MemberDTO> getMember(@PathVariable int id) {
        try {
            MemberDTO member = memberService.getMemberDetails(id);
            if (member == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity(member, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/coach/{coachId}")
    @PreAuthorize("hasRole('COACH')")
    public ResponseEntity<List<MemberDTO>> getMembersOfCoach(@PathVariable int coachId) {
        try {
            List<MemberDTO> members = memberService.listMembersOfCoach(coachId);
            if (members.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(members, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/edit/{memberId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MemberDTO> editMember(@PathVariable int memberId, @RequestBody MemberDTO memberDTO) {
        try {
            MemberDTO member = memberService.updateMember(memberId, memberDTO);
            return new ResponseEntity<>(member, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/edit/{memberId}/swim-status")
    @PreAuthorize("hasRole('COACH')")
    public ResponseEntity<MemberDTO> editMemberSwimStatus(@PathVariable int memberId, @RequestBody MemberDTO memberDTO) {
        try {
            MemberDTO member = memberService.getMemberDetails(memberId);
            member.setCanSwim(memberDTO.isCanSwim());
            MemberDTO updatedMember = memberService.updateMember(memberId, member);
            return new ResponseEntity<>(updatedMember, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/edit/{memberId}/swimming-level")
    @PreAuthorize("hasRole('COACH')")
    public ResponseEntity<MemberDTO> updateSwimmingLevel(@PathVariable int memberId, @RequestBody Map<String, String> payload) {
        try {
            String level = payload.get("level");
            if (level == null || level.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            MemberDTO updatedMember = memberService.updateSwimmingLevel(memberId, level);
            return new ResponseEntity<>(updatedMember, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/edit/{memberId}/swimming-notes")
    @PreAuthorize("hasRole('COACH')")
    public ResponseEntity<MemberDTO> updateSwimmingNotes(@PathVariable int memberId, @RequestBody Map<String, String> payload) {
        try {
            String notes = payload.get("notes");
            if (notes == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            MemberDTO member = memberService.getMemberDetails(memberId);
            member.setSwimmingNotes(notes);
            MemberDTO updatedMember = memberService.updateMember(memberId, member);
            return new ResponseEntity<>(updatedMember, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMember(@PathVariable int id) {
        try {
            memberService.deleteMember(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Belirli durumdaki başvuruları listele (örnek: bekleyenler)
    @GetMapping("/members/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MemberDTO>> getPendingApplications() {
        try {
            List<MemberDTO> pendingMembers = memberService.getMembersByStatuses(List.of(
                    StatusEnum.PENDING_ID_CARD_VERIFICATION,
                    StatusEnum.PENDING_PHOTO_VERIFICATION,
                    StatusEnum.PENDING_HEALTH_REPORT,
                    StatusEnum.PENDING_HEALTH_FORM_APPROVAL,
                    StatusEnum.PENDING_DOCTOR_APPROVAL
            ));

            return pendingMembers.isEmpty() ?
                    new ResponseEntity<>(HttpStatus.NO_CONTENT) :
                    new ResponseEntity<>(pendingMembers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Admin bir üyeyi belirli bir statüye güncelleyebilir
    @PutMapping("/edit/{memberId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MemberDTO> updateMemberStatus(@PathVariable int memberId, @RequestBody Map<String, String> payload) {
        try {
            String status = payload.get("status");
            if (status == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            MemberDTO updatedMember = memberService.updateMemberStatus(memberId, status);
            return new ResponseEntity<>(updatedMember, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/members/active")
    @PreAuthorize("hasRole('ADMIN')")
    public List<MemberDTO> getActiveMembers() {
        return memberService.getMembersByStatus(StatusEnum.ACTIVE);
    }
    @GetMapping("/members/rejected")
    @PreAuthorize("hasRole('ADMIN')")
    public List<MemberDTO> getRejectedMembers() {
        return memberService.getMembersByStatuses(List.of(
                StatusEnum.REJECTED_ID_CARD,
                StatusEnum.REJECTED_PHOTO,
                StatusEnum.REJECTED_HEALTH_REPORT
        ));
    }


    @GetMapping("/members/disabled")
    @PreAuthorize("hasRole('ADMIN')")
    public List<MemberDTO> getDisabledMembers() {
        return memberService.getMembersByStatus(StatusEnum.DISABLED);
    }


    @GetMapping("/{status}/count")
    @PreAuthorize("hasRole('DOCTOR')")
    public int getCountByStatus(@PathVariable StatusEnum status) {
        return memberService.countMembersByStatus(status);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<MemberDTO> getMembersByStatus(@PathVariable StatusEnum status) {
        return memberService.getMembersByStatus(status);
    }


    @PutMapping("/doctor/review-health-form/{memberId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<MemberDTO> reviewHealthForm(
            @PathVariable int memberId,
            @RequestParam boolean requiresMedicalReport) {
        return ResponseEntity.ok(memberService.reviewHealthForm(memberId, requiresMedicalReport));
    }

    @PutMapping("/doctor/review-medical-report/{memberId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<MemberDTO> reviewMedicalReport(
            @PathVariable int memberId,
            @RequestParam boolean isEligibleForPool) {
        return ResponseEntity.ok(memberService.reviewMedicalReport(memberId, isEligibleForPool));
    }


}