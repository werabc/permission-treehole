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
            // Dev fallback only - log a warning
            log.warn("WARNING: Using default JWT secret key. Set JWT_SECRET_KEY env var or jwt.secret property in production!");
            secret = "cGVybWlzc2lvbi1hZG1pbi1zZWNyZXQta2V5LTIwMjQtbXVzdC1iZS1sb25nLWVub3VnaC1mb3ItaHMyNTY=";
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 256 bits (32 bytes)");
        }
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
// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.framework.security.JwtTokenProvider
// 【模块】permission-framework（安全与基础设施模块）
//
// 【使用的注解/技术】
//   - @Slf4j — Lombok，自动生成 log 字段
//   - @Component — Spring，声明 Bean 并被 JwtAuthenticationFilter 注入
//   - @Value("${jwt.secret:}") — Spring，从配置文件注入密钥（空串默认值保证无配置启动不报错）
//   - io.jsonwebtoken.* — JJWT 库（api/io/parser），构造及解析 JWT
//
// 【关键依赖/注入】
//   - @Value("${jwt.secret:}") String jwtSecret — 构造参数接收 jwt.secret 配置（首选项）
//   - 启发式密钥加载：1) jwt.secret；2) JWT_SECRET_KEY 环境变量；3) 仅 dev 兜底默认密钥（启动时 warn）
//   - SecretKey — 由 Keys.hmacShaKeyFor 生成，算法 HS256
//   - 依赖 SecurityConstants — TOKEN_EXPIRE(7200s) / REFRESH_TOKEN_EXPIRE(604800s)
//
// 【关联文件】
//   - 被 JwtAuthenticationFilter 注入（validateToken / parseToken）
//   - 被 permission-service 登录/刷新接口注入（createAccessToken / createRefreshToken）
//   - 与 Blacklist（Redis）配合实现令牌吊销
//
// 【核心作用】JwtTokenProvider 是 Jwt 全生命周期管理器：签发 accessToken/refreshToken、解析 Claims、校验
//            签名与过期、获取用户 ID 与过期时间。密钥外部化并支持环境变量兜底。
//
// 【设计必要性】
//   - 集中管理令牌算法、密钥、有效期；避免各 Service 再次重复实现；
//   - 密钥三层加载策略让默认零配置即可开发运行，但生产必须使用环境变量或密钥管理服务；
//   - 启动时校验密钥长度（>=32 字节 / 256 位），避免弱密钥直接运行。
//
// 【注意事项/安全提示】
//   - ⚠️ 默认兜底密钥仅作开发用途；生产必须设置 JWT_SECRET_KEY 环境变量或 jwt.secret 配置，否则签名
//     可被伪造；
//   - 建议将密钥托管到 KMS/云密钥服务或 k8s secret，而非提交到仓库；
//   - accessToken 与 refreshToken 使用不同过期时间（2h / 7d），refreshToken 包含 claim type="refresh"
//     提示业务层区分用途；
//   - parseToken 解析失败统一在本类 validateToken 处理，INVALID token 不抛异常而是返回 false，便于上层
//     fail-closed 决策；
//   - 密钥轮换需要双密钥兼容期（旧 token 不能立即失效），或一次性要求全部重登录。
// ============================================================
