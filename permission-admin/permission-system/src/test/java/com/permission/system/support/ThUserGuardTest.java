package com.permission.system.support;

import com.permission.common.entity.ThUser;
import com.permission.common.exception.BusinessException;
import com.permission.system.mapper.ThUserLogMapper;
import com.permission.system.mapper.ThUserMapper;
import com.permission.system.service.ThSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 用户治理守卫单元测试
 *
 * 覆盖点：封号拦截、禁言拦截、违规计分、阈值自动禁言/封号、处罚留痕。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ThUserGuardTest {

    @Mock
    private ThUserMapper userMapper;

    @Mock
    private ThUserLogMapper userLogMapper;

    @Mock
    private ThSettingsService settingsService;

    @InjectMocks
    private ThUserGuard userGuard;

    @BeforeEach
    void setUp() {
        // 默认策略：违规 5 分禁言 3 天，10 分封号 7 天，一次举报成立计 2 分
        when(settingsService.get("violation_mute_threshold")).thenReturn("5");
        when(settingsService.get("violation_mute_days")).thenReturn("3");
        when(settingsService.get("violation_ban_threshold")).thenReturn("10");
        when(settingsService.get("violation_ban_days")).thenReturn("7");
        when(settingsService.get("report_violation_score")).thenReturn("2");
    }

    private ThUser user(Long id, Integer status, LocalDateTime muteUntil, LocalDateTime banUntil, int violation) {
        ThUser u = new ThUser();
        u.setId(id);
        u.setUsername("u" + id);
        u.setStatus(status);
        u.setMuteUntil(muteUntil);
        u.setBanUntil(banUntil);
        u.setViolationCount(violation);
        return u;
    }

    @Test
    void checkBanned_StatusDisabled_ShouldThrow() {
        ThUser banned = user(1L, 0, null, null, 0);
        BusinessException ex = assertThrows(BusinessException.class, () -> userGuard.checkBanned(banned));
        assertTrue(ex.getMessage().contains("封禁"));
    }

    @Test
    void checkBanned_BanUntilInFuture_ShouldThrow() {
        ThUser banned = user(2L, 1, null, LocalDateTime.now().plusDays(2), 0);
        assertThrows(BusinessException.class, () -> userGuard.checkBanned(banned));
    }

    @Test
    void checkBanned_ExpiredBan_ShouldPass() {
        ThUser normal = user(3L, 1, null, LocalDateTime.now().minusDays(1), 0);
        assertDoesNotThrow(() -> userGuard.checkBanned(normal));
    }

    @Test
    void checkMuted_ActiveMute_ShouldThrow() {
        when(userMapper.selectById(4L)).thenReturn(user(4L, 1, LocalDateTime.now().plusHours(5), null, 0));
        BusinessException ex = assertThrows(BusinessException.class, () -> userGuard.checkMuted(4L));
        assertTrue(ex.getMessage().contains("禁言"));
    }

    @Test
    void checkMuted_ExpiredMute_ShouldPass() {
        when(userMapper.selectById(5L)).thenReturn(user(5L, 1, LocalDateTime.now().minusHours(1), null, 0));
        assertDoesNotThrow(() -> userGuard.checkMuted(5L));
    }

    @Test
    void addViolation_BelowThreshold_ShouldOnlyAccumulate() {
        when(userMapper.selectById(6L)).thenReturn(user(6L, 1, null, null, 0));

        int total = userGuard.addViolation(6L, 2, "测试违规");

        assertEquals(2, total);
        verify(userMapper).updateById(argThat(u -> u.getViolationCount() == 2
                && u.getMuteUntil() == null
                && u.getBanUntil() == null));
        verify(userLogMapper).insert(any()); // 处罚留痕
    }

    @Test
    void addViolation_ReachMuteThreshold_ShouldAutoMute() {
        when(userMapper.selectById(7L)).thenReturn(user(7L, 1, null, null, 4));

        int total = userGuard.addViolation(7L, 2, "举报成立");

        assertEquals(6, total);
        verify(userMapper).updateById(argThat(u -> u.getMuteUntil() != null
                && u.getMuteUntil().isAfter(LocalDateTime.now().plusDays(2))
                && u.getBanUntil() == null));
    }

    @Test
    void addViolation_ReachBanThreshold_ShouldAutoBan() {
        when(userMapper.selectById(8L)).thenReturn(user(8L, 1, null, null, 9));

        int total = userGuard.addViolation(8L, 2, "多次违规");

        assertEquals(11, total);
        verify(userMapper).updateById(argThat(u -> u.getBanUntil() != null
                && u.getBanUntil().isAfter(LocalDateTime.now().plusDays(6))));
    }

    @Test
    void getReportScore_ShouldReadFromSettings() {
        assertEquals(2, userGuard.getReportScore());
    }

    @Test
    void getReportScore_IllegalValue_ShouldFallbackToDefault() {
        when(settingsService.get("report_violation_score")).thenReturn("abc");
        assertEquals(2, userGuard.getReportScore());
    }

    @Test
    void mute_ShouldWriteAuditLog() {
        when(userMapper.selectById(9L)).thenReturn(user(9L, 1, null, null, 0));

        userGuard.mute(9L, 3, "恶意灌水", 100L);

        verify(userMapper).updateById(argThat(u -> u.getMuteUntil() != null));
        verify(userLogMapper).insert(argThat(log ->
                "MUTE".equals(log.getAction()) && log.getDetail().contains("恶意灌水")));
    }

    @Test
    void release_ShouldClearPenaltyAndOptionallyResetViolation() {
        when(userMapper.selectById(10L)).thenReturn(user(10L, 0, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1), 8));

        userGuard.release(10L, true, 100L);

        verify(userMapper).updateById(argThat(u -> u.getMuteUntil() == null
                && u.getBanUntil() == null
                && u.getStatus() == 1
                && u.getViolationCount() == 0));
    }

    @Test
    void checkMuted_UserNotFound_ShouldThrow() {
        when(userMapper.selectById(anyLong())).thenReturn(null);
        assertThrows(BusinessException.class, () -> userGuard.checkMuted(999L));
    }
}
