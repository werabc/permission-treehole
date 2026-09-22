package com.permission.framework.security;

import com.permission.common.constant.SecurityConstants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    public JwtTokenProvider(@Value("${jwt.secret:}") String jwtSecret) {
        String secret = jwtSecret;
        if (secret == null || secret.isBlank()) {
            // Fallback to env variable
            secret = System.getenv("JWT_SECRET_KEY");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT secret key is not configured. Please set 'jwt.secret' property or 'JWT_SECRET_KEY' environment variable. "
                + "The key must be a Base64-encoded string of at least 32 bytes (256 bits).");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 256 bits (32 bytes)");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT secret key initialized successfully ({} bytes)", keyBytes.length);
    }

    public String createAccessToken(Long userId, String username, Map<String, Object> claims) {
        return createAccessToken(userId, username, claims, "admin");
    }

    /**
     * 创建 accessToken，带用户类型标识
     * @param userId 用户ID
     * @param username 用户名
     * @param claims 额外声明
     * @param userType 用户类型: "admin" 或 "treehole"
     */
    public String createAccessToken(Long userId, String username, Map<String, Object> claims, String userType) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + SecurityConstants.TOKEN_EXPIRE * 1000);

        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiration)
                .claim("username", username)
                .claim("userType", userType);

        if (claims != null) {
            claims.forEach(builder::claim);
        }

        return builder.signWith(secretKey).compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + SecurityConstants.REFRESH_TOKEN_EXPIRE * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiration)
                .claim("type", "refresh")
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        String subject = parseToken(token).getSubject();
        return Long.valueOf(subject);
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT token invalid: {}", e.getMessage());
            return false;
        }
    }

    public long getExpiration(String token) {
        return parseToken(token).getExpiration().getTime();
    }
}

