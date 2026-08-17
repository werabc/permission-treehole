package com.permission.framework.security;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JWT 认证工具类
 * <p>
 * 统一处理 Token 解析与用户身份提取，消除各 Controller 中重复的 JWT 解码逻辑。
 * 支持树洞端（弱校验）和管理端（强校验）两种场景。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationUtil {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 从 HttpServletRequest 中提取并解析 userId（严格模式：校验签名与过期时间）
     *
     * @param request HTTP 请求
     * @return userId，未登录或 token 无效时返回 null
     */
    public Long extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StrUtil.isBlank(header) || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return null;
        }
        return jwtTokenProvider.getUserId(token);
    }

    /**
     * 从 Token 字符串中提取 userId
     *
     * @param token JWT token（不含 Bearer 前缀）
     * @return userId，无效时返回 null
     */
    public Long extractUserId(String token) {
        if (StrUtil.isBlank(token) || !jwtTokenProvider.validateToken(token)) {
            return null;
        }
        return jwtTokenProvider.getUserId(token);
    }

    /**
     * 从 Authorization Header 原始值中提取 userId
     *
     * @param authorization Authorization 头的完整值
     * @return userId，无效时返回 null
     */
    public Long extractUserIdFromHeader(String authorization) {
        if (StrUtil.isBlank(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return extractUserId(authorization.substring(7));
    }
}
