package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.repos.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

}
