package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.RegisterRequest;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.enums.MemberGenderEnum;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.repos.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Member register(RegisterRequest request) {
        // Create new member
        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .surname(request.getSurname())
                .identityNumber(request.getIdentityNumber())
                .phoneNumber(request.getPhoneNumber())
                .birthDate(request.getBirthDate())
                .gender(MemberGenderEnum.valueOf(request.getGender()))
                .height(request.getHeight())
                .weight(request.getWeight())
                .canSwim(request.isCanSwim())
                .status(StatusEnum.PENDING_ID_CARD_VERIFICATION)
                .registrationDate(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return memberRepository.save(member);
    }

    public boolean isEmailRegistered(String email) {
        return memberRepository.findByEmail(email).isPresent();
    }

    public boolean isIdentityNumberRegistered(String identityNumber) {
        return memberRepository.findByIdentityNumber(identityNumber).isPresent();
    }
}