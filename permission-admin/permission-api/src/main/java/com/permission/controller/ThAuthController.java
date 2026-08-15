package com.permission.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.permission.common.R;
import com.permission.common.dto.LoginDTO;
import com.permission.common.entity.ThUser;
import com.permission.framework.security.JwtTokenProvider;
import com.permission.system.mapper.ThUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name = "树洞用户认证")
@RestController
@RequestMapping("/api/th/auth")
@RequiredArgsConstructor
public class ThAuthController {

    private final ThUserMapper thUserMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Operation(summary = "注册")
    @PostMapping("/register")
    public R<Map<String, String>> register(@RequestBody LoginDTO loginDTO) {
        if (StrUtil.isBlank(loginDTO.getUsername()) || StrUtil.isBlank(loginDTO.getPassword())) {
            return R.fail(400, "用户名和密码不能为空");
        }
        if (loginDTO.getUsername().length() < 3 || loginDTO.getUsername().length() > 20) {
            return R.fail(400, "用户名长度应为3-20位");
        }
        if (loginDTO.getPassword().length() < 6) {
            return R.fail(400, "密码至少6位");
        }

        // 检查用户名是否已存在
        Long count = thUserMapper.selectCount(new LambdaQueryWrapper<ThUser>()
                .eq(ThUser::getNickname, loginDTO.getUsername()));
        if (count > 0) {
            return R.fail(400, "用户名已存在");
        }

        // 创建树洞用户
        ThUser user = new ThUser();
        user.setNickname(loginDTO.getUsername());
        user.setPassword(passwordEncoder.encode(loginDTO.getPassword()));
        user.setGender(0);
        user.setStatus(1);
        thUserMapper.insert(user);

        // 生成 Token
        String token = jwtTokenProvider.createAccessToken(user.getId(), user.getNickname(), null);
        redisTemplate.opsForValue().set("token:" + token, user.getId(), 7200, TimeUnit.SECONDS);

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return R.ok(result);
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody LoginDTO loginDTO) {
        ThUser user = thUserMapper.selectOne(new LambdaQueryWrapper<ThUser>()
                .eq(ThUser::getNickname, loginDTO.getUsername()));

        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            return R.fail(401, "用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            return R.fail(403, "账号已被封禁");
        }

        // 生成 Token
        String token = jwtTokenProvider.createAccessToken(user.getId(), user.getNickname(), null);
        redisTemplate.opsForValue().set("token:" + token, user.getId(), 7200, TimeUnit.SECONDS);

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("nickname", user.getNickname());
        return R.ok(result);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/user-info")
    public R<Map<String, Object>> userInfo(HttpServletRequest request) {
        String token = extractToken(request);
        if (StrUtil.isBlank(token)) {
            return R.fail(401, "未登录");
        }

        Long userId = (Long) redisTemplate.opsForValue().get("token:" + token);
        if (userId == null) {
            return R.fail(401, "登录已过期");
        }

        ThUser user = thUserMapper.selectById(userId);
        if (user == null) {
            return R.fail(401, "用户不存在");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("nickname", user.getNickname());
        info.put("avatar", user.getAvatar());
        info.put("bio", user.getBio());
        info.put("postCount", user.getPostCount());
        info.put("commentCount", user.getCommentCount());
        return R.ok(info);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
