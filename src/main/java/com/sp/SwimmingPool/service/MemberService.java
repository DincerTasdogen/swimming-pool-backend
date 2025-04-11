package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.MemberDTO;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.enums.MemberGenderEnum;
import com.sp.SwimmingPool.model.enums.SwimmingLevelEnum;
import com.sp.SwimmingPool.repos.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
        member.setPhoneNumber(dto.getPhoneNumber());
        member.setIdPhotoFront(dto.getIdPhotoFront());
        member.setIdPhotoBack(dto.getIdPhotoBack());
        member.setPhoto(dto.getPhoto());
        member.setCanSwim(dto.isCanSwim());

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
        dto.setPhoneNumber(member.getPhoneNumber());
        dto.setIdPhotoFront(member.getIdPhotoFront());
        dto.setIdPhotoBack(member.getIdPhotoBack());
        dto.setPhoto(member.getPhoto());
        dto.setCanSwim(member.isCanSwim());

        // Convert swimming level enum to its display name
        if (member.getSwimmingLevel() != null) {
            dto.setSwimmingLevel(member.getSwimmingLevel().getDisplayName());
        }

        dto.setLastLessonDate(member.getLastLessonDate());
        dto.setSwimmingNotes(member.getSwimmingNotes());
        dto.setCoachId(member.getCoachId());

        return dto;
    }

    public MemberDTO createMember(MemberDTO memberDTO) {
        Member member = convertToEntity(memberDTO);
        memberRepository.save(member);
        return convertToDTO(member);
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
}