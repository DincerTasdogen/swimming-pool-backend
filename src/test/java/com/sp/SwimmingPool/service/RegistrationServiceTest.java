package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.AnswerRequest;
import com.sp.SwimmingPool.dto.RegisterRequest;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.MemberHealthAssessment;
import com.sp.SwimmingPool.model.entity.RegistrationFile;
import com.sp.SwimmingPool.model.enums.MemberGenderEnum;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.model.enums.SwimmingLevelEnum;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.repos.RegistrationFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private VerificationService verificationService;
    @Mock
    private HealthAssessmentService healthAssessmentService;
    @Mock
    private RegistrationFileRepository registrationFileRepository;

    @InjectMocks
    private RegistrationService registrationService;

    private RegisterRequest registerRequest;
    private Map<String, Object> oauthTempData;
    private List<AnswerRequest> healthAnswersDto;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Test");
        registerRequest.setSurname("User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setPhoneNumber("1234567890");
        registerRequest.setIdentityNumber("11223344556");
        registerRequest.setBirthDate(LocalDate.of(1990, 1, 1));
        registerRequest.setHeight(175.0);
        registerRequest.setWeight(70.0);
        registerRequest.setGender(MemberGenderEnum.MALE.name());
        registerRequest.setCanSwim(true);
        registerRequest.setPhoto("s3key/request/photo.jpg");
        registerRequest.setIdPhotoFront("s3key/request/id_front.jpg");
        registerRequest.setIdPhotoBack("s3key/request/id_back.jpg");


        oauthTempData = new HashMap<>();
        oauthTempData.put("email", "oauth@example.com");
        oauthTempData.put("photo", "s3key/oauth/photo_oauth.jpg");
        oauthTempData.put("idPhotoFront", "s3key/oauth/id_front_oauth.jpg");
        oauthTempData.put("idPhotoBack", "s3key/oauth/id_back_oauth.jpg");

        AnswerRequest answer1 = new AnswerRequest();
        answer1.setQuestionId(1L);
        answer1.setAnswer(true);
        healthAnswersDto = List.of(answer1);
        oauthTempData.put("healthAnswers", healthAnswersDto);
    }

    @Test
    void isEmailRegistered_emailExists_returnsTrue() {
        when(memberRepository.findByEmail("exists@example.com")).thenReturn(Optional.of(new Member()));
        assertTrue(registrationService.isEmailRegistered("exists@example.com"));
    }

    @Test
    void isEmailRegistered_emailNotExists_returnsFalse() {
        when(memberRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        assertFalse(registrationService.isEmailRegistered("new@example.com"));
    }

    @Test
    void isIdentityNumberRegistered_idExists_returnsTrue() {
        when(memberRepository.findByIdentityNumber("123")).thenReturn(Optional.of(new Member()));
        assertTrue(registrationService.isIdentityNumberRegistered("123"));
    }

    @Test
    void isIdentityNumberRegistered_idNotExists_returnsFalse() {
        when(memberRepository.findByIdentityNumber("456")).thenReturn(Optional.empty());
        assertFalse(registrationService.isIdentityNumberRegistered("456"));
    }

    @Test
    void storeOAuthTempData_callsVerificationService() {
        Map<String, Object> data = Map.of("key", "value");
        registrationService.storeOAuthTempData(data);
        verify(verificationService).storeTempUserData(null, data);
    }

    @Test
    void storeOAuthTempData_withEmail_callsVerificationServiceWithEmail() {
        Map<String, Object> dataWithEmail = new HashMap<>();
        dataWithEmail.put("email", "test@oauth.com");
        dataWithEmail.put("otherKey", "value");
        registrationService.storeOAuthTempData(dataWithEmail);
        verify(verificationService).storeTempUserData("test@oauth.com", dataWithEmail);
    }


    @Test
    void register_withRequestDataOnly_noOAuthData_noHealthAnswers_success() {
        when(verificationService.getTempUserData(registerRequest.getEmail())).thenReturn(null);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member m = invocation.getArgument(0);
            m.setId(1); // Simulate ID generation
            return m;
        });
        when(registrationFileRepository.save(any(RegistrationFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Member registeredMember = registrationService.register(registerRequest);

        assertNotNull(registeredMember);
        assertEquals(1, registeredMember.getId());
        assertEquals(registerRequest.getName(), registeredMember.getName());
        assertEquals("encodedPassword", registeredMember.getPassword());
        assertEquals(StatusEnum.PENDING_ID_CARD_VERIFICATION, registeredMember.getStatus());
        assertEquals(SwimmingLevelEnum.BEGINNER, registeredMember.getSwimmingLevel());
        assertTrue(registeredMember.isCanSwim());

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertEquals(registerRequest.getEmail(), memberCaptor.getValue().getEmail());

        // Verify 3 files saved from request
        ArgumentCaptor<RegistrationFile> fileCaptor = ArgumentCaptor.forClass(RegistrationFile.class);
        verify(registrationFileRepository, times(3)).save(fileCaptor.capture());
        List<RegistrationFile> savedFiles = fileCaptor.getAllValues();
        assertTrue(savedFiles.stream().anyMatch(f -> f.getS3Key().equals(registerRequest.getPhoto()) && f.getType().equals("biometric")));
        assertTrue(savedFiles.stream().anyMatch(f -> f.getS3Key().equals(registerRequest.getIdPhotoFront()) && f.getType().equals("id-front")));
        assertTrue(savedFiles.stream().anyMatch(f -> f.getS3Key().equals(registerRequest.getIdPhotoBack()) && f.getType().equals("id-back")));

        verify(healthAssessmentService, never()).createHealthAssessmentForMember(anyInt(), anyList());
    }

    @Test
    void register_withOAuthDataOverride_andHealthAnswers_success() {
        registerRequest.setEmail("oauth@example.com");
        registerRequest.setCanSwim(false);
        when(verificationService.getTempUserData("oauth@example.com")).thenReturn(oauthTempData);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPasswordOAuth");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member m = invocation.getArgument(0);
            m.setId(2);
            return m;
        });
        when(registrationFileRepository.save(any(RegistrationFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(healthAssessmentService.createHealthAssessmentForMember(eq(2), eq(healthAnswersDto)))
                .thenReturn(new MemberHealthAssessment()); // Mock successful health assessment creation

        Member registeredMember = registrationService.register(registerRequest);

        assertNotNull(registeredMember);
        assertEquals(2, registeredMember.getId());
        assertEquals(SwimmingLevelEnum.NONE, registeredMember.getSwimmingLevel()); // Because canSwim is false
        assertFalse(registeredMember.isCanSwim());


        ArgumentCaptor<RegistrationFile> fileCaptor = ArgumentCaptor.forClass(RegistrationFile.class);
        verify(registrationFileRepository, times(3)).save(fileCaptor.capture());
        List<RegistrationFile> savedFiles = fileCaptor.getAllValues();
        // Verify files from OAuth data are used
        assertTrue(savedFiles.stream().anyMatch(f -> f.getS3Key().equals(oauthTempData.get("photo")) && f.getType().equals("biometric")));
        assertTrue(savedFiles.stream().anyMatch(f -> f.getS3Key().equals(oauthTempData.get("idPhotoFront")) && f.getType().equals("id-front")));
        assertTrue(savedFiles.stream().anyMatch(f -> f.getS3Key().equals(oauthTempData.get("idPhotoBack")) && f.getType().equals("id-back")));

        verify(healthAssessmentService).createHealthAssessmentForMember(eq(2), eq(healthAnswersDto));
    }

    @Test
    void register_noFilePathsInRequestOrOAuth_savesNoFiles() {
        registerRequest.setPhoto(null);
        registerRequest.setIdPhotoFront(null);
        registerRequest.setIdPhotoBack(null);
        Map<String, Object> emptyOAuthData = new HashMap<>(); // OAuth data exists but no file paths
        emptyOAuthData.put("email", registerRequest.getEmail());


        when(verificationService.getTempUserData(registerRequest.getEmail())).thenReturn(emptyOAuthData);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member m = invocation.getArgument(0);
            m.setId(3);
            return m;
        });

        registrationService.register(registerRequest);

        verify(registrationFileRepository, never()).save(any(RegistrationFile.class));
        verify(healthAssessmentService, never()).createHealthAssessmentForMember(anyInt(), anyList());
    }

    @Test
    void register_oauthDataHasNullFilePaths_usesRequestPathsIfAvailable() {
        Map<String, Object> oauthDataWithNullFiles = new HashMap<>();
        oauthDataWithNullFiles.put("email", registerRequest.getEmail());
        oauthDataWithNullFiles.put("photo", null); // Null in OAuth
        // idPhotoFront and idPhotoBack not in oauthDataWithNullFiles, so request ones should be used

        when(verificationService.getTempUserData(registerRequest.getEmail())).thenReturn(oauthDataWithNullFiles);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member m = invocation.getArgument(0);
            m.setId(4);
            return m;
        });
        when(registrationFileRepository.save(any(RegistrationFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registrationService.register(registerRequest);

        ArgumentCaptor<RegistrationFile> fileCaptor = ArgumentCaptor.forClass(RegistrationFile.class);
        verify(registrationFileRepository, times(2)).save(fileCaptor.capture());
        List<RegistrationFile> savedFiles = fileCaptor.getAllValues();

        assertTrue(savedFiles.stream().anyMatch(f -> f.getS3Key().equals(registerRequest.getIdPhotoFront()) && f.getType().equals("id-front")));
        assertTrue(savedFiles.stream().anyMatch(f -> f.getS3Key().equals(registerRequest.getIdPhotoBack()) && f.getType().equals("id-back")));
    }

    @Test
    void register_oauthDataHasSomeFiles_requestHasOthers_usesOAuthWhenPresent() {
        // OAuth has photo, request has ID photos
        Map<String, Object> partialOAuthData = new HashMap<>();
        partialOAuthData.put("email", registerRequest.getEmail());
        partialOAuthData.put("photo", "s3key/oauth/partial_photo.jpg");
        // No idPhotoFront or idPhotoBack in partialOAuthData

        when(verificationService.getTempUserData(registerRequest.getEmail())).thenReturn(partialOAuthData);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member m = invocation.getArgument(0);
            m.setId(5);
            return m;
        });
        when(registrationFileRepository.save(any(RegistrationFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registrationService.register(registerRequest);

        ArgumentCaptor<RegistrationFile> fileCaptor = ArgumentCaptor.forClass(RegistrationFile.class);
        verify(registrationFileRepository, times(3)).save(fileCaptor.capture());
        List<RegistrationFile> savedFiles = fileCaptor.getAllValues();

        // Photo from OAuth
        assertTrue(savedFiles.stream().anyMatch(f -> f.getS3Key().equals("s3key/oauth/partial_photo.jpg") && f.getType().equals("biometric")));
        // ID photos from request
        assertTrue(savedFiles.stream().anyMatch(f -> f.getS3Key().equals(registerRequest.getIdPhotoFront()) && f.getType().equals("id-front")));
        assertTrue(savedFiles.stream().anyMatch(f -> f.getS3Key().equals(registerRequest.getIdPhotoBack()) && f.getType().equals("id-back")));
    }

    @Test
    void register_oauthDataHasNoHealthAnswers_doesNotCallHealthAssessment() {
        Map<String, Object> oauthDataNoHealth = new HashMap<>(oauthTempData);
        oauthDataNoHealth.remove("healthAnswers"); // Remove health answers

        registerRequest.setEmail("oauth@example.com");

        when(verificationService.getTempUserData("oauth@example.com")).thenReturn(oauthDataNoHealth);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPasswordOAuth");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member m = invocation.getArgument(0);
            m.setId(6);
            return m;
        });

        registrationService.register(registerRequest);

        verify(healthAssessmentService, never()).createHealthAssessmentForMember(anyInt(), anyList());
    }
}