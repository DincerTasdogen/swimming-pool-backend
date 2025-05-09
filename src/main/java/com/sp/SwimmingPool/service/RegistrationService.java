package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.AnswerRequest;
import com.sp.SwimmingPool.dto.RegisterRequest;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.RegistrationFile;
import com.sp.SwimmingPool.model.enums.MemberGenderEnum;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.model.enums.SwimmingLevelEnum;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.repos.RegistrationFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;
    private final HealthAssessmentService healthAssessmentService;
    private final RegistrationFileRepository registrationFileRepository;

    public boolean isEmailRegistered(String email) {
        return memberRepository.findByEmail(email).isPresent();
    }

    public boolean isIdentityNumberRegistered(String identityNumber) {
        return memberRepository.findByIdentityNumber(identityNumber).isPresent();
    }

    public void storeOAuthTempData(Map<String, Object> oauthData) {
        String email = (String) oauthData.get("email");
        verificationService.storeTempUserData(email, oauthData);
    }

    public Member register(RegisterRequest request) {
        Map<String, Object> userData = verificationService.getTempUserData(request.getEmail());

        String photo = userData != null && userData.containsKey("photo") ?
                (String) userData.get("photo") : request.getPhoto();

        String idPhotoFront = userData != null && userData.containsKey("idPhotoFront") ?
                (String) userData.get("idPhotoFront") : request.getIdPhotoFront();

        String idPhotoBack = userData != null && userData.containsKey("idPhotoBack") ?
                (String) userData.get("idPhotoBack") : request.getIdPhotoBack();

        SwimmingLevelEnum level = request.isCanSwim() ? SwimmingLevelEnum.BEGINNER : SwimmingLevelEnum.NONE;

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
                .swimmingLevel(level)
                .registrationDate(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        memberRepository.save(member);

        if (photo != null) {
            registrationFileRepository.save(RegistrationFile.builder()
                    .s3Key(photo)
                    .member(member)
                    .type("biometric")
                    .uploadedAt(LocalDateTime.now())
                    .build());
        }
        if (idPhotoFront != null) {
            registrationFileRepository.save(RegistrationFile.builder()
                    .s3Key(idPhotoFront)
                    .member(member)
                    .type("id-front")
                    .uploadedAt(LocalDateTime.now())
                    .build());
        }
        if (idPhotoBack != null) {
            registrationFileRepository.save(RegistrationFile.builder()
                    .s3Key(idPhotoBack)
                    .member(member)
                    .type("id-back")
                    .uploadedAt(LocalDateTime.now())
                    .build());
        }

        if (userData != null && userData.containsKey("healthAnswers")) {
            List<AnswerRequest> healthAnswers = (List<AnswerRequest>) userData.get("healthAnswers");
            if (healthAnswers != null) {
                healthAssessmentService.createHealthAssessmentForMember(member.getId(), healthAnswers);
            }
        }

        return member;
    }
}