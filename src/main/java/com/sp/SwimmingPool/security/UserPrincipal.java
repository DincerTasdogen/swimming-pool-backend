package com.sp.SwimmingPool.security;

import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails {
    private Integer id;
    private String email;
    @Getter
    private String name;
    private String password;
    private String role;
    private String userType;
    private Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal createFromUser(User user) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return UserPrincipal.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPassword())
                .role(user.getRole().name())
                .userType("STAFF")
                .authorities(authorities)
                .build();
    }

    public static UserPrincipal createFromMember(Member member) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_MEMBER")
        );

        return UserPrincipal.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .password(member.getPassword())
                .role("MEMBER")
                .userType("MEMBER")
                .authorities(authorities)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
