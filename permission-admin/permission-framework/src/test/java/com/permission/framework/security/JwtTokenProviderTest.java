package com.permission.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT Token Provider 单元测试
 * Alibaba-Java: 单元测试 AIR 原则 — Automatic, Independent, Repeatable
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // 使用测试密钥 (32+ bytes for HS256)
        String testSecret = Base64.getEncoder().encodeToString(
                "test-secret-key-for-unit-testing-must-be-long-enough".getBytes());
        jwtTokenProvider = new JwtTokenProvider(testSecret);
    }

    @Test
    void createAccessToken_ShouldReturnValidToken() {
        String token = jwtTokenProvider.createAccessToken(1L, "testuser", null);
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void createAccessToken_WithClaims_ShouldContainClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("permissions", java.util.Set.of("read", "write"));
        String token = jwtTokenProvider.createAccessToken(1L, "testuser", claims);
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void createRefreshToken_ShouldReturnValidToken() {
        String token = jwtTokenProvider.createRefreshToken(1L);
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void getUserId_ShouldReturnCorrectUserId() {
        String token = jwtTokenProvider.createAccessToken(42L, "testuser", null);
        Long userId = jwtTokenProvider.getUserId(token);
        assertEquals(42L, userId);
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    void parseToken_ShouldReturnClaims() {
        String token = jwtTokenProvider.createAccessToken(1L, "testuser", null);
        Claims claims = jwtTokenProvider.parseToken(token);
        assertNotNull(claims);
        assertEquals("1", claims.getSubject());
        assertEquals("testuser", claims.get("username"));
    }

    // 测试密钥不足32字节时应抛出异常
    @Test
    void constructor_WithShortKey_ShouldThrowException() {
        String shortSecret = Base64.getEncoder().encodeToString("short".getBytes());
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(shortSecret));
    }

    // 测试空密钥时应抛出异常
    @Test
    void constructor_WithEmptyKey_ShouldThrowException() {
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(""));
    }

    // 测试null密钥时应抛出异常
    @Test
    void constructor_WithNullKey_ShouldThrowException() {
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(null));
    }
}
