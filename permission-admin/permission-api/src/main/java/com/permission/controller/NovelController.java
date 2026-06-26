package com.permission.controller;

import com.permission.common.R;
import com.permission.common.dto.LoginUser;
import com.permission.common.entity.Novel;
import com.permission.system.service.NovelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "小说管理")
@RestController
@RequestMapping("/api/novel")
@RequiredArgsConstructor
public class NovelController {

    private final NovelService novelService;

    @Operation(summary = "前台-分页查询已发布小说")
    @GetMapping("/published")
    public R<?> published(@RequestParam(defaultValue = "1") long pageNum,
                          @RequestParam(defaultValue = "12") long pageSize,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long categoryId) {
        return R.ok(novelService.pagePublishedNovels(pageNum, pageSize, keyword, categoryId));
    }

    @Operation(summary = "后台-分页查询所有小说")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<?> page(@RequestParam(defaultValue = "1") long pageNum,
                     @RequestParam(defaultValue = "10") long pageSize,
                     @RequestParam(required = false) String keyword,
                     @RequestParam(required = false) Long categoryId,
                     @RequestParam(required = false) Integer status) {
        return R.ok(novelService.pageNovels(pageNum, pageSize, keyword, categoryId, status, null));
    }

    @Operation(summary = "作者-分页查询自己的小说")
    @GetMapping("/my")
    @PreAuthorize("hasAnyAuthority('author', 'admin')")
    public R<?> myNovels(@RequestParam(defaultValue = "1") long pageNum,
                         @RequestParam(defaultValue = "10") long pageSize,
                         @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(novelService.pageNovels(pageNum, pageSize, null, null, null, loginUser.getUserId()));
    }

    @Operation(summary = "获取小说详情")
    @GetMapping("/{id}")
    public R<Novel> detail(@PathVariable Long id) {
        return R.ok(novelService.getNovelDetail(id));
    }

    @Operation(summary = "作者-新增小说")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('author', 'admin')")
    public R<Void> create(@RequestBody Novel novel, @AuthenticationPrincipal LoginUser loginUser) {
        novelService.createNovel(novel, loginUser.getUserId(), loginUser.getUsername());
        return R.ok();
    }

    @Operation(summary = "作者-修改小说")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('author', 'admin')")
    public R<Void> update(@PathVariable Long id, @RequestBody Novel novel,
                          @AuthenticationPrincipal LoginUser loginUser) {
        novel.setId(id);
        novelService.updateNovel(novel, loginUser.getUserId());
        return R.ok();
    }

    @Operation(summary = "作者-删除小说")
    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAnyAuthority('author', 'admin')")
    public R<Void> delete(@PathVariable List<Long> ids, @AuthenticationPrincipal LoginUser loginUser) {
        novelService.deleteNovels(ids, loginUser.getUserId());
        return R.ok();
    }
}
