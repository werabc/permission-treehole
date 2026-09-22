package com.permission.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.permission.common.ResultCode;
import com.permission.common.entity.ThSensitiveWord;
import com.permission.common.exception.BusinessException;
import com.permission.system.mapper.ThSensitiveWordMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 敏感词过滤服务 —— Trie（前缀树）实现
 *
 * 设计要点：
 *  1. 词库全量加载进内存前缀树，单次匹配 O(n)（n 为文本长度），与词库规模无关
 *  2. 词库变更由管理端调用 refresh() 主动刷新，避免每次发帖都查库
 *  3. 支持分级：level=1 直接拦截，level=2 警告并转人工审核
 *  4. 总开关 sensitive_filter_enabled 走站点配置，可后台秒关
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordService {

    private final ThSensitiveWordMapper sensitiveWordMapper;
    private final ThSettingsService settingsService;

    /** 前缀树根节点（volatile：刷新时整体替换引用，读线程无需加锁） */
    private volatile TrieNode root = new TrieNode();

    /** 当前词库规模，便于监控 */
    private volatile int wordCount = 0;

    private static final String SWITCH_KEY = "sensitive_filter_enabled";

    @PostConstruct
    public void init() {
        try {
            refresh();
        } catch (Exception e) {
            // 启动时数据库不可用不应阻断服务启动，治理能力降级为"放行"
            log.warn("敏感词库初始化失败，过滤将暂时失效: {}", e.getMessage());
        }
    }

    /**
     * 重新加载词库（管理端增删改后调用）
     */
    public void refresh() {
        List<ThSensitiveWord> words = sensitiveWordMapper.selectList(
                new LambdaQueryWrapper<ThSensitiveWord>()
                        .eq(ThSensitiveWord::getStatus, 1)
                        .eq(ThSensitiveWord::getDeleted, 0));

        TrieNode newRoot = new TrieNode();
        int count = 0;
        for (ThSensitiveWord word : words) {
            if (word.getWord() == null || word.getWord().isBlank()) continue;
            insert(newRoot, word.getWord().trim().toLowerCase(),
                    word.getLevel() == null ? 1 : word.getLevel());
            count++;
        }
        this.root = newRoot;
        this.wordCount = count;
        log.info("敏感词库已加载，共 {} 条", count);
    }

    /**
     * 检测文本是否命中敏感词
     *
     * @param text 待检测文本
     * @return 命中结果；未命中或总开关关闭时返回 null
     */
    public SensitiveHit check(String text) {
        if (!isEnabled() || text == null || text.isEmpty()) return null;

        String lower = text.toLowerCase();
        TrieNode node = root;
        for (int i = 0; i < lower.length(); i++) {
            node = node.children.get(lower.charAt(i));
            if (node == null) {
                node = root;
                continue;
            }
            if (node.word != null) {
                return new SensitiveHit(node.word, node.level);
            }
        }
        return null;
    }

    /** 便捷方法：是否命中需要直接拦截的敏感词（level=1） */
    public boolean isBlocked(String text) {
        SensitiveHit hit = check(text);
        return hit != null && hit.level() == 1;
    }

    /** 便捷方法：是否命中需要转人工审核的敏感词（level=2） */
    public boolean needAudit(String text) {
        SensitiveHit hit = check(text);
        return hit != null && hit.level() == 2;
    }

    /** 过滤总开关 */
    public boolean isEnabled() {
        return settingsService.isEnabled(SWITCH_KEY);
    }

    public int getWordCount() {
        return wordCount;
    }

    // ========== 管理端维护接口 ==========

    /** 分页查询词库（可按关键词/等级/状态过滤） */
    public IPage<ThSensitiveWord> page(long pageNum, long pageSize, String keyword, Integer level, Integer status) {
        Page<ThSensitiveWord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ThSensitiveWord> wrapper = new LambdaQueryWrapper<ThSensitiveWord>()
                .eq(ThSensitiveWord::getDeleted, 0)
                .eq(level != null, ThSensitiveWord::getLevel, level)
                .eq(status != null, ThSensitiveWord::getStatus, status)
                .like(keyword != null && !keyword.isBlank(), ThSensitiveWord::getWord, keyword)
                .orderByDesc(ThSensitiveWord::getCreateTime);
        return sensitiveWordMapper.selectPage(page, wrapper);
    }

    /** 新增单个敏感词（重复则报错） */
    public void add(ThSensitiveWord word, String operator) {
        validate(word);
        Long exists = sensitiveWordMapper.selectCount(new LambdaQueryWrapper<ThSensitiveWord>()
                .eq(ThSensitiveWord::getWord, word.getWord().trim()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "敏感词已存在：" + word.getWord());
        }
        word.setWord(word.getWord().trim());
        if (word.getLevel() == null) word.setLevel(1);
        if (word.getStatus() == null) word.setStatus(1);
        if (word.getCategory() == null || word.getCategory().isBlank()) word.setCategory("其他");
        word.setCreateBy(operator);
        sensitiveWordMapper.insert(word);
        refresh();
    }

    /** 批量导入（每行一个词，或逗号/顿号分隔；自动跳过重复） */
    public int importBatch(String text, Integer defaultLevel, String category, String operator) {
        if (text == null || text.isBlank()) return 0;
        String[] tokens = text.split("[\\r\\n,，、;；]+");
        int inserted = 0;
        for (String token : tokens) {
            String w = token.trim();
            if (w.isEmpty()) continue;
            Long exists = sensitiveWordMapper.selectCount(new LambdaQueryWrapper<ThSensitiveWord>()
                    .eq(ThSensitiveWord::getWord, w));
            if (exists != null && exists > 0) continue;
            ThSensitiveWord entity = new ThSensitiveWord();
            entity.setWord(w);
            entity.setLevel(defaultLevel == null ? 1 : defaultLevel);
            entity.setCategory(category == null || category.isBlank() ? "其他" : category);
            entity.setStatus(1);
            entity.setCreateBy(operator);
            sensitiveWordMapper.insert(entity);
            inserted++;
        }
        if (inserted > 0) refresh();
        log.info("批量导入敏感词 {} 条，执行人={}", inserted, operator);
        return inserted;
    }

    /** 修改（词/等级/分类/状态） */
    public void update(ThSensitiveWord word) {
        if (word.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "敏感词ID不能为空");
        }
        ThSensitiveWord existing = sensitiveWordMapper.selectById(word.getId());
        if (existing == null || existing.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "敏感词不存在");
        }
        if (word.getWord() != null) {
            validate(word);
            existing.setWord(word.getWord().trim());
        }
        if (word.getLevel() != null) existing.setLevel(word.getLevel());
        if (word.getCategory() != null) existing.setCategory(word.getCategory());
        if (word.getStatus() != null) existing.setStatus(word.getStatus());
        if (word.getRemark() != null) existing.setRemark(word.getRemark());
        sensitiveWordMapper.updateById(existing);
        refresh();
    }

    /** 删除（逻辑删除） */
    public void delete(Long id) {
        ThSensitiveWord existing = sensitiveWordMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "敏感词不存在");
        }
        sensitiveWordMapper.deleteById(id);
        refresh();
    }

    private void validate(ThSensitiveWord word) {
        if (word.getWord() == null || word.getWord().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "敏感词不能为空");
        }
        if (word.getWord().trim().length() > 50) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "敏感词长度不能超过50字");
        }
        if (word.getLevel() != null && (word.getLevel() < 1 || word.getLevel() > 2)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "等级只能是 1-拦截 或 2-转审");
        }
    }

    // ========== 内部实现 ==========

    private void insert(TrieNode node, String word, int level) {
        TrieNode cur = node;
        for (char c : word.toCharArray()) {
            cur = cur.children.computeIfAbsent(c, k -> new TrieNode());
        }
        cur.word = word;
        // 同一词重复出现时，取更严格的等级（1 比 2 严格）
        cur.level = cur.level == null ? level : Math.min(cur.level, level);
    }

    private static class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        String word;
        Integer level;
    }

    /** 命中结果 */
    public record SensitiveHit(String word, int level) {
    }
}
