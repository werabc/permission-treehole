package com.permission.controller;

import com.permission.common.R;
import com.permission.common.entity.NovelChapter;
import com.permission.system.service.NovelChapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "小说章节管理")
@RestController
@RequestMapping("/api/novel/chapter")
@RequiredArgsConstructor
public class NovelChapterController {

    private final NovelChapterService chapterService;

    @Operation(summary = "获取小说章节列表")
    @GetMapping("/list/{novelId}")
    public R<List<NovelChapter>> list(@PathVariable Long novelId) {
        return R.ok(chapterService.listByNovel(novelId));
    }

    @Operation(summary = "获取章节详情(内容)")
    @GetMapping("/{id}")
    public R<NovelChapter> detail(@PathVariable Long id) {
        return R.ok(chapterService.getChapterDetail(id));
    }

    @Operation(summary = "获取上一章/下一章")
    @GetMapping("/{id}/nav")
    public R<?> nav(@PathVariable Long id) {
        NovelChapter chapter = chapterService.getChapterDetail(id);
        NovelChapter prev = chapterService.getPrevChapter(chapter.getNovelId(), chapter.getChapterNum());
        NovelChapter next = chapterService.getNextChapter(chapter.getNovelId(), chapter.getChapterNum());
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("prev", prev);
        map.put("next", next);
        return R.ok(map);
    }

    @Operation(summary = "作者-新增章节")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('author', 'admin')")
    public R<Void> create(@RequestBody NovelChapter chapter) {
        chapterService.createChapter(chapter);
        return R.ok();
    }

    @Operation(summary = "作者-修改章节")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('author', 'admin')")
    public R<Void> update(@PathVariable Long id, @RequestBody NovelChapter chapter) {
        chapter.setId(id);
        chapterService.updateChapter(chapter);
        return R.ok();
    }

    @Operation(summary = "作者-删除章节")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('author', 'admin')")
    public R<Void> delete(@PathVariable Long id) {
        chapterService.deleteChapter(id);
        return R.ok();
    }
}
