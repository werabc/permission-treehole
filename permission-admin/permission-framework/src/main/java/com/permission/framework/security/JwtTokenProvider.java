package com.permission.framework.security;

import com.permission.common.constant.SecurityConstants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    public JwtTokenProvider() {
        byte[] keyBytes = Decoders.BASE64.decode("ZGV2LWRlbW8tand0LXNlY3JldC1wbGVhc2UtcmVwbGFjZS1pbi1wcm9kdWN0aW9uLTMyYg==");
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long userId, String username, Map<String, Object> claims) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + SecurityConstants.TOKEN_EXPIRE * 1000);

        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiration)
                .claim("username", username);

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
            throw e;
        } catch (JwtException e) {
            log.debug("JWT token invalid: {}", e.getMessage());
            return false;
        }
    }

    public long getExpiration(String token) {
        return parseToken(token).getExpiration().getTime();
    }
}
