package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.User;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.repos.UserRepository;
import com.sp.SwimmingPool.security.UserPrincipal;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            return UserPrincipal.createFromUser(user);
        }

        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return UserPrincipal.createFromMember(member);

    }
}
