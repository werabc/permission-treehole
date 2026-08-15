package com.permission.system.service;

import com.permission.common.entity.SysUser;
import com.permission.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnlineUserService {

    private static final String ONLINE_KEY_PREFIX = "online:user:";
    private static final String ONLINE_INDEX_KEY = "online:users:index";

    private final RedisTemplate<String, Object> redisTemplate;
    private final SysUserMapper userMapper;

    /**
     * 记录用户上线
     */
    public void userOnline(Long userId, String username, String nickname, String ip) {
        String key = ONLINE_KEY_PREFIX + userId;
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("userId", userId.toString());
        info.put("username", username);
        info.put("nickname", nickname != null ? nickname : username);
        info.put("ip", ip);
        info.put("loginTime", LocalDateTime.now().toString());

        redisTemplate.opsForHash().putAll(key, info);
        redisTemplate.expire(key, Duration.ofHours(2));
        redisTemplate.opsForSet().add(ONLINE_INDEX_KEY, userId.toString());
    }

    /**
     * 获取所有在线用户
     */
    public List<Map<String, Object>> getOnlineUsers() {
        Set<Object> userIds = redisTemplate.opsForSet().members(ONLINE_INDEX_KEY);
        List<Map<String, Object>> result = new ArrayList<>();

        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        for (Object userId : userIds) {
            String key = ONLINE_KEY_PREFIX + userId;
            Map<Object, Object> info = redisTemplate.opsForHash().entries(key);
            if (info != null && !info.isEmpty()) {
                Map<String, Object> user = new LinkedHashMap<>();
                user.put("userId", info.get("userId"));
                user.put("username", info.get("username"));
                user.put("nickname", info.get("nickname"));
                user.put("ip", info.get("ip"));
                user.put("loginTime", info.get("loginTime"));
                result.add(user);
            } else {
                // 清理过期的索引
                redisTemplate.opsForSet().remove(ONLINE_INDEX_KEY, userId);
            }
        }
        return result;
    }

    /**
     * 强制用户下线
     */
    public void forceLogout(Long userId) {
        String key = ONLINE_KEY_PREFIX + userId;
        redisTemplate.delete(key);
        redisTemplate.opsForSet().remove(ONLINE_INDEX_KEY, userId.toString());

        // 同时加入黑名单，使 token 失效
        String tokenKey = "token:" + userId;
        String token = (String) redisTemplate.opsForValue().get(tokenKey);
        if (token != null) {
            redisTemplate.opsForValue().set("blacklist:" + token, "1",
                    Duration.ofHours(2));
            redisTemplate.delete(tokenKey);
        }
    }

    /**
     * 获取在线用户数
     */
    public long getOnlineCount() {
        Long count = redisTemplate.opsForSet().size(ONLINE_INDEX_KEY);
        return count != null ? count : 0;
    }
}
