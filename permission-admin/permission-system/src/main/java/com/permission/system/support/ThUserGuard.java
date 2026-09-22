package com.permission.system.support;

import com.permission.common.ResultCode;
import com.permission.common.entity.ThUser;
import com.permission.common.entity.ThUserLog;
import com.permission.common.exception.BusinessException;
import com.permission.system.mapper.ThUserLogMapper;
import com.permission.system.mapper.ThUserMapper;
import com.permission.system.service.ThSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 树洞用户治理守卫 —— 统一承载"封号 / 禁言 / 违规计分 / 自动处罚"
 *
 * 为什么单独抽一个组件：
 *  - 登录、发帖、评论、点赞、举报处理 5 处都要做同样的校验，散落在各 Service 里必然漏改
 *  - 处罚策略（阈值/时长）全部走站点配置，运营可调，不需要重新发版
 *  - 违规计分与处罚动作集中在一处，避免出现"扣了分没处罚"或"处罚了没留痕"
 *
 * 所有处罚动作都会写入 th_user_log，保证可追溯。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThUserGuard {

    private final ThUserMapper userMapper;
    private final ThUserLogMapper userLogMapper;
    private final ThSettingsService settingsService;

    // ========== 配置键 ==========
    private static final String KEY_MUTE_THRESHOLD = "violation_mute_threshold";
    private static final String KEY_MUTE_DAYS = "violation_mute_days";
    private static final String KEY_BAN_THRESHOLD = "violation_ban_threshold";
    private static final String KEY_BAN_DAYS = "violation_ban_days";
    private static final String KEY_REPORT_SCORE = "report_violation_score";

    // ========== 默认值（站点配置缺失时的兜底） ==========
    private static final int DEFAULT_MUTE_THRESHOLD = 5;
    private static final int DEFAULT_MUTE_DAYS = 3;
    private static final int DEFAULT_BAN_THRESHOLD = 10;
    private static final int DEFAULT_BAN_DAYS = 7;
    private static final int DEFAULT_REPORT_SCORE = 2;

    // ========== 校验类 ==========

    /**
     * 登录前置校验：账号是否被封禁
     */
    public void checkBanned(ThUser user) {
        if (user == null) return;
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_ACCOUNT_DISABLED, "账号已被封禁");
        }
        LocalDateTime banUntil = user.getBanUntil();
        if (banUntil != null && banUntil.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.USER_ACCOUNT_DISABLED,
                    "账号已被封禁，解封时间：" + banUntil.toLocalDate());
        }
    }

    /**
     * 互动前置校验：是否处于禁言期（发帖 / 评论 / 点赞 / 举报）
     */
    public void checkMuted(Long userId) {
        if (userId == null) return;
        ThUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        // 封号用户同样不允许互动
        checkBanned(user);

        LocalDateTime muteUntil = user.getMuteUntil();
        if (muteUntil != null && muteUntil.isAfter(LocalDateTime.now())) {
            long hours = Math.max(Duration.between(LocalDateTime.now(), muteUntil).toHours(), 1);
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "你当前处于禁言期，剩余约 " + hours + " 小时");
        }
    }

    // ========== 计分与处罚 ==========

    /**
     * 增加违规分，并按阈值自动执行禁言 / 封号
     *
     * @return 本次累计后的违规分
     */
    public int addViolation(Long userId, int score, String reason) {
        if (userId == null || score <= 0) return 0;
        ThUser user = userMapper.selectById(userId);
        if (user == null) return 0;

        int total = (user.getViolationCount() == null ? 0 : user.getViolationCount()) + score;
        user.setViolationCount(total);

        int muteThreshold = getInt(KEY_MUTE_THRESHOLD, DEFAULT_MUTE_THRESHOLD);
        int banThreshold = getInt(KEY_BAN_THRESHOLD, DEFAULT_BAN_THRESHOLD);

        String action = "VIOLATION";
        String detail = reason + "（违规分 +" + score + "，当前 " + total + "）";

        if (total >= banThreshold) {
            int days = getInt(KEY_BAN_DAYS, DEFAULT_BAN_DAYS);
            user.setBanUntil(LocalDateTime.now().plusDays(days));
            user.setStatus(1); // status 保持 1，封禁由 banUntil 表达，便于到期自动解除
            action = "AUTO_BAN";
            detail += "，触发自动封号 " + days + " 天";
        } else if (total >= muteThreshold) {
            int days = getInt(KEY_MUTE_DAYS, DEFAULT_MUTE_DAYS);
            // 已有禁言且未到期时累加，避免覆盖更长的处罚
            LocalDateTime base = (user.getMuteUntil() != null && user.getMuteUntil().isAfter(LocalDateTime.now()))
                    ? user.getMuteUntil() : LocalDateTime.now();
            user.setMuteUntil(base.plusDays(days));
            action = "AUTO_MUTE";
            detail += "，触发自动禁言 " + days + " 天";
        }

        userMapper.updateById(user);
        writeLog(userId, action, "USER", userId, detail);
        log.warn("用户治理: userId={} 违规分={} 动作={}", userId, total, action);
        return total;
    }

    /** 举报成立一次计多少分（站点配置可调） */
    public int getReportScore() {
        return getInt(KEY_REPORT_SCORE, DEFAULT_REPORT_SCORE);
    }

    /** 管理员手动禁言 */
    public void mute(Long userId, int days, String reason, Long operatorId) {
        ThUser user = requireUser(userId);
        LocalDateTime base = (user.getMuteUntil() != null && user.getMuteUntil().isAfter(LocalDateTime.now()))
                ? user.getMuteUntil() : LocalDateTime.now();
        user.setMuteUntil(base.plusDays(days));
        userMapper.updateById(user);
        writeLog(operatorId, "MUTE", "USER", userId, "手动禁言 " + days + " 天：" + reason);
        log.warn("管理员手动禁言: userId={} days={} operator={}", userId, days, operatorId);
    }

    /** 管理员手动封号 */
    public void ban(Long userId, int days, String reason, Long operatorId) {
        ThUser user = requireUser(userId);
        user.setBanUntil(days <= 0 ? null : LocalDateTime.now().plusDays(days));
        // days<=0 视为永久封禁
        if (days <= 0) {
            user.setStatus(0);
        }
        userMapper.updateById(user);
        writeLog(operatorId, "BAN", "USER", userId,
                (days <= 0 ? "永久封号：" : "手动封号 " + days + " 天：") + reason);
        log.warn("管理员手动封号: userId={} days={} operator={}", userId, days, operatorId);
    }

    /** 解除处罚（禁言 + 封号 + 违规分清零可选） */
    public void release(Long userId, boolean resetViolation, Long operatorId) {
        ThUser user = requireUser(userId);
        user.setMuteUntil(null);
        user.setBanUntil(null);
        user.setStatus(1);
        if (resetViolation) {
            user.setViolationCount(0);
        }
        userMapper.updateById(user);
        writeLog(operatorId, "RELEASE", "USER", userId,
                resetViolation ? "解除处罚并清零违规分" : "解除处罚（保留违规分）");
        log.warn("管理员解除处罚: userId={} operator={}", userId, operatorId);
    }

    // ========== 私有辅助 ==========

    private ThUser requireUser(Long userId) {
        ThUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private int getInt(String key, int defaultValue) {
        try {
            String val = settingsService.get(key);
            return val == null ? defaultValue : Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("站点配置 {} 非法，使用默认值 {}", key, defaultValue);
            return defaultValue;
        }
    }

    /** 处罚留痕：失败不应影响主流程 */
    private void writeLog(Long userId, String action, String targetType, Long targetId, String detail) {
        try {
            ThUserLog userLog = new ThUserLog();
            userLog.setUserId(userId);
            userLog.setAction(action);
            userLog.setTargetType(targetType);
            userLog.setTargetId(targetId);
            userLog.setDetail(detail);
            userLogMapper.insert(userLog);
        } catch (Exception e) {
            log.error("写入用户治理日志失败 userId={} action={}", userId, action, e);
        }
    }
}
