package com.permission.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.permission.common.R;
import com.permission.common.constant.SecurityConstants;
import com.permission.common.dto.LoginDTO;
import com.permission.common.dto.LoginUser;
import com.permission.common.dto.TokenVO;
import cn.hutool.core.util.StrUtil;
import com.permission.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public R<Map<String, Object>> captcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String captchaKey = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                SecurityConstants.CAPTCHA_PREFIX + captchaKey,
                captcha.getCode(),
                5,
                TimeUnit.MINUTES);

        Map<String, Object> result = new HashMap<>();
        result.put("captchaKey", captchaKey);
        result.put("captchaImage", captcha.getImageBase64Data());
        return R.ok(result);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<TokenVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return R.ok(userService.login(loginDTO));
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public R<TokenVO> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return R.ok(userService.refreshToken(refreshToken));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = SecurityConstants.TOKEN_HEADER, required = false) String header) {
        if (StrUtil.isNotBlank(header)) {
            String token = header.replace(SecurityConstants.TOKEN_PREFIX, "");
            userService.logout(token);
        }
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/user-info")
    public R<Map<String, Object>> userInfo(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return R.fail(401, "未登录");
        }
        Map<String, Object> info = new HashMap<>();
        info.put("userId", loginUser.getUserId());
        info.put("username", loginUser.getUsername());
        info.put("nickname", loginUser.getNickname());
        info.put("deptId", loginUser.getDeptId());
        info.put("deptName", loginUser.getDeptName());
        info.put("permissions", loginUser.getPermissions());
        info.put("roles", loginUser.getRoles());
        return R.ok(info);
    }
}

