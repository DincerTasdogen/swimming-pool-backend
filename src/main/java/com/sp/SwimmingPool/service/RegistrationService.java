package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.RegisterRequest;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.enums.MemberGenderEnum;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.repos.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;

    public boolean isEmailRegistered(String email) {
        return memberRepository.findByEmail(email).isPresent();
    }

    public boolean isIdentityNumberRegistered(String identityNumber) {
        return memberRepository.findByIdentityNumber(identityNumber).isPresent();
    }

    public void storeOAuthTempData(Map<String, Object> oauthData) {
        String email = (String) oauthData.get("email");
        // Store OAuth data in the verification service for later use
        verificationService.storeTempUserData(email, oauthData);
    }

    public Member register(RegisterRequest request) {
        // Get the user's temporary data including uploaded file paths
        Map<String, Object> userData = verificationService.getTempUserData(request.getEmail());

        // Extract file paths from temp data if available
        String photo = userData != null && userData.containsKey("photo") ?
                (String) userData.get("photo") : request.getPhoto();

        String idPhotoFront = userData != null && userData.containsKey("idPhotoFront") ?
                (String) userData.get("idPhotoFront") : request.getIdPhotoFront();

        String idPhotoBack = userData != null && userData.containsKey("idPhotoBack") ?
                (String) userData.get("idPhotoBack") : request.getIdPhotoBack();

        // Create member entity
        Member member = Member.builder()
                .name(request.getName())
                .surname(request.getSurname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .identityNumber(request.getIdentityNumber())
                .birthDate(request.getBirthDate())
                .height(request.getHeight())
                .weight(request.getWeight())
                .gender(MemberGenderEnum.valueOf(request.getGender()))
                .canSwim(request.isCanSwim())
                .status(StatusEnum.PENDING_ID_CARD_VERIFICATION)
                .photo(photo)
                .idPhotoFront(idPhotoFront)
                .idPhotoBack(idPhotoBack)
                .registrationDate(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        memberRepository.save(member);
        return member;
    }
}