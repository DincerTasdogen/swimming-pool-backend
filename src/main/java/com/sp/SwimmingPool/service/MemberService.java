package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.HealthAnswerDTO;
import com.sp.SwimmingPool.dto.MemberDTO;
import com.sp.SwimmingPool.dto.MemberHealthAssessmentDTO;
import com.sp.SwimmingPool.dto.MemberReportStatusDTO;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.MemberHealthAssessment;
import com.sp.SwimmingPool.model.enums.MemberGenderEnum;
import com.sp.SwimmingPool.model.enums.RiskLevel;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.model.enums.SwimmingLevelEnum;
import com.sp.SwimmingPool.repos.MemberHealthAssessmentRepository;
import com.sp.SwimmingPool.repos.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberHealthAssessmentRepository assessmentRepository;
    private final MemberFileService memberFileService;
    private final EmailService emailService;
    private final HealthAssessmentService healthAssessmentService;
    private final RiskAssessmentService riskAssessmentService;
    private final StorageService storageService;
    private final CoachAssignmentService coachAssignmentService;


    private Member convertToEntity(MemberDTO dto) {
        Member member = new Member();
        updateEntityFromDTO(member, dto);
        return member;
    }

    private void updateEntityFromDTO(Member member, MemberDTO dto) {
        member.setName(dto.getName());
        member.setSurname(dto.getSurname());
        member.setEmail(dto.getEmail());
        member.setIdentityNumber(dto.getIdentityNumber());
        member.setGender(MemberGenderEnum.valueOf(dto.getGender().toUpperCase()));
        member.setWeight(dto.getWeight());
        member.setHeight(dto.getHeight());
        member.setBirthDate(dto.getBirthDate());
        member.setPhoneNumber(dto.getPhoneNumber());
        member.setCanSwim(dto.isCanSwim());
        member.setStatus(StatusEnum.valueOf(dto.getStatus()));

        // Set swimming level if provided
        if (dto.getSwimmingLevel() != null && !dto.getSwimmingLevel().isEmpty()) {
            try {
                SwimmingLevelEnum level = getSwimmingLevelFromString(dto.getSwimmingLevel());
                member.setSwimmingLevel(level);

                // Update canSwim based on the swimming level
                member.setCanSwim(level != SwimmingLevelEnum.NONE);
            } catch (IllegalArgumentException e) {
                // If the string doesn't match any enum, default based on canSwim flag
                member.setSwimmingLevel(dto.isCanSwim() ? SwimmingLevelEnum.BEGINNER : SwimmingLevelEnum.NONE);
            }
        } else {
            // If no swimming level is provided, set the default based on canSwim flag
            member.setSwimmingLevel(dto.isCanSwim() ? SwimmingLevelEnum.BEGINNER : SwimmingLevelEnum.NONE);
        }

        member.setLastLessonDate(dto.getLastLessonDate());
        member.setSwimmingNotes(dto.getSwimmingNotes());
        member.setCoachId(dto.getCoachId());
        member.setUpdatedAt(dto.getUpdatedDate());
        member.setRegistrationDate(dto.getRegistrationDate());
    }

    private MemberDTO convertToDTO(Member member) {
        MemberDTO dto = new MemberDTO();
        dto.setId(member.getId());
        dto.setName(member.getName());
        dto.setSurname(member.getSurname());
        dto.setEmail(member.getEmail());
        dto.setIdentityNumber(member.getIdentityNumber());
        dto.setGender(member.getGender().name());
        dto.setWeight(member.getWeight());
        dto.setHeight(member.getHeight());
        dto.setBirthDate(member.getBirthDate());
        dto.setPhoneNumber(member.getPhoneNumber());
        dto.setCanSwim(member.isCanSwim());
        memberFileService.getBiometricPhotoPath(member.getId()).ifPresent(dto::setPhoto);
        memberFileService.getIdPhotoFrontPath(member.getId()).ifPresent(dto::setIdPhotoFront);
        memberFileService.getIdPhotoBackPath(member.getId()).ifPresent(dto::setIdPhotoBack);

        dto.setStatus(String.valueOf(member.getStatus()));

        if (member.getSwimmingLevel() != null) {
            dto.setSwimmingLevel(member.getSwimmingLevel().getDisplayName());
        }

        dto.setLastLessonDate(member.getLastLessonDate());
        dto.setSwimmingNotes(member.getSwimmingNotes());
        dto.setCoachId(member.getCoachId());
        dto.setRegistrationDate(member.getRegistrationDate());
        dto.setUpdatedDate(member.getUpdatedAt());

        Optional<MemberHealthAssessment> assessmentOpt = assessmentRepository
                .findTopByMemberIdOrderByCreatedAtDesc(member.getId());
        if (assessmentOpt.isPresent()) {
            MemberHealthAssessment assessment = assessmentOpt.get();
            dto.setLatestMedicalReportPath(assessment.getMedicalReportPath());
            dto.setRequiresMedicalReport(assessment.isRequiresMedicalReport());
        } else {
            dto.setRequiresMedicalReport(member.getStatus() == StatusEnum.PENDING_HEALTH_REPORT);
        }

        return dto;
    }


    public MemberDTO updateMember(int id, MemberDTO memberDTO) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + id));

        updateEntityFromDTO(member, memberDTO);
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);

        return convertToDTO(member);
    }

    public void updatePassword(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with email: " + email));

        String encodedPassword = passwordEncoder.encode(password);
        member.setPassword(encodedPassword);
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);
    }

    public MemberDTO findByEmail(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Member not found with email: " + email));
        return convertToDTO(member);
    }

    private SwimmingLevelEnum getSwimmingLevelFromString(String level) {
        try {
            // Try direct enum name mapping first (NONE, BEGINNER, INTERMEDIATE, ADVANCED)
            return SwimmingLevelEnum.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try display name mapping
            if (level.equalsIgnoreCase("Yüzme Bilmiyor")) {
                return SwimmingLevelEnum.NONE;
            } else if (level.equalsIgnoreCase("Başlangıç")) {
                return SwimmingLevelEnum.BEGINNER;
            } else if (level.equalsIgnoreCase("Orta")) {
                return SwimmingLevelEnum.INTERMEDIATE;
            } else if (level.equalsIgnoreCase("İleri")) {
                return SwimmingLevelEnum.ADVANCED;
            } else {
                throw new IllegalArgumentException("Invalid swimming level: " + level);
            }
        }
    }

    public MemberDTO updateSwimmingLevel(int id, String level) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + id));

        SwimmingLevelEnum swimmingLevel = getSwimmingLevelFromString(level);

        // Update both swimming level and canSwim flag
        member.setSwimmingLevel(swimmingLevel);
        member.setCanSwim(swimmingLevel != SwimmingLevelEnum.NONE);

        member.setLastLessonDate(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());

        memberRepository.save(member);
        return convertToDTO(member);
    }

    public void deleteMember(int id) {
        if (!memberRepository.existsById(id)) {
            throw new IllegalArgumentException("Member with id " + id + " not found");
        }
        memberRepository.deleteById(id);
    }

    public MemberDTO getMemberDetails(int id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + id));

        return convertToDTO(member);
    }

    public List<MemberDTO> listAllMembers() {
        return memberRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<MemberDTO> listMembersOfCoach(int coachId) {
        return memberRepository.findByCoachId(coachId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public boolean hasSwimmingAbility(int memberId) throws Exception {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new Exception("Member not found"));

        return member.isCanSwim() && member.getSwimmingLevel() != SwimmingLevelEnum.NONE;
    }

    public List<MemberDTO> getMembersByStatuses(List<StatusEnum> statuses) {
        return memberRepository.findAll().stream()
                .filter(member -> statuses.contains(member.getStatus()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public MemberDTO updateMemberStatus(int memberId, String statusStr) {
        StatusEnum status = StatusEnum.valueOf(statusStr);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + memberId));

        member.setStatus(status);
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);

        return convertToDTO(member);
    }
    public List<MemberDTO> getMembersByStatus(StatusEnum status) {
        return memberRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public int countMembersByStatus(StatusEnum status) {
        return memberRepository.countMembersByStatus(status);
    }

    // Sağlık formunu incele ve sağlık raporu gerekliyse statüyü değiştir
    public MemberDTO reviewHealthForm(int memberId, boolean requiresMedicalReport) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (requiresMedicalReport) {
            member.setStatus(StatusEnum.PENDING_HEALTH_REPORT);
        } else {
            // Assign coach if not already assigned
            if (member.getCoachId() == null) {
                member.setCoachId(coachAssignmentService.findLeastLoadedCoachId());
            }
            member.setStatus(StatusEnum.ACTIVE);
        }

        memberRepository.save(member);
        return convertToDTO(member);
    }

    public MemberReportStatusDTO getMemberReportStatus(int memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + memberId));

        Optional<MemberHealthAssessment> assessmentOpt = assessmentRepository
                .findTopByMemberIdOrderByCreatedAtDesc(memberId);

        if (assessmentOpt.isPresent()) {
            MemberHealthAssessment assessment = assessmentOpt.get();
            return new MemberReportStatusDTO(
                    member.getStatus(),
                    assessment.getDoctorNotes(),
                    assessment.isRequiresMedicalReport(),
                    assessment.getMedicalReportPath(),
                    assessment.getUpdatedAt(),
                    assessment.isDoctorApproved()
            );
        } else {
            return new MemberReportStatusDTO(
                    member.getStatus(),
                    null,
                    member.getStatus() == StatusEnum.PENDING_HEALTH_REPORT,
                    null,
                    null,
                    false
            );
        }
    }

    public MemberDTO processDoctorMedicalReportReview(
            int memberId,
            int doctorId,
            boolean isEligibleForPool,
            boolean isDocumentInvalid,
            String doctorNotes
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Üye bulunamadı Üye ID: " + memberId));

        MemberHealthAssessment assessment = assessmentRepository
                .findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Bu üyeye ait sağlık değerlendirmesi bulunamadı Üye ID: " + memberId));

        assessment.setDoctorId(doctorId);
        assessment.setDoctorNotes(doctorNotes);
        assessment.setUpdatedAt(LocalDateTime.now());

        if (isDocumentInvalid) {
            assessment.setDoctorApproved(false);
            assessment.setRequiresMedicalReport(true);


             if (assessment.getMedicalReportPath() != null && !assessment.getMedicalReportPath().isBlank()) {
                 try {
                     storageService.deleteFile(assessment.getMedicalReportPath());
                 } catch (IOException e) {
                     log.error("Could not delete invalid medical report file: {} - {}", assessment.getMedicalReportPath(), e.getMessage());
                 }
             }
            assessment.setMedicalReportPath(null);

            member.setStatus(StatusEnum.PENDING_HEALTH_REPORT);
            emailService.sendInvalidDocumentNotification(member.getEmail(), member.getName(), doctorNotes);
        } else {
            assessment.setDoctorApproved(isEligibleForPool);
            assessment.setRequiresMedicalReport(false);

            if (isEligibleForPool) {
                if (member.getCoachId() == null) {
                    member.setCoachId(coachAssignmentService.findLeastLoadedCoachId());
                }
                member.setStatus(StatusEnum.ACTIVE);
                emailService.sendRegistrationApproval(member.getEmail());
            } else {
                member.setStatus(StatusEnum.REJECTED_HEALTH_REPORT);
                emailService.sendRegistrationRejection(member.getEmail(), doctorNotes);
            }
        }

        assessmentRepository.save(assessment);
        memberRepository.save(member);

        return convertToDTO(member);
    }

    public MemberHealthAssessmentDTO getHealthAssessmentReviewForMember(int memberId) {
        MemberHealthAssessment assessment = assessmentRepository
                .findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Health assessment not found for member: " + memberId));

        RiskLevel riskLevel = riskAssessmentService.determineRiskLevel(assessment.getRiskScore());

        List<HealthAnswerDTO> answers = assessment.getAnswers().stream()
                .map(ans -> new HealthAnswerDTO(
                        ans.getQuestion().getQuestionText(),
                        ans.isAnswer(),
                        ans.getAdditionalNotes()
                ))
                .collect(Collectors.toList());

        MemberHealthAssessmentDTO dto = new MemberHealthAssessmentDTO();
        dto.setRiskScore(assessment.getRiskScore());
        dto.setRiskLevel(riskLevel.name());
        dto.setRiskLevelDescription(riskLevel.getDescription());
        dto.setRequiresMedicalReport(assessment.isRequiresMedicalReport());
        dto.setAnswers(answers);

        return dto;
    }

}