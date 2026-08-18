package com.permission.controller;

import cn.hutool.core.util.StrUtil;
import com.permission.common.R;
import com.permission.common.constant.SecurityConstants;
import com.permission.common.dto.LoginDTO;
import com.permission.common.entity.ThUser;
import com.permission.framework.security.JwtAuthenticationUtil;
import com.permission.framework.security.JwtTokenProvider;
import com.permission.system.service.ThUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "树洞认证")
@RestController
@RequestMapping("/api/th/auth")
@RequiredArgsConstructor
public class ThAuthController {

    private final ThUserService userService;
    private final JwtAuthenticationUtil jwtUtil;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Operation(summary = "注册")
    @PostMapping("/register")
    public R<Map<String, String>> register(@RequestBody LoginDTO loginDTO) {
        ThUser user = userService.register(loginDTO);
        Map<String, String> result = new HashMap<>();
        result.put("id", String.valueOf(user.getId()));
        return R.ok(result);
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody LoginDTO loginDTO) {
        return R.ok(userService.login(loginDTO));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = SecurityConstants.TOKEN_HEADER, required = false) String header) {
        if (StrUtil.isNotBlank(header) && header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            String token = header.substring(SecurityConstants.TOKEN_PREFIX.length());
            try {
                // 从 Redis 中删除 token 缓存
                redisTemplate.delete(SecurityConstants.TOKEN_CACHE_PREFIX + token);
                // 将 token 加入黑名单，使其失效
                long remaining = jwtTokenProvider.getExpiration(token) - System.currentTimeMillis();
                if (remaining > 1000) {
                    long remainingSeconds = Math.max(remaining / 1000, 1);
                    redisTemplate.opsForValue().set(
                            SecurityConstants.TOKEN_BLACKLIST_PREFIX + token,
                            "1",
                            remainingSeconds,
                            TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                // 解析失败时忽略，仍返回成功
            }
        }
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/user-info")
    public R<Map<String, Object>> userInfo(HttpServletRequest request) {
        Long userId = jwtUtil.extractUserId(request);
        if (userId == null) return R.fail(401, "未登录");

        ThUser user = userService.getUserById(userId);
        if (user == null) return R.fail(401, "用户不存在");

        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("avatar", user.getAvatar());
        info.put("bio", user.getBio());
        info.put("postCount", user.getPostCount());
        info.put("commentCount", user.getCommentCount());
        return R.ok(info);
    }
}
