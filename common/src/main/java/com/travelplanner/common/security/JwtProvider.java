package com.travelplanner.common.security;

import com.travelplanner.common.enums.SubscriptionTier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final int accessTokenExpiry;
    private final int refreshTokenExpiry;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity:1800}") int accessTokenExpiry,
            @Value("${jwt.refresh-token-validity:86400}") int refreshTokenExpiry) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    public String generateAccessToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (long) accessTokenExpiry * 1000);

        return Jwts.builder()
                .subject(principal.getUserId())
                .claim("email", principal.getEmail())
                .claim("tier", principal.getTier().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (long) refreshTokenExpiry * 1000);

        return Jwts.builder()
                .subject(userId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public JwtToken generateToken(UserPrincipal principal) {
        return new JwtToken(
                generateAccessToken(principal),
                generateRefreshToken(principal.getUserId()),
                accessTokenExpiry
        );
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UserPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userId = claims.getSubject();
        String email = claims.get("email", String.class);
        String tierStr = claims.get("tier", String.class);
        SubscriptionTier tier = tierStr != null
                ? SubscriptionTier.valueOf(tierStr)
                : SubscriptionTier.FREE;

        return new UserPrincipal(userId, email, tier);
    }

    public void invalidateToken(String userId) {
        // 토큰 블랙리스트 처리는 각 서비스의 Redis에서 수행
        // 이 메서드는 인터페이스 계약용으로 제공됨
    }
}
