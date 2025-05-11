package com.sp.SwimmingPool;

import com.sp.SwimmingPool.dto.UserDTO;
import com.sp.SwimmingPool.model.entity.User;
import com.sp.SwimmingPool.model.enums.UserRoleEnum;
import com.sp.SwimmingPool.repos.UserRepository;
import com.sp.SwimmingPool.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(userDTO);
        });

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

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.deleteUser(1);
        });

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

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(1, updatedDTO);
        });

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
}
