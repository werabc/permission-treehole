package com.permission.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.dto.LoginDTO;
import com.permission.common.entity.ThUser;
import com.permission.common.exception.BusinessException;
import com.permission.common.ResultCode;
import com.permission.framework.security.JwtTokenProvider;
import com.permission.system.mapper.ThUserMapper;
import com.permission.system.service.ThUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ThUserServiceImpl extends ServiceImpl<ThUserMapper, ThUser> implements ThUserService {

    private final ThUserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public ThUser register(LoginDTO loginDTO) {
        if (StrUtil.isBlank(loginDTO.getUsername()) || StrUtil.isBlank(loginDTO.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名和密码不能为空");
        }
        if (loginDTO.getUsername().length() < 3 || loginDTO.getUsername().length() > 20) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名长度应为3-20位");
        }
        if (loginDTO.getPassword().length() < 6) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码至少6位");
        }

        // 检查用户名唯一
        Long count = userMapper.selectCount(new LambdaQueryWrapper<ThUser>()
                .eq(ThUser::getUsername, loginDTO.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "用户名已存在");
        }

        ThUser user = new ThUser();
        user.setUsername(loginDTO.getUsername());
        user.setNickname(loginDTO.getUsername());
        user.setPassword(passwordEncoder.encode(loginDTO.getPassword()));
        user.setGender(0);
        user.setStatus(1);
        user.setAvatar("default.png");
        user.setPostCount(0);
        user.setCommentCount(0);
        user.setViolationCount(0);
        userMapper.insert(user);

        return user;
    }

    @Override
    public Map<String, String> login(LoginDTO loginDTO) {
        ThUser user = userMapper.selectOne(new LambdaQueryWrapper<ThUser>()
                .eq(ThUser::getUsername, loginDTO.getUsername()));

        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_ACCOUNT_DISABLED, "账号已被封禁");
        }

        // 生成 Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        String token = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), claims);

        // 缓存到 Redis
        redisTemplate.opsForValue().set("token:" + token, user.getId(), 7200, TimeUnit.SECONDS);

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("nickname", user.getNickname());
        return result;
    }

    @Override
    public ThUser getUserById(Long id) {
        return userMapper.selectById(id);
    }
}
