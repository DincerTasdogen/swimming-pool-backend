package com.sp.SwimmingPool.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;


@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    protected SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("userId", userPrincipal.getId())
                .claim("role", userPrincipal.getRole())
                .claim("userType", userPrincipal.getUserType())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String generateReservationQrToken(int reservationId, int memberId, LocalDateTime sessionStart, LocalDateTime sessionEnd) {
        Date now = new Date();
        Date notBefore = Date.from(sessionStart.minusMinutes(5).atZone(ZoneId.systemDefault()).toInstant());
        Date expiry = Date.from(sessionEnd.atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .claim("reservationId", reservationId)
                .claim("memberId", memberId)
                .claim("sessionStart", sessionStart.toString())
                .claim("sessionEnd", sessionEnd.toString())
                .notBefore(notBefore)
                .expiration(expiry)
                .issuedAt(now)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseReservationQrToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
