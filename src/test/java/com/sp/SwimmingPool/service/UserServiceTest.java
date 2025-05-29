package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.UserDTO;
import com.sp.SwimmingPool.model.entity.User;
import com.sp.SwimmingPool.model.enums.UserRoleEnum;
import com.sp.SwimmingPool.repos.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserDTO userDTO;
    private User user;

    @BeforeEach
    public void setUp() {
        userDTO = new UserDTO();
        userDTO.setName("John");
        userDTO.setSurname("Doe");
        userDTO.setEmail("john.doe@example.com");
        userDTO.setRole(UserRoleEnum.DOCTOR);
        userDTO.setMemberCount(10);

        user = new User();
        user.setId(1);
        user.setName("John");
        user.setSurname("Doe");
        user.setEmail("john.doe@example.com");
        userDTO.setRole(UserRoleEnum.DOCTOR);
        user.setMemberCount(10);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    public void createUser_shouldCreateUserSuccessfully() {
        when(userRepository.findByEmail(userDTO.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        User createdUser = userService.createUser(userDTO);

        assertNotNull(createdUser);
        assertEquals(user.getName(), createdUser.getName());
        assertEquals(user.getEmail(), createdUser.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void createUser_shouldThrowExceptionWhenEmailExists() {
        when(userRepository.findByEmail(userDTO.getEmail())).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.createUser(userDTO));

        assertEquals("Email already in use: john.doe@example.com", exception.getMessage());
    }

    @Test
    public void deleteUser_shouldDeleteUserSuccessfully() {
        when(userRepository.existsById(1)).thenReturn(true);

        userService.deleteUser(1);

        verify(userRepository, times(1)).deleteById(1);
    }

    @Test
    public void deleteUser_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.existsById(1)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.deleteUser(1));

        assertEquals("User with id 1 not found", exception.getMessage());
    }

    @Test
    public void updateUser_shouldUpdateUserSuccessfully() {
        UserDTO updatedDTO = new UserDTO();
        updatedDTO.setName("Updated Name");
        updatedDTO.setSurname("Updated Surname");
        updatedDTO.setRole(UserRoleEnum.DOCTOR);  // DOCTOR olarak güncellendi.
        updatedDTO.setMemberCount(20);

        User user = new User();
        user.setId(1);
        user.setName("Old Name");
        user.setSurname("Old Surname");
        user.setRole(UserRoleEnum.ADMIN);  // Eski role değeri.
        user.setMemberCount(10);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            // Kaydedilen user nesnesi güncellenmiş olacak
            User savedUser = invocation.getArgument(0);
            savedUser.setRole(updatedDTO.getRole());
            savedUser.setName(updatedDTO.getName());
            savedUser.setSurname(updatedDTO.getSurname());
            savedUser.setMemberCount(updatedDTO.getMemberCount());
            return savedUser;
        });

        UserDTO updatedUserDTO = userService.updateUser(1, updatedDTO);

        assertEquals("Updated Name", updatedUserDTO.getName());
        assertEquals("Updated Surname", updatedUserDTO.getSurname());
        assertEquals(UserRoleEnum.DOCTOR, updatedUserDTO.getRole());  // DOCTOR rolü bekleniyor
        assertEquals(20, updatedUserDTO.getMemberCount());
    }

    @Test
    public void updateUser_shouldThrowExceptionWhenUserNotFound() {
        UserDTO updatedDTO = new UserDTO();
        updatedDTO.setName("Updated Name");

        when(userRepository.findById(1)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(1, updatedDTO));

        assertEquals("User not found with id: 1", exception.getMessage());
    }

    @Test
    public void listAllUsers_shouldReturnListOfUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDTO> userList = userService.listAllUsers();

        assertNotNull(userList);
        assertFalse(userList.isEmpty());
        assertEquals(1, userList.size());
    }

    @Test
    public void getUserDetails_shouldReturnUserDetails() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        UserDTO userDTO = userService.getUserDetails(1);

        assertNotNull(userDTO);
        assertEquals("John", userDTO.getName());
    }

    @Test
    public void storePasswordToken_shouldStoreTokenSuccessfully() {
        String token = "passwordResetToken";
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        boolean result = userService.storePasswordToken(1, token, expiryDate);

        assertTrue(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void storePasswordToken_shouldReturnFalseWhenUserNotFound() {
        String token = "passwordResetToken";
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1);

        when(userRepository.findById(1)).thenReturn(Optional.empty());

        boolean result = userService.storePasswordToken(1, token, expiryDate);

        assertFalse(result);
    }

    @Test
    public void activateStaffWithToken_shouldActivateStaffAccountSuccessfully() {
        String token = "validToken";
        String password = "newPassword123";

        user.setPasswordResetToken(token);
        user.setTokenExpiryDate(LocalDateTime.now().plusHours(1));

        when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");

        boolean result = userService.activateStaffWithToken(token, password);

        assertTrue(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void activateStaffWithToken_shouldReturnFalseWhenTokenIsExpired() {
        String token = "expiredToken";
        String password = "newPassword123";

        user.setPasswordResetToken(token);
        user.setTokenExpiryDate(LocalDateTime.now().minusHours(1));

        when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(user));

        boolean result = userService.activateStaffWithToken(token, password);

        assertFalse(result);
    }

    @Test
    void createUser_emailNotExists_createsAndReturnsUser() {
        when(userRepository.findByEmail(userDTO.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedTempPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1);
            return savedUser;
        });

        User createdUser = userService.createUser(userDTO);

        assertNotNull(createdUser);
        assertEquals(userDTO.getName(), createdUser.getName());
        assertEquals(userDTO.getEmail(), createdUser.getEmail());
        assertEquals("encodedTempPassword", createdUser.getPassword());
        assertEquals(userDTO.getRole(), createdUser.getRole());
        assertNotNull(createdUser.getCreatedAt());
        assertNotNull(createdUser.getUpdatedAt());

        verify(userRepository).findByEmail(userDTO.getEmail());
        verify(passwordEncoder).encode(anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_emailExists_throwsRuntimeException() {
        when(userRepository.findByEmail(userDTO.getEmail())).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.createUser(userDTO));

        assertEquals("Email already in use: " + userDTO.getEmail(), exception.getMessage());
        verify(userRepository).findByEmail(userDTO.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_userExists_deletesUser() {
        when(userRepository.existsById(user.getId())).thenReturn(true);
        doNothing().when(userRepository).deleteById(user.getId());

        userService.deleteUser(user.getId());

        verify(userRepository).existsById(user.getId());
        verify(userRepository).deleteById(user.getId());
    }

    @Test
    void deleteUser_userNotExists_throwsRuntimeException() {
        int nonExistentId = 99;
        when(userRepository.existsById(nonExistentId)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.deleteUser(nonExistentId));

        assertEquals("User with id " + nonExistentId + " not found", exception.getMessage());
        verify(userRepository).existsById(nonExistentId);
        verify(userRepository, never()).deleteById(anyInt());
    }

    @Test
    void updateUser_userExists_updatesAndReturnsDTO() throws InterruptedException {
        UserDTO updatedDetailsDTO = new UserDTO();
        updatedDetailsDTO.setName("UpdatedName");
        updatedDetailsDTO.setSurname("UpdatedSurname");
        updatedDetailsDTO.setRole(UserRoleEnum.COACH);
        updatedDetailsDTO.setMemberCount(5);
        // Email is not updated in this method as per current service logic
        LocalDateTime originalUpdatedAt = user.getUpdatedAt();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        sleep(1000);

        UserDTO resultDTO = userService.updateUser(user.getId(), updatedDetailsDTO);

        assertNotNull(resultDTO);
        assertEquals(updatedDetailsDTO.getName(), resultDTO.getName());
        assertEquals(updatedDetailsDTO.getSurname(), resultDTO.getSurname());
        assertEquals(user.getEmail(), resultDTO.getEmail());
        assertEquals(updatedDetailsDTO.getRole(), resultDTO.getRole());
        assertEquals(updatedDetailsDTO.getMemberCount(), resultDTO.getMemberCount());
        assertNotNull(resultDTO.getUpdatedAt());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("UpdatedName", savedUser.getName());
        assertTrue(savedUser.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void updateUser_userNotExists_throwsIllegalArgumentException() {
        int nonExistentId = 99;
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(nonExistentId, userDTO));

        assertEquals("User not found with id: " + nonExistentId, exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void listAllUsers_returnsListOfUserDTOs() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDTO> resultList = userService.listAllUsers();

        assertNotNull(resultList);
        assertEquals(1, resultList.size());
        UserDTO resultDTO = resultList.getFirst();
        assertEquals(user.getName(), resultDTO.getName());
        assertEquals(user.getEmail(), resultDTO.getEmail());
    }

    @Test
    void listAllUsers_noUsers_returnsEmptyList() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        List<UserDTO> resultList = userService.listAllUsers();
        assertNotNull(resultList);
        assertTrue(resultList.isEmpty());
    }

    @Test
    void getUserDetails_userExists_returnsUserDTO() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        UserDTO resultDTO = userService.getUserDetails(user.getId());
        assertNotNull(resultDTO);
        assertEquals(user.getName(), resultDTO.getName());
    }

    @Test
    void getUserDetails_userNotExists_throwsIllegalArgumentException() {
        int nonExistentId = 99;
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.getUserDetails(nonExistentId));
        assertEquals("User not found with id: " + nonExistentId, exception.getMessage());
    }

    @Test
    void storePasswordToken_userExists_storesTokenAndReturnsTrue() {
        String token = "testToken";
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        boolean result = userService.storePasswordToken(user.getId(), token, expiryDate);

        assertTrue(result);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(token, savedUser.getPasswordResetToken());
        assertEquals(expiryDate, savedUser.getTokenExpiryDate());
    }

    @Test
    void storePasswordToken_invalidUserId_returnsFalse() {
        boolean result = userService.storePasswordToken(0, "token", LocalDateTime.now());
        assertFalse(result);
        verify(userRepository, never()).findById(anyInt());
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void storePasswordToken_userNotExists_returnsFalse() {
        int nonExistentId = 99;
        String token = "testToken";
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1);
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        boolean result = userService.storePasswordToken(nonExistentId, token, expiryDate);

        assertFalse(result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void activateStaffWithToken_validTokenNotExpired_activatesAndReturnsTrue() {
        String token = "validToken";
        String newPassword = "newPassword123";
        user.setPasswordResetToken(token);
        user.setTokenExpiryDate(LocalDateTime.now().plusHours(1));

        when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        boolean result = userService.activateStaffWithToken(token, newPassword);

        assertTrue(result);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("encodedNewPassword", savedUser.getPassword());
        assertNull(savedUser.getPasswordResetToken());
        assertNull(savedUser.getTokenExpiryDate());
        assertNotNull(savedUser.getUpdatedAt());
    }

    @Test
    void activateStaffWithToken_nullOrBlankToken_returnsFalse() {
        assertFalse(userService.activateStaffWithToken(null, "password"));
        assertFalse(userService.activateStaffWithToken("  ", "password"));
        verify(userRepository, never()).findByPasswordResetToken(anyString());
    }

    @Test
    void activateStaffWithToken_invalidToken_returnsFalse() {
        String invalidToken = "invalidToken";
        when(userRepository.findByPasswordResetToken(invalidToken)).thenReturn(Optional.empty());

        boolean result = userService.activateStaffWithToken(invalidToken, "newPassword123");

        assertFalse(result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void activateStaffWithToken_expiredToken_returnsFalseAndClearsToken() {
        String token = "expiredToken";
        user.setPasswordResetToken(token);
        user.setTokenExpiryDate(LocalDateTime.now().minusHours(1)); // Expired

        when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(user));
        // Save will be called to clear the token
        when(userRepository.save(any(User.class))).thenReturn(user);


        boolean result = userService.activateStaffWithToken(token, "newPassword123");

        assertFalse(result);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture()); // Verify save is called to clear token
        User savedUser = userCaptor.getValue();
        assertNull(savedUser.getPasswordResetToken());
        assertNull(savedUser.getTokenExpiryDate());
        verify(passwordEncoder, never()).encode(anyString()); // Password should not be encoded/set
    }

    @Test
    void activateStaffWithToken_weakPassword_returnsFalse() {
        String token = "validToken";
        String weakPassword = "short"; // Less than 8 chars
        user.setPasswordResetToken(token);
        user.setTokenExpiryDate(LocalDateTime.now().plusHours(1));

        when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(user));

        boolean result = userService.activateStaffWithToken(token, weakPassword);

        assertFalse(result);
        verify(userRepository, never()).save(any(User.class)); // Or save might be called if token clearing happens before password check
        // Current logic: throws before save
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void convertToDto_nullUser_returnsNull() {
        assertNull(userService.convertToDto(null));
    }

    @Test
    void convertToDto_validUser_returnsCorrectDTO() {
        UserDTO dto = userService.convertToDto(user);
        assertNotNull(dto);
        assertEquals(user.getId(), dto.getId());
        assertEquals(user.getName(), dto.getName());
        assertEquals(user.getSurname(), dto.getSurname());
        assertEquals(user.getEmail(), dto.getEmail());
        assertEquals(user.getRole(), dto.getRole());
        assertEquals(user.getMemberCount(), dto.getMemberCount());
        assertEquals(user.getCreatedAt(), dto.getCreatedAt());
        assertEquals(user.getUpdatedAt(), dto.getUpdatedAt());
    }
}
