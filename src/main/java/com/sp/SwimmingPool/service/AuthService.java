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
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final CookieService cookieService;

    public AuthResponse authenticate(LoginRequest loginRequest, HttpServletResponse response) {
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

            cookieService.createAuthCookie(jwt, response);

            return AuthResponse.builder()
                    .userType(user != null ? "STAFF" : "MEMBER")
                    .role(user != null ? user.getRole().name() : "MEMBER")
                    .email(loginRequest.getEmail())
                    .name(user != null ? user.getName() : member.getName())
                    .id(user != null ? user.getId() : member.getId())
                    .status(user != null ? null : member.getStatus())
                    .swimmingLevel(user != null ? null : member.getSwimmingLevel())
                    .canSwim(user != null ? null : member.isCanSwim())
                    .build();
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();
        }
    }

    public AuthResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        Member member = null;
        if (user == null) {
            member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException(email));
        }

        return AuthResponse.builder()
                .userType(user != null ? "STAFF" : "MEMBER")
                .role(user != null ? user.getRole().name() : "MEMBER")
                .email(email)
                .id(user != null ? user.getId() : member.getId())
                .name(user != null ? user.getName() : member.getName())
                .status(user != null ? null : member.getStatus())
                .swimmingLevel(user != null ? null : member.getSwimmingLevel())
                .canSwim(user != null ? null : member.isCanSwim())
                .build();
    }



}
