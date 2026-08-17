package com.permission.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.dto.LoginDTO;
import com.permission.common.entity.*;
import com.permission.common.exception.BusinessException;
import com.permission.common.ResultCode;
import com.permission.framework.security.JwtTokenProvider;
import com.permission.system.mapper.*;
import com.permission.system.service.ThUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ThUserServiceImpl extends ServiceImpl<ThUserMapper, ThUser> implements ThUserService {

    private final ThUserMapper userMapper;
    private final ThPostMapper thPostMapper;
    private final ThCommentMapper thCommentMapper;
    private final ThNotificationMapper thNotificationMapper;
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

    @Override
    public IPage<ThPost> getPosts(Long userId, long pageNum, long pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.permission.common.entity.ThPost> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        LambdaQueryWrapper<com.permission.common.entity.ThPost> wrapper = new LambdaQueryWrapper<com.permission.common.entity.ThPost>()
                .eq(com.permission.common.entity.ThPost::getUserId, userId)
                .eq(com.permission.common.entity.ThPost::getDeleted, 0)
                .orderByDesc(com.permission.common.entity.ThPost::getCreateTime);
        return (IPage<ThPost>) thPostMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<ThComment> getMyComments(Long userId, long pageNum, long pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.permission.common.entity.ThComment> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        LambdaQueryWrapper<com.permission.common.entity.ThComment> wrapper = new LambdaQueryWrapper<com.permission.common.entity.ThComment>()
                .eq(com.permission.common.entity.ThComment::getUserId, userId)
                .eq(com.permission.common.entity.ThComment::getDeleted, 0)
                .orderByDesc(com.permission.common.entity.ThComment::getCreateTime);
        return (IPage<ThComment>) thCommentMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<ThComment> getReceivedComments(Long userId, long pageNum, long pageSize) {
        // 获取用户的所有帖子ID
        List<Long> postIds = thPostMapper.selectList(
                new LambdaQueryWrapper<com.permission.common.entity.ThPost>()
                        .eq(com.permission.common.entity.ThPost::getUserId, userId)
                        .eq(com.permission.common.entity.ThPost::getDeleted, 0)
        ).stream().map(com.permission.common.entity.ThPost::getId).collect(java.util.stream.Collectors.toList());

        if (postIds.isEmpty()) {
            return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        }

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.permission.common.entity.ThComment> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        LambdaQueryWrapper<com.permission.common.entity.ThComment> wrapper = new LambdaQueryWrapper<com.permission.common.entity.ThComment>()
                .in(com.permission.common.entity.ThComment::getPostId, postIds)
                .eq(com.permission.common.entity.ThComment::getDeleted, 0)
                .ne(com.permission.common.entity.ThComment::getUserId, userId)
                .orderByDesc(com.permission.common.entity.ThComment::getCreateTime);
        return (IPage<ThComment>) thCommentMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<ThNotification> getNotifications(Long userId, long pageNum, long pageSize, Boolean unreadOnly) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.permission.common.entity.ThNotification> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        LambdaQueryWrapper<com.permission.common.entity.ThNotification> wrapper = new LambdaQueryWrapper<com.permission.common.entity.ThNotification>()
                .eq(com.permission.common.entity.ThNotification::getUserId, userId)

                .eq(unreadOnly != null && unreadOnly, com.permission.common.entity.ThNotification::getIsRead, 0)
                .orderByDesc(com.permission.common.entity.ThNotification::getCreateTime);
        return (IPage<ThNotification>) thNotificationMapper.selectPage(page, wrapper);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return thNotificationMapper.selectCount(new LambdaQueryWrapper<com.permission.common.entity.ThNotification>()
                .eq(com.permission.common.entity.ThNotification::getUserId, userId)

                .eq(com.permission.common.entity.ThNotification::getIsRead, 0));
    }

    @Override
    public void markNotificationsRead(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            com.permission.common.entity.ThNotification notification = thNotificationMapper.selectById(id);
            if (notification != null && notification.getUserId().equals(userId)) {
                notification.setIsRead(1);
                thNotificationMapper.updateById(notification);
            }
        }
    }

    @Override
    public void updateProfile(ThUser user) {
        ThUser existing = userMapper.selectById(user.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (user.getNickname() != null) existing.setNickname(user.getNickname());
        if (user.getBio() != null) existing.setBio(user.getBio());
        if (user.getGender() != null) existing.setGender(user.getGender());
        if (user.getAvatar() != null) existing.setAvatar(user.getAvatar());
        userMapper.updateById(existing);
    }
}
