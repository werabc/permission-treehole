package com.permission.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.permission.common.R;
import com.permission.common.dto.LoginUser;
import com.permission.common.entity.ThSensitiveWord;
import com.permission.system.service.SensitiveWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 敏感词库管理（管理端）
 * 说明：所有写操作后由 Service 内部自动重建前缀树，发帖/评论立即生效，无需重启
 */
@Slf4j
@Tag(name = "树洞敏感词管理")
@RestController
@RequestMapping("/api/admin/th/sensitive-word")
@RequiredArgsConstructor
public class ThSensitiveWordAdminController {

    private final SensitiveWordService sensitiveWordService;

    @Operation(summary = "敏感词分页列表")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<IPage<ThSensitiveWord>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "20") long pageSize,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer level,
                                          @RequestParam(required = false) Integer status) {
        return R.ok(sensitiveWordService.page(pageNum, pageSize, keyword, level, status));
    }

    @Operation(summary = "词库统计（词条数 / 总开关状态）")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Object>> stats() {
        return R.ok(Map.of(
                "wordCount", sensitiveWordService.getWordCount(),
                "enabled", sensitiveWordService.isEnabled()
        ));
    }

    @Operation(summary = "新增敏感词")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> add(@RequestBody ThSensitiveWord word,
                       @AuthenticationPrincipal LoginUser operator) {
        sensitiveWordService.add(word, operator == null ? "system" : operator.getUsername());
        return R.ok();
    }

    @Operation(summary = "批量导入（换行或逗号分隔）")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Integer> importBatch(@RequestBody Map<String, Object> body,
                                  @AuthenticationPrincipal LoginUser operator) {
        String text = (String) body.get("text");
        Integer level = body.get("level") == null ? 1 : Integer.valueOf(String.valueOf(body.get("level")));
        String category = (String) body.get("category");
        int inserted = sensitiveWordService.importBatch(text, level, category,
                operator == null ? "system" : operator.getUsername());
        return R.ok(inserted);
    }

    @Operation(summary = "修改敏感词")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> update(@RequestBody ThSensitiveWord word) {
        sensitiveWordService.update(word);
        return R.ok();
    }

    @Operation(summary = "删除敏感词")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> delete(@PathVariable Long id) {
        sensitiveWordService.delete(id);
        return R.ok();
    }

    @Operation(summary = "手动刷新词库缓存")
    @PostMapping("/refresh")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Integer> refresh() {
        sensitiveWordService.refresh();
        return R.ok(sensitiveWordService.getWordCount());
    }
}
