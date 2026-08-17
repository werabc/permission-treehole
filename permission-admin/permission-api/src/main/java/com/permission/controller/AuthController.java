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

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.controller.AuthController
// 【模块】permission-api
//
// 【使用的注解/技术】
//   - @Tag(name = "认证管理") — Swagger/OpenAPI，API 分组标签
//   - @RestController — Spring MVC，声明 REST 控制器（自动 JSON 序列化响应体）
//   - @RequestMapping("/api/auth") — Spring MVC，基础路由前缀
//   - @RequiredArgsConstructor — Lombok，生成必需参数构造器（构造器注入）
//   - @Operation — Swagger/OpenAPI，描述每个接口的用途，生成 API 文档
//   - @GetMapping / @PostMapping — Spring MVC，声明 GET/POST 端点
//   - @RequestBody — Spring MVC，绑定请求 JSON 体到方法参数
//   - @RequestHeader — Spring MVC，绑定请求头到方法参数
//   - @AuthenticationPrincipal — Spring Security，注入当前认证主体的 LoginUser 对象
//   - @Valid — Jakarta Bean Validation，触发请求参数校验
//   - RedirectAttributes（未使用）/ LineCaptcha — Hutool，生成线段验证码
//   - RedisTemplate — Spring Data Redis，缓存/读取验证码
//   - StrUtil — Hutool，字符串非空判断
//   - R / LoginDTO / LoginUser / TokenVO — 公共 DTO 与统一响应封装
//   - SecurityConstants 安全常量 — 验证码前缀、Token 前缀/头名
//
// 【关键依赖】
//   - 依赖 SysUserService → 执行登录、刷新 Token、登出业务
//   - 依赖 RedisTemplate → 验证码的 5 分钟缓存存储与校验
//   - 依赖 LineCaptcha（Hutool） → 图片验证码生成
//   - 依赖 SecurityConstants → 读取 Token 头名、前缀与验证码 Key 前缀
//
// 【关联文件】
//   - 被 JwtAuthenticationFilter 关联（logout 通过 header 传 Token）
//   - 被 OperationLogAspect 部分切面拦截（@OperationLog 注解的方法会记录日志）
//   - 依赖 SecurityConstants 常量
//   - userInfo 方法返回当前用户的基本信息/权限/角色，供前端展示用户状态与菜单权限
//
// 【核心作用】
//   认证管理控制器：提供获取图片验证码、用户登录、刷新 Token、退出登录、获取当前用户信息
//   五个核心接口，是系统鉴权与用户会话管理的入口。
//
// 【设计必要性】
//   认证流程为无状态 RESTful 设计，集中在一个 Controller 统一管理会话生命周期；验证码通过
//   独立接口返回 Key + 图片 Base64，与登录接口解耦，便于前后端分离架构。
//
// 【注意事项/安全提示】
//   - logout 接口的 header 参数用 required=false 修复：允许未登录/无 Token 的请求也能
//     正常登出，避免 Spring MVC 因缺少必要请求头直接返回 400 报错导致前端强制登出失败
//   - captcha 接口使用 Hutool 的 getImageBase64Data() 返回完整数据 URI，无需额外加前缀
//     （已修复的双前缀 bug）
//   - 验证码 5 分钟过期，每次校验后从 Redis 删除，防止重复使用
//   - 登录请求通过 @Valid 触发 Bean Validation，配合 LoginDTO 上的约束注解保障入参合法
//   - userInfo 不返回密码、仅返回基础信息，安全可控
// ============================================================
