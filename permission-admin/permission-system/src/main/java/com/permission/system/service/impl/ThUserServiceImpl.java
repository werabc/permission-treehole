package com.permission.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.constant.SecurityConstants;
import com.permission.common.dto.LoginDTO;
import com.permission.common.dto.ThProfileDTO;
import com.permission.common.entity.*;
import com.permission.common.exception.BusinessException;
import com.permission.common.ResultCode;
import com.permission.framework.security.JwtTokenProvider;
import com.permission.system.mapper.*;
import com.permission.system.service.OnlineUserService;
import com.permission.system.service.ThUserService;
import com.permission.system.support.ThUserGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
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
    private final OnlineUserService onlineUserService;
    private final ThUserGuard userGuard;

    // 最大分页大小限制
    private static final long MAX_PAGE_SIZE = 100;

    // ========== 密码策略（与 Admin 保持一致的高强度要求） ==========
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[~!@#$%^&*()_+\\-=\\[\\]{}|;:',.<>?/]).{8,}$");
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "123456", "password", "123456789", "12345678", "12345",
            "admin", "qwerty", "abc123", "letmein", "monkey", "111111",
            "123123", "dragon", "1234", "1234567890", "iloveyou");

    // ========== 树洞登录限流/锁定常量 ==========
    private static final int TH_MAX_LOGIN_FAIL = 5;          // 连续失败次数锁定
    private static final int TH_ACCOUNT_LOCK_MINUTES = 30;   // 锁定时间
    private static final int TH_RATE_LIMIT_MAX = 10;         // 每窗口最大登录尝试
    private static final int TH_RATE_LIMIT_WINDOW = 60;      // 窗口秒数
    private static final int TH_REGISTER_LIMIT_MAX = 3;      // 每窗口最大注册数
    private static final String TH_RATE_PREFIX = "rate_limit:th_login:";
    private static final String TH_REG_PREFIX = "rate_limit:th_register:";
    private static final String TH_FAIL_PREFIX = "th_login_fail:";

    @Override
    public ThUser register(LoginDTO loginDTO) {
        if (StrUtil.isBlank(loginDTO.getUsername()) || StrUtil.isBlank(loginDTO.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名和密码不能为空");
        }
        if (loginDTO.getUsername().length() < 3 || loginDTO.getUsername().length() > 20) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名长度应为3-20位");
        }
        // 强密码策略：8位以上，包含大小写字母、数字和特殊字符
        validatePassword(loginDTO.getPassword(), loginDTO.getUsername());

        // 注册速率限制（按 IP）
        String clientIp = getClientIp();
        String regKey = TH_REG_PREFIX + clientIp;
        Long regCount = redisTemplate.opsForValue().increment(regKey);
        if (regCount == 1) {
            redisTemplate.expire(regKey, TH_RATE_LIMIT_WINDOW, TimeUnit.SECONDS);
        }
        if (regCount != null && regCount > TH_REGISTER_LIMIT_MAX) {
            throw new BusinessException(ResultCode.RATE_LIMITED, "注册过于频繁，请稍后再试");
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
        // 不写 "default.png" 这种占位字符串：它不是可访问的 URL，
        // 前端 AppAvatar 会把它当图片地址去请求 → 必然破图（历史上首页出现 3 张坏图就是这个）。
        // 留空即可，AppAvatar 会自动退回"首字母"头像。
        user.setAvatar(null);
        user.setPostCount(0);
        user.setCommentCount(0);
        user.setViolationCount(0);
        userMapper.insert(user);

        log.info("Registered new user: {}", user.getUsername());
        return user;
    }

    @Override
    public Map<String, String> login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername();

        // 1. 登录速率限制（按 IP）
        String clientIp = getClientIp();
        String rateLimitKey = TH_RATE_PREFIX + clientIp;
        Long rateCount = redisTemplate.opsForValue().increment(rateLimitKey);
        if (rateCount == 1) {
            redisTemplate.expire(rateLimitKey, TH_RATE_LIMIT_WINDOW, TimeUnit.SECONDS);
        }
        if (rateCount != null && rateCount > TH_RATE_LIMIT_MAX) {
            throw new BusinessException(ResultCode.RATE_LIMITED, "登录过于频繁，请稍后再试");
        }

        // 2. 账号锁定检查
        String failKey = TH_FAIL_PREFIX + username;
        Object failCountObj = redisTemplate.opsForValue().get(failKey);
        int failCount = failCountObj instanceof Integer ? (Integer) failCountObj : 0;
        if (failCount >= TH_MAX_LOGIN_FAIL) {
            throw new BusinessException(ResultCode.ACCOUNT_TEMP_LOCKED, "账号已被临时锁定，请" + TH_ACCOUNT_LOCK_MINUTES + "分钟后再试");
        }

        ThUser user = userMapper.selectOne(new LambdaQueryWrapper<ThUser>()
                .eq(ThUser::getUsername, username));

        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            // 累加失败计数
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, TH_ACCOUNT_LOCK_MINUTES, TimeUnit.MINUTES);
            log.warn("Failed login attempt for user: {} (failCount={})", username, failCount + 1);
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_ACCOUNT_DISABLED, "账号已被封禁");
        }
        // 治理：封号（ban_until）校验，与 status=0 的永久封禁叠加生效
        userGuard.checkBanned(user);

        // 登录成功，清除失败计数
        redisTemplate.delete(failKey);

        // 记录登录 IP（用于风控与审计）
        try {
            user.setLastLoginIp(clientIp);
            userMapper.updateById(user);
        } catch (Exception e) {
            log.warn("更新登录IP失败 userId={}", user.getId());
        }

        // 生成 Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        String token = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), claims, "treehole");

        // 缓存到 Redis
        redisTemplate.opsForValue().set(SecurityConstants.TOKEN_CACHE_PREFIX + token, user.getId(), 7200, TimeUnit.SECONDS);

        // 记录在线状态
        onlineUserService.userOnline(user.getId(), user.getUsername(), user.getNickname(), "treehole-client");

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("nickname", user.getNickname());

        log.info("Treehole user logged in: {}", user.getUsername());
        return result;
    }

    @Override
    public ThUser getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public IPage<ThPost> getPosts(Long userId, long pageNum, long pageSize) {
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
            log.warn("Page size exceeds maximum, capped to {}", MAX_PAGE_SIZE);
        }
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
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
            log.warn("Page size exceeds maximum, capped to {}", MAX_PAGE_SIZE);
        }
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
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
            log.warn("Page size exceeds maximum, capped to {}", MAX_PAGE_SIZE);
        }
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
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
            log.warn("Page size exceeds maximum, capped to {}", MAX_PAGE_SIZE);
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.permission.common.entity.ThNotification> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        LambdaQueryWrapper<com.permission.common.entity.ThNotification> wrapper = new LambdaQueryWrapper<com.permission.common.entity.ThNotification>()
                .eq(com.permission.common.entity.ThNotification::getUserId, userId)
                .eq(unreadOnly != null && unreadOnly, com.permission.common.entity.ThNotification::getIsRead, 0)
                .orderByDesc(com.permission.common.entity.ThNotification::getCreateTime);
        IPage<ThNotification> result = (IPage<ThNotification>) thNotificationMapper.selectPage(page, wrapper);
        log.debug("Fetched {} notifications for user={}", result.getTotal(), userId);
        return result;
    }

    @Override
    public long getUnreadCount(Long userId) {
        long count = thNotificationMapper.selectCount(new LambdaQueryWrapper<com.permission.common.entity.ThNotification>()
                .eq(com.permission.common.entity.ThNotification::getUserId, userId)
                .eq(com.permission.common.entity.ThNotification::getIsRead, 0));
        log.debug("Unread count for user={}: {}", userId, count);
        return count;
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
        log.debug("Marked {} notifications as read for user={}", ids.size(), userId);
    }

    @Override
    public void updateProfile(Long userId, ThProfileDTO dto) {
        ThUser existing = userMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        // 使用 DTO 白名单 + HTML 转义，防止 Mass Assignment 和 Stored XSS
        if (dto.getNickname() != null) existing.setNickname(sanitizeHtml(dto.getNickname()));
        if (dto.getBio() != null) existing.setBio(sanitizeHtml(dto.getBio()));
        if (dto.getGender() != null) existing.setGender(dto.getGender());
        if (dto.getAvatar() != null) existing.setAvatar(dto.getAvatar());
        if (dto.getEmail() != null) existing.setEmail(dto.getEmail());
        userMapper.updateById(existing);
        log.info("Updated profile for user={}", existing.getUsername());
    }

    // ========== 私有辅助方法 ==========

    @Override
    public Map<String, Object> getPublicProfile(Long userId) {
        ThUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("nickname", user.getNickname());
        profile.put("avatar", user.getAvatar());
        profile.put("bio", user.getBio());
        profile.put("gender", user.getGender());
        profile.put("postCount", user.getPostCount());
        profile.put("commentCount", user.getCommentCount());
        profile.put("createTime", user.getCreateTime());
        // 不返回 username / email / lastLoginIp 等隐私字段
        return profile;
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        ThUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (StrUtil.isBlank(oldPassword) || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException(ResultCode.PASSWORD_WEAK, "新密码不能与原密码相同");
        }
        // 复用注册时的强密码策略
        validatePassword(newPassword, user.getUsername());

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        // 改密后让已签发的 Token 真正失效：JwtAuthenticationFilter 只认黑名单，
        // 因此必须把该用户所有已缓存 Token 加入黑名单，仅删缓存是无效的。
        // 注意：缓存值可能是 Long（树洞端）也可能是 LoginUser 对象（管理端），
        // 反序列化不可靠，因此统一从 key 里的 JWT 解析 userId 来判定归属。
        try {
            // 用 SCAN 游标遍历代替 KEYS：KEYS 是 O(N) 全库阻塞扫描，
            // Token 量大时会卡住整个 Redis（所有请求排队），改密接口就成了 DoS 入口
            java.util.Set<String> keys = new java.util.HashSet<>();
            try (var conn = redisTemplate.getConnectionFactory().getConnection();
                 var cursor = conn.scan(org.springframework.data.redis.core.ScanOptions
                         .scanOptions().match(SecurityConstants.TOKEN_CACHE_PREFIX + "*").count(500).build())) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next()));
                }
            }
            int revoked = 0;
            if (keys != null) {
                for (String key : keys) {
                    String token = key.substring(SecurityConstants.TOKEN_CACHE_PREFIX.length());
                    Long tokenUserId;
                    try {
                        tokenUserId = jwtTokenProvider.getUserId(token);
                    } catch (Exception ignore) {
                        continue; // 非法/损坏的缓存键直接跳过
                    }
                    if (!userId.equals(tokenUserId)) {
                        continue;
                    }
                    try {
                        long remaining = jwtTokenProvider.getExpiration(token) - System.currentTimeMillis();
                        if (remaining > 1000) {
                            redisTemplate.opsForValue().set(SecurityConstants.TOKEN_BLACKLIST_PREFIX + token,
                                    "1", remaining / 1000, TimeUnit.SECONDS);
                        }
                    } catch (Exception ignore) {
                        // Token 已过期等情况忽略
                    }
                    redisTemplate.delete(key);
                    revoked++;
                }
            }
            log.info("用户修改密码成功 userId={}，已吊销旧 Token {} 个", userId, revoked);
        } catch (Exception e) {
            log.error("吊销旧 Token 失败 userId={}", userId, e);
        }
    }

    /**
     * 密码强度校验：8-64位，包含大小写字母、数字、特殊字符，且不能与用户名相同
     */
    private void validatePassword(String password, String username) {
        if (StrUtil.isBlank(password) || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(ResultCode.PASSWORD_WEAK,
                    "密码至少8位，需包含大写字母、小写字母、数字和特殊字符");
        }
        if (password.length() > 64) {
            throw new BusinessException(ResultCode.PASSWORD_WEAK, "密码长度不能超过64位");
        }
        if (password.equalsIgnoreCase(username)) {
            throw new BusinessException(ResultCode.PASSWORD_WEAK, "密码不能与用户名相同");
        }
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            throw new BusinessException(ResultCode.PASSWORD_WEAK, "密码过于简单，请使用更复杂的密码");
        }
    }

    /**
     * HTML 转义：将 < > " ' & 转为实体，防止 Stored XSS
     */
    private String sanitizeHtml(String input) {
        if (input == null) return null;
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * 获取客户端 IP（优先从 X-Forwarded-For / X-Real-IP，需由可信反向代理覆盖）
     */
    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";
        jakarta.servlet.http.HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
