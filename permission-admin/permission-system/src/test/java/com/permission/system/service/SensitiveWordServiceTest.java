package com.permission.system.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.permission.common.entity.ThSensitiveWord;
import com.permission.common.exception.BusinessException;
import com.permission.system.mapper.ThSensitiveWordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 敏感词过滤单元测试
 *
 * 覆盖点：L1 直接拦截、L2 转审、未命中、总开关关闭、大小写归一化、重复词与批量导入。
 * 采用宽松 stub 策略：部分用例会重建词库，公共桩在该用例中不再被调用属正常情况。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SensitiveWordServiceTest {

    @Mock
    private ThSensitiveWordMapper sensitiveWordMapper;

    @Mock
    private ThSettingsService settingsService;

    private SensitiveWordService sensitiveWordService;

    @BeforeEach
    void setUp() {
        sensitiveWordService = new SensitiveWordService(sensitiveWordMapper, settingsService);

        ThSensitiveWord l1 = word("加微信", 1, "广告");
        ThSensitiveWord l1b = word("赌博", 1, "违法");
        ThSensitiveWord l2 = word("傻子", 2, "辱骂");
        when(sensitiveWordMapper.selectList(any(Wrapper.class))).thenReturn(List.of(l1, l1b, l2));
        // 默认开启过滤
        when(settingsService.isEnabled("sensitive_filter_enabled")).thenReturn(true);

        sensitiveWordService.refresh();
    }

    private ThSensitiveWord word(String text, int level, String category) {
        ThSensitiveWord w = new ThSensitiveWord();
        w.setWord(text);
        w.setLevel(level);
        w.setCategory(category);
        w.setStatus(1);
        w.setDeleted(0);
        return w;
    }

    @Test
    void refresh_ShouldLoadEnabledWordsOnlyStructure() {
        assertEquals(3, sensitiveWordService.getWordCount());
    }

    @Test
    void check_Level1Word_ShouldBeBlocked() {
        assertTrue(sensitiveWordService.isBlocked("快来加微信买东西"));
        SensitiveWordService.SensitiveHit hit = sensitiveWordService.check("快来加微信买东西");
        assertNotNull(hit);
        assertEquals(1, hit.level());
        assertEquals("加微信", hit.word());
    }

    @Test
    void check_Level2Word_ShouldNeedAuditButNotBlocked() {
        assertFalse(sensitiveWordService.isBlocked("你这个傻子"));
        assertTrue(sensitiveWordService.needAudit("你这个傻子"));
    }

    @Test
    void check_CleanText_ShouldReturnNull() {
        assertNull(sensitiveWordService.check("今天天气不错，去公园散步"));
        assertFalse(sensitiveWordService.isBlocked("今天天气不错"));
    }

    @Test
    void check_WhenFilterDisabled_ShouldAlwaysPass() {
        when(settingsService.isEnabled("sensitive_filter_enabled")).thenReturn(false);
        assertNull(sensitiveWordService.check("加微信赌博"));
        assertFalse(sensitiveWordService.isBlocked("加微信"));
    }

    @Test
    void check_ShouldBeCaseInsensitive() {
        // 词库加载时统一转小写，英文词同样命中
        ThSensitiveWord en = word("spam", 1, "广告");
        when(sensitiveWordMapper.selectList(any(Wrapper.class))).thenReturn(List.of(en));
        sensitiveWordService.refresh();

        assertTrue(sensitiveWordService.isBlocked("this is SPAM content"));
    }

    @Test
    void check_EmptyOrNullText_ShouldReturnNull() {
        assertNull(sensitiveWordService.check(null));
        assertNull(sensitiveWordService.check(""));
    }

    @Test
    void add_DuplicateWord_ShouldThrow() {
        when(sensitiveWordMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        ThSensitiveWord dup = word("加微信", 1, "广告");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sensitiveWordService.add(dup, "admin"));
        assertTrue(ex.getMessage().contains("已存在"));
    }

    @Test
    void importBatch_ShouldSkipDuplicateAndBlank() {
        when(sensitiveWordMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        int inserted = sensitiveWordService.importBatch("刷屏, 违规词\n\n  ",
                1, "广告", "admin");
        assertEquals(2, inserted);
    }

    @Test
    void page_ShouldDelegateToMapper() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ThSensitiveWord> mocked =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        mocked.setRecords(List.of(word("加微信", 1, "广告")));
        mocked.setTotal(1);
        when(sensitiveWordMapper.selectPage(any(), any(Wrapper.class))).thenReturn(mocked);

        IPage<ThSensitiveWord> result = sensitiveWordService.page(1, 10, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        verify(sensitiveWordMapper, times(1)).selectPage(any(), any(Wrapper.class));
    }

    @Test
    void update_UserLevelOutOfRange_ShouldThrow() {
        ThSensitiveWord existing = word("测试词", 1, "其他");
        existing.setId(1L);
        when(sensitiveWordMapper.selectById(1L)).thenReturn(existing);

        ThSensitiveWord bad = new ThSensitiveWord();
        bad.setId(1L);
        bad.setWord("测试词");
        bad.setLevel(5);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sensitiveWordService.update(bad));
        assertTrue(ex.getMessage().contains("等级"));
    }
}
