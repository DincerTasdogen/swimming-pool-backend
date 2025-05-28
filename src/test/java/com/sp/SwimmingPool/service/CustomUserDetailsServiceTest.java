package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.User;
import com.sp.SwimmingPool.model.enums.MemberGenderEnum;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.model.enums.SwimmingLevelEnum;
import com.sp.SwimmingPool.model.enums.UserRoleEnum;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.repos.UserRepository;
import com.sp.SwimmingPool.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;
    private Member testMember;
    private final String userEmail = "user@example.com";
    private final String memberEmail = "member@example.com";
    private final String unknownEmail = "unknown@example.com";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(userEmail)
                .name("Test")
                .surname("User")
                .password("password123")
                .role(UserRoleEnum.ADMIN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testMember = Member.builder()
                .email(memberEmail)
                .name("Test")
                .surname("Member")
                .password("password456")
                .identityNumber("12345678901")
                .gender(MemberGenderEnum.MALE)
                .birthDate(LocalDate.now().minusYears(20))
                .status(StatusEnum.ACTIVE)
                .swimmingLevel(SwimmingLevelEnum.BEGINNER)
                .canSwim(true)
                .registrationDate(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void loadUserByUsername_userFoundInUserRepository_returnsUserPrincipalForUser() {
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);

        assertNotNull(userDetails);
        assertTrue(userDetails instanceof UserPrincipal);
        UserPrincipal principal = (UserPrincipal) userDetails;

        assertEquals(testUser.getId(), principal.getId());
        assertEquals(testUser.getEmail(), principal.getEmail());
        assertEquals(testUser.getName(), principal.getName());
        assertEquals(testUser.getPassword(), principal.getPassword());
        assertEquals("STAFF", principal.getUserType());
        assertEquals(testUser.getRole().name(), principal.getRole());

        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_" + testUser.getRole().name())));

        verify(userRepository).findByEmail(userEmail);
        verify(memberRepository, never()).findByEmail(anyString());
    }

    @Test
    void loadUserByUsername_userNotFoundInUserRepository_memberFoundInMemberRepository_returnsUserPrincipalForMember() {
        when(userRepository.findByEmail(memberEmail)).thenReturn(Optional.empty());
        when(memberRepository.findByEmail(memberEmail)).thenReturn(Optional.of(testMember));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(memberEmail);

        assertNotNull(userDetails);
        assertTrue(userDetails instanceof UserPrincipal);
        UserPrincipal principal = (UserPrincipal) userDetails;

        assertEquals(testMember.getId(), principal.getId());
        assertEquals(testMember.getEmail(), principal.getEmail());
        assertEquals(testMember.getName(), principal.getName());
        assertEquals(testMember.getPassword(), principal.getPassword());
        assertEquals("MEMBER", principal.getUserType());
        assertEquals("MEMBER", principal.getRole());

        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_MEMBER")));

        verify(userRepository).findByEmail(memberEmail);
        verify(memberRepository).findByEmail(memberEmail);
    }

    @Test
    void loadUserByUsername_userNotFoundInEitherRepository_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());
        when(memberRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());

        Exception exception = assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername(unknownEmail);
        });

        assertEquals("User not found with email: " + unknownEmail, exception.getMessage());

        verify(userRepository).findByEmail(unknownEmail);
        verify(memberRepository).findByEmail(unknownEmail);
    }
}
