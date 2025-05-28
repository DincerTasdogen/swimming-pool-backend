package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.AuthResponse;
import com.sp.SwimmingPool.dto.LoginRequest;
import com.sp.SwimmingPool.exception.InvalidCredentialsException;
import com.sp.SwimmingPool.exception.UserNotFoundException;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.User;
import com.sp.SwimmingPool.model.enums.MemberGenderEnum;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.model.enums.SwimmingLevelEnum;
import com.sp.SwimmingPool.model.enums.UserRoleEnum;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.repos.UserRepository;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CookieService cookieService;
    @Mock
    private HttpServletResponse httpServletResponse; // Mock for the response object
    @Mock
    private Authentication successfulAuthentication; // Mock for the Authentication object returned by AuthenticationManager
    @Mock
    private SecurityContext securityContext; // Mock for SecurityContext

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Member testMember;
    private LoginRequest userLoginRequest;
    private LoginRequest memberLoginRequest;

    private MockedStatic<SecurityContextHolder> securityContextHolderMockedStatic;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("staff@example.com")
                .name("StaffName")
                .surname("StaffSurname") // Added surname
                .password("encodedPassword")
                .role(UserRoleEnum.COACH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userLoginRequest = new LoginRequest(); // Initialize using setters or ensure constructor if available
        userLoginRequest.setEmail("staff@example.com");
        userLoginRequest.setPassword("password123");


        testMember = Member.builder()
                .email("member@example.com")
                .name("MemberName")
                .surname("MemberSurname") // Added surname
                .password("encodedPassword2")
                .identityNumber("12345678901")
                .gender(MemberGenderEnum.FEMALE)
                .birthDate(LocalDate.now().minusYears(25))
                .status(StatusEnum.ACTIVE)
                .swimmingLevel(SwimmingLevelEnum.INTERMEDIATE)
                .canSwim(true)
                .registrationDate(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        memberLoginRequest = new LoginRequest();
        memberLoginRequest.setEmail("member@example.com");
        memberLoginRequest.setPassword("password456");

        // Mock SecurityContextHolder to return our mocked SecurityContext
        securityContextHolderMockedStatic = Mockito.mockStatic(SecurityContextHolder.class);
        securityContextHolderMockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }

    @AfterEach
    void tearDown() {
        // Release the static mock
        securityContextHolderMockedStatic.close();
    }


    @Test
    void authenticate_staffUserSuccess_returnsAuthResponseAndSetsCookie() {
        // Arrange
        when(userRepository.findByEmail(userLoginRequest.getEmail())).thenReturn(Optional.of(testUser));
        // memberRepository.findByEmail should not be called for a staff user if found in userRepository
        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(successfulAuthentication); // Return the mocked Authentication
        when(jwtTokenProvider.generateToken(successfulAuthentication)).thenReturn("test-jwt-token");
        // No need to mock successfulAuthentication.getPrincipal() for AuthResponse building,
        // as AuthService re-fetches user/member.

        // Act
        AuthResponse response = authService.authenticate(userLoginRequest, httpServletResponse);

        // Assert
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getId());
        assertEquals("STAFF", response.getUserType());
        assertEquals(testUser.getRole().name(), response.getRole());
        assertEquals(userLoginRequest.getEmail(), response.getEmail()); // Email from request
        assertEquals(testUser.getName(), response.getName());
        assertNull(response.getStatus()); // Status is for members
        assertNull(response.getCanSwim()); // canSwim is for members
        assertNull(response.getSwimmingLevel()); // swimmingLevel is for members

        verify(securityContext).setAuthentication(successfulAuthentication); // Verify SecurityContext is updated
        verify(cookieService).createAuthCookie("test-jwt-token", httpServletResponse);
        verify(memberRepository, never()).findByEmail(anyString()); // Ensure member repo not called for staff
    }

    @Test
    void authenticate_memberUserSuccess_returnsAuthResponseAndSetsCookie() {
        // Arrange
        when(userRepository.findByEmail(memberLoginRequest.getEmail())).thenReturn(Optional.empty());
        when(memberRepository.findByEmail(memberLoginRequest.getEmail())).thenReturn(Optional.of(testMember));
        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(successfulAuthentication);
        when(jwtTokenProvider.generateToken(successfulAuthentication)).thenReturn("test-jwt-token-member");

        // Act
        AuthResponse response = authService.authenticate(memberLoginRequest, httpServletResponse);

        // Assert
        assertNotNull(response);
        assertEquals(testMember.getId(), response.getId());
        assertEquals("MEMBER", response.getUserType());
        assertEquals("MEMBER", response.getRole()); // Role is hardcoded to MEMBER for members in UserPrincipal
        assertEquals(memberLoginRequest.getEmail(), response.getEmail()); // Email from request
        assertEquals(testMember.getName(), response.getName());
        assertEquals(testMember.getStatus(), response.getStatus());
        assertEquals(testMember.isCanSwim(), response.getCanSwim());
        assertEquals(testMember.getSwimmingLevel(), response.getSwimmingLevel());

        verify(securityContext).setAuthentication(successfulAuthentication);
        verify(cookieService).createAuthCookie("test-jwt-token-member", httpServletResponse);
    }

    @Test
    void authenticate_userNotFound_throwsUserNotFoundException() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("unknown@example.com");
        loginRequest.setPassword("password");

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());
        when(memberRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            authService.authenticate(loginRequest, httpServletResponse);
        });
        assertEquals("AUTH_001", exception.getMessage());

        verify(authenticationManager, never()).authenticate(any());
        verify(jwtTokenProvider, never()).generateToken(any());
        verify(cookieService, never()).createAuthCookie(anyString(), any());
        verify(securityContext, never()).setAuthentication(any());
    }

    @Test
    void authenticate_badCredentials_throwsInvalidCredentialsException() {
        // Arrange
        // User is found, but authentication manager throws BadCredentialsException
        when(userRepository.findByEmail(userLoginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials from manager"));

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class, () -> {
            authService.authenticate(userLoginRequest, httpServletResponse);
        });

        assertEquals("AUTH_002", exception.getMessage());


        verify(jwtTokenProvider, never()).generateToken(any());
        verify(cookieService, never()).createAuthCookie(anyString(), any());
        verify(securityContext, never()).setAuthentication(any()); // Should not be set if auth fails
    }
}