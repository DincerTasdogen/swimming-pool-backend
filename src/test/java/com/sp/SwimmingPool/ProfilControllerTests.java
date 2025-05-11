package com.sp.SwimmingPool;

import com.sp.SwimmingPool.controller.ProfileController;
import com.sp.SwimmingPool.dto.MemberDTO;
import com.sp.SwimmingPool.dto.UserDTO;
import com.sp.SwimmingPool.service.MemberService;
import com.sp.SwimmingPool.service.UserService;
import com.sp.SwimmingPool.service.StorageService;
import com.sp.SwimmingPool.util.FilePathUtil;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ProfilControllerTests {

    @Mock
    private MemberService memberService;

    @Mock
    private UserService userService;

    @Mock
    private StorageService storageService;

    @Mock
    private FilePathUtil filePathUtil;

    @InjectMocks
    private ProfileController profileController;

    @Test
    public void getMemberProfileDetails_shouldReturnMemberDetails() {
        int memberId = 1;
        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setPhoto("photoPath");
        memberDTO.setIdPhotoFront("idFrontPath");
        memberDTO.setIdPhotoBack("idBackPath");

        when(memberService.getMemberDetails(memberId)).thenReturn(memberDTO);
        when(filePathUtil.getFileUrl("photoPath")).thenReturn("photoUrl");
        when(filePathUtil.getFileUrl("idFrontPath")).thenReturn("idFrontUrl");
        when(filePathUtil.getFileUrl("idBackPath")).thenReturn("idBackUrl");

        ResponseEntity<?> response = profileController.getMemberProfileDetails(memberId);

        assertEquals(200, response.getStatusCodeValue());
        MemberDTO responseBody = (MemberDTO) response.getBody();
        assertNotNull(responseBody);
        assertEquals("photoUrl", responseBody.getPhoto());
        assertEquals("idFrontUrl", responseBody.getIdPhotoFront());
        assertEquals("idBackUrl", responseBody.getIdPhotoBack());
    }

    @Test
    public void getStaffProfileDetails_shouldReturnStaffDetails() {
        int userId = 1;
        UserDTO userDTO = new UserDTO();
        userDTO.setName("Staff Member");

        when(userService.getUserDetails(userId)).thenReturn(userDTO);

        ResponseEntity<?> response = profileController.getStaffProfileDetails(userId);

        assertEquals(200, response.getStatusCodeValue());
        UserDTO responseBody = (UserDTO) response.getBody();
        assertNotNull(responseBody);
        assertEquals("Staff Member", responseBody.getName());
    }

    @Test
    public void updateStaffProfileDetails_shouldUpdateStaffDetails() {
        int userId = 1;
        UserDTO userDTO = new UserDTO();
        userDTO.setName("Updated Staff");

        when(userService.updateUser(eq(userId), any(UserDTO.class))).thenReturn(userDTO);

        ResponseEntity<?> response = profileController.updateStaffProfileDetails(userId, userDTO);

        assertEquals(200, response.getStatusCodeValue());
        verify(userService).updateUser(eq(userId), any(UserDTO.class)); // ❗ çağrıldığını kontrol et
    }


    @Test
    public void updateMemberProfileDetails_shouldUpdateMemberDetails() {
        int memberId = 1;
        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setName("Updated Member");

        // Mock updateMember to return the updated DTO
        when(memberService.updateMember(eq(memberId), any(MemberDTO.class)))
                .thenReturn(memberDTO);

        ResponseEntity<?> response = profileController.updateMemberProfileDetails(memberId, memberDTO);

        assertEquals(200, response.getStatusCodeValue());
    }
    @Test
    public void uploadMemberPhoto_shouldUploadPhotoSuccessfully() throws Exception {
        int memberId = 1;
        String photoPath = "uploaded/photoPath.jpg";
        String expectedUrl = "http://localhost/files/photoPath.jpg";

        MultipartFile mockPhoto = Mockito.mock(MultipartFile.class);

        when(storageService.storeFile(mockPhoto, "members/photos")).thenReturn(photoPath);
        when(filePathUtil.getFileUrl(photoPath)).thenReturn(expectedUrl);
        when(memberService.updateMember(eq(memberId), any(MemberDTO.class))).thenReturn(new MemberDTO());

        ResponseEntity<?> response = profileController.uploadMemberPhoto(memberId, mockPhoto);

        assertEquals(200, response.getStatusCodeValue());

        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();

        assertNotNull(responseBody);
        assertTrue(responseBody.containsKey("photoUrl"));
        assertEquals(expectedUrl, responseBody.get("photoUrl"));


        verify(storageService).storeFile(mockPhoto, "members/photos");
        verify(memberService).updateMember(eq(memberId), any(MemberDTO.class));
    }

}
