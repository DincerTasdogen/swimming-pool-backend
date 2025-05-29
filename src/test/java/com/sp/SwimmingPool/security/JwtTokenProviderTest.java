package com.sp.SwimmingPool.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts; // Ensure Jwts is imported
import io.jsonwebtoken.security.Keys; // Ensure Keys is imported
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private Authentication authentication;

    private final String testSecret = Base64.getEncoder().encodeToString("testSecretKeyForHS256AlgorithmOkayLength".getBytes());
    private final int testExpirationMs = 3600000; // 1 hour

    private Instant nowInstant;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", testExpirationMs);

        nowInstant = Instant.now();
    }

    private UserPrincipal createTestUserPrincipal(Integer id, String email, String role, String userType) {
        return UserPrincipal.builder()
                .id(id)
                .email(email)
                .name("Test User")
                .role(role)
                .userType(userType)
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)))
                .build();
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        UserPrincipal userPrincipal = createTestUserPrincipal(1, "test@example.com", "MEMBER", "MEMBER");
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        String token = jwtTokenProvider.generateToken(authentication);
        assertNotNull(token);

        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("test@example.com", jwtTokenProvider.getUsernameFromToken(token));

        Claims claims = Jwts.parser()
                .setSigningKey(jwtTokenProvider.getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(1, claims.get("userId", Integer.class));
        assertEquals("MEMBER", claims.get("role", String.class));
        assertEquals("MEMBER", claims.get("userType", String.class));
        // Allow some leeway for clock differences if not using an injected clock
        assertTrue(claims.getExpiration().after(Date.from(nowInstant.minusSeconds(10))));
        assertTrue(claims.getIssuedAt().before(Date.from(nowInstant.plusSeconds(10))));
    }

    @Test
    void validateToken_withExpiredToken_shouldReturnFalse() {
        // Create a token that is already expired
        String expiredToken = Jwts.builder()
                .subject("expiredUser")
                .issuedAt(new Date(nowInstant.toEpochMilli() - 2 * testExpirationMs))
                .expiration(new Date(nowInstant.toEpochMilli() - testExpirationMs))
                .signWith(jwtTokenProvider.getSigningKey())
                .compact();
        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }

    @Test
    void validateToken_withMalformedToken_shouldReturnFalse() {
        assertFalse(jwtTokenProvider.validateToken("this.is.not.a.jwt"));
    }

    @Test
    void validateToken_withUnsupportedToken_shouldReturnFalse() {
        String unsupportedToken = Jwts.builder().subject("user").compact(); // Unsigned
        assertFalse(jwtTokenProvider.validateToken(unsupportedToken));
    }

    @Test
    void validateToken_withInvalidSignature_shouldReturnFalse() {
        String tokenWithDifferentSignature = Jwts.builder()
                .subject("user")
                .issuedAt(Date.from(nowInstant))
                .expiration(new Date(nowInstant.toEpochMilli() + testExpirationMs))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(Base64.getEncoder().encodeToString("anotherSecretKeyWithSufficientLength".getBytes()))))
                .compact();
        assertFalse(jwtTokenProvider.validateToken(tokenWithDifferentSignature));
    }

    @Test
    void getUsernameFromToken_withExpiredToken_shouldThrowException() {
        String expiredToken = Jwts.builder()
                .subject("expiredUser")
                .issuedAt(new Date(nowInstant.toEpochMilli() - 2 * testExpirationMs))
                .expiration(new Date(nowInstant.toEpochMilli() - testExpirationMs))
                .signWith(jwtTokenProvider.getSigningKey())
                .compact();
        assertThrows(ExpiredJwtException.class, () -> jwtTokenProvider.getUsernameFromToken(expiredToken));
    }


    @Test
    void generateReservationQrToken_shouldCreateValidTokenWithCorrectClaims() {
        int reservationId = 101;
        int memberId = 202;
        // Adjust sessionStart so nbf is at or before nowInstant
        // nbf = sessionStart - 5 minutes. We want nbf <= nowInstant
        // So, sessionStart <= nowInstant + 5 minutes
        LocalDateTime sessionStart = LocalDateTime.ofInstant(nowInstant, ZoneId.systemDefault()).plusMinutes(5);
        LocalDateTime sessionEnd = sessionStart.plusHours(2); // exp = nowInstant + 5m + 2h

        String qrToken = jwtTokenProvider.generateReservationQrToken(reservationId, memberId, sessionStart, sessionEnd);
        assertNotNull(qrToken);

        // Now this token should be valid at nowInstant because nbf is nowInstant
        assertTrue(jwtTokenProvider.validateToken(qrToken), "Token should be valid as NBF is met");

        Claims claims = jwtTokenProvider.parseReservationQrToken(qrToken);
        assertEquals(reservationId, claims.get("reservationId", Integer.class));
        assertEquals(memberId, claims.get("memberId", Integer.class));
        assertEquals(sessionStart.toString(), claims.get("sessionStart", String.class));
        assertEquals(sessionEnd.toString(), claims.get("sessionEnd", String.class));

        Date expectedNbf = Date.from(sessionStart.minusMinutes(5).atZone(ZoneId.systemDefault()).toInstant());
        Date expectedExp = Date.from(sessionEnd.atZone(ZoneId.systemDefault()).toInstant());

        assertTrue(Math.abs(expectedNbf.getTime() - claims.getNotBefore().getTime()) < 1000, "NBF time mismatch");
        assertTrue(Math.abs(expectedExp.getTime() - claims.getExpiration().getTime()) < 1000, "Expiration time mismatch");
        // IssuedAt is based on 'new Date()' in provider, so compare with nowInstant
        assertTrue(Math.abs(Date.from(nowInstant).getTime() - claims.getIssuedAt().getTime()) < 2000, "IssuedAt time mismatch"); // Allow 2s for test execution variance
    }

    @Test
    void parseReservationQrToken_withValidToken_shouldReturnClaims() {
        int reservationId = 102;
        int memberId = 203;
        // Adjust sessionStart so nbf is in the past relative to nowInstant
        LocalDateTime sessionStart = LocalDateTime.ofInstant(nowInstant, ZoneId.systemDefault()).minusMinutes(10); // nbf = nowInstant - 15m
        LocalDateTime sessionEnd = sessionStart.plusHours(1); // exp = nowInstant -10m + 1h = nowInstant + 50m

        String token = jwtTokenProvider.generateReservationQrToken(reservationId, memberId, sessionStart, sessionEnd);

        Claims claims = jwtTokenProvider.parseReservationQrToken(token);
        assertNotNull(claims);
        assertEquals(reservationId, claims.get("reservationId"));
    }

    @Test
    void parseReservationQrToken_withExpiredToken_shouldThrowException() {
        // Ensure sessionEnd is well in the past from nowInstant
        LocalDateTime pastSessionStart = LocalDateTime.ofInstant(nowInstant, ZoneId.systemDefault()).minusHours(3);
        LocalDateTime pastSessionEnd = LocalDateTime.ofInstant(nowInstant, ZoneId.systemDefault()).minusHours(2); // Token is expired
        String expiredQrToken = jwtTokenProvider.generateReservationQrToken(1, 1, pastSessionStart, pastSessionEnd);

        assertThrows(ExpiredJwtException.class, () -> jwtTokenProvider.parseReservationQrToken(expiredQrToken));
    }

    @Test
    void parseReservationQrToken_withTokenNotYetValid_shouldThrowException() {
        // nbf will be futureSessionStart - 5 minutes
        LocalDateTime futureSessionStart = LocalDateTime.ofInstant(nowInstant, ZoneId.systemDefault()).plusHours(2); // nbf = now + 2h - 5m
        LocalDateTime futureSessionEnd = futureSessionStart.plusHours(1);
        String nbfQrToken = jwtTokenProvider.generateReservationQrToken(1,1,futureSessionStart, futureSessionEnd);

        assertThrows(io.jsonwebtoken.PrematureJwtException.class, () -> jwtTokenProvider.parseReservationQrToken(nbfQrToken));
    }
}