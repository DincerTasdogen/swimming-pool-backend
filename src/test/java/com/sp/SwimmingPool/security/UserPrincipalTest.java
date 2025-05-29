package com.sp.SwimmingPool.security;

import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.User;
import com.sp.SwimmingPool.model.enums.UserRoleEnum;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserPrincipalTest {

    @Test
    void createFromUser_shouldMapCorrectly() {
        User user = new User();
        user.setId(1);
        user.setName("Staff Name");
        user.setEmail("staff@example.com");
        user.setPassword("hashedpassword");
        user.setRole(UserRoleEnum.ADMIN);

        UserPrincipal principal = UserPrincipal.createFromUser(user);

        assertEquals(1, principal.getId());
        assertEquals("Staff Name", principal.getName()); // getName() in UserPrincipal returns String.valueOf(name)
        assertEquals("staff@example.com", principal.getEmail());
        assertEquals("staff@example.com", principal.getUsername());
        assertEquals("hashedpassword", principal.getPassword());
        assertEquals("ADMIN", principal.getRole());
        assertEquals("STAFF", principal.getUserType());

        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertNull(principal.getAttributes()); // Attributes not set in this factory
    }

    @Test
    void createFromMember_shouldMapCorrectly() {
        Member member = new Member();
        member.setId(10);
        member.setName("Member Name");
        member.setEmail("member@example.com");
        member.setPassword("memberpassword");

        UserPrincipal principal = UserPrincipal.createFromMember(member);

        assertEquals(10, principal.getId());
        assertEquals("Member Name", principal.getName());
        assertEquals("member@example.com", principal.getEmail());
        assertEquals("member@example.com", principal.getUsername());
        assertEquals("memberpassword", principal.getPassword());
        assertEquals("MEMBER", principal.getRole());
        assertEquals("MEMBER", principal.getUserType());

        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_MEMBER")));
        assertNull(principal.getAttributes());
    }

    @Test
    void create_fromMemberAndAttributes_shouldMapCorrectly() {
        Member member = new Member();
        member.setId(11);
        member.setName("OAuth Member");
        member.setEmail("oauth@example.com");
        member.setPassword(null); // OAuth users might not have a direct password initially

        Map<String, Object> oauthAttributes = new HashMap<>();
        oauthAttributes.put("provider_id", "12345");
        oauthAttributes.put("custom_claim", "custom_value");

        UserPrincipal principal = UserPrincipal.create(member, oauthAttributes);

        assertEquals(11, principal.getId());
        assertEquals("OAuth Member", principal.getName());
        assertEquals("oauth@example.com", principal.getEmail());
        assertNull(principal.getPassword());
        assertEquals("MEMBER", principal.getRole());
        assertEquals("MEMBER", principal.getUserType());

        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_MEMBER")));

        assertNotNull(principal.getAttributes());
        assertEquals("12345", principal.getAttributes().get("provider_id"));
        assertEquals("custom_value", principal.getAttributes().get("custom_claim"));
    }

    @Test
    void standardUserDetailsMethods_shouldReturnExpectedValues() {
        UserPrincipal principal = UserPrincipal.builder()
                .email("test@example.com")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_TEST")))
                .name("Test Name") // Ensure name is set for getName()
                .build();

        assertEquals("test@example.com", principal.getUsername());
        assertEquals("password", principal.getPassword());
        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isCredentialsNonExpired());
        assertTrue(principal.isEnabled());
        assertEquals("Test Name", principal.getName()); // Test the OAuth2User getName()
    }
}