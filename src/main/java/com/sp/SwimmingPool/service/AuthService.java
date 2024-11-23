package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.AuthResponse;
import com.sp.SwimmingPool.dto.LoginRequest;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.User;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.repos.UserRepository;
import com.sp.SwimmingPool.security.JwtTokenProvider;
import com.sp.SwimmingPool.exception.InvalidCredentialsException;
import com.sp.SwimmingPool.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;

    public AuthResponse authenticate(LoginRequest loginRequest) {
        try {
            User user = userRepository.findByEmail(loginRequest.getEmail()).orElse(null);
            Member member = null;

            if (user == null) {
                member = memberRepository.findByEmail(loginRequest.getEmail())
                        .orElseThrow(() -> new UserNotFoundException(loginRequest.getEmail()));
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenProvider.generateToken(authentication);

            return AuthResponse.builder()
                    .token(jwt)
                    .userType(user != null ? "STAFF" : "MEMBER")
                    .role(user != null ? user.getRole().name() : "MEMBER")
                    .email(loginRequest.getEmail())
                    .name(user != null ? user.getName() : member.getName())
                    .build();
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();
        }

    }

}
