package com.permission.system.support;

import com.permission.common.ResultCode;
import com.permission.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 树洞互动频率限制器 —— 按自然日计数的 Redis 计数器
 *
 * 说明：
 *  - 计数键：th:rate:{action}:{userId}:{yyyy-MM-dd}，过期时间设到次日 0 点，天然按日归零
 *  - 限额 -1 或 0 表示不限制（方便运营临时放开）
 *  - Redis 异常时采用"放行"策略：限流是保护手段，不应因缓存故障阻断正常用户
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "th:rate:";
    private static final int EXTRA_TTL_SECONDS = 60;

    /**
     * 校验并累加当日操作次数
     *
     * @param userId 用户ID
     * @param action 动作标识（post / comment）
     * @param limit  每日上限，<=0 表示不限制
     * @param label  提示文案（如"今日发帖"）
     */
    public void checkDaily(Long userId, String action, int limit, String label) {
        if (userId == null || limit <= 0) return;

        String key = KEY_PREFIX + action + ":" + userId + ":" + java.time.LocalDate.now();
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) return;

            if (count == 1L) {
                redisTemplate.expire(key, ttlToNextDay(), java.util.concurrent.TimeUnit.SECONDS);
            }
            if (count > limit) {
                throw new BusinessException(ResultCode.RATE_LIMITED,
                        label + "已达每日上限（" + limit + " 次），请明天再来");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // 缓存不可用时放行，仅记录告警
            log.warn("频率限制校验降级放行 key={} 原因={}", key, e.getMessage());
        }
    }

    /** 剩余计数（供前端提示展示） */
    public long remaining(Long userId, String action, int limit) {
        if (userId == null || limit <= 0) return -1;
        try {
            Object val = redisTemplate.opsForValue().get(KEY_PREFIX + action + ":" + userId + ":" + java.time.LocalDate.now());
            long used = val instanceof Number ? ((Number) val).longValue() : 0L;
            return Math.max(limit - used, 0);
        } catch (Exception e) {
            return -1;
        }
    }

    /** 距离次日 0 点的秒数（+60 秒冗余，避免临界丢键） */
    private long ttlToNextDay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.toLocalDate().plusDays(1).atTime(LocalTime.MIDNIGHT);
        return Math.max(Duration.between(now, tomorrow).getSeconds(), 1) + EXTRA_TTL_SECONDS;
    }
}
