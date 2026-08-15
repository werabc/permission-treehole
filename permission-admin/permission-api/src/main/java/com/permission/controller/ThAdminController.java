package com.permission.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.permission.common.R;
import com.permission.common.annotation.OperationLog;
import com.permission.common.entity.*;
import com.permission.system.mapper.ThCategoryMapper;
import com.permission.system.mapper.ThCommentMapper;
import com.permission.system.mapper.ThLikeMapper;
import com.permission.system.mapper.ThPostMapper;
import com.permission.system.mapper.ThReportMapper;
import com.permission.system.service.ThPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "树洞管理后台")
@RestController
@RequestMapping("/api/admin/treehole")
@RequiredArgsConstructor
public class ThAdminController {

    private final ThPostService postService;
    private final ThPostMapper postMapper;
    private final ThCommentMapper commentMapper;
    private final ThCategoryMapper categoryMapper;
    private final ThReportMapper reportMapper;
    private final ThLikeMapper likeMapper;

    // ========== 帖子管理 ==========

    @Operation(summary = "分页查询帖子")
    @GetMapping("/post/page")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<IPage<ThPost>> postPage(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) Integer status) {
        Page<ThPost> page = new Page(pageNum, pageSize);
        LambdaQueryWrapper<ThPost> wrapper = new LambdaQueryWrapper<ThPost>()
                .eq(ThPost::getDeleted, 0)
                .eq(status != null, ThPost::getStatus, status)
                .orderByDesc(ThPost::getCreateTime);
        return R.ok(postMapper.selectPage(page, wrapper));
    }

    @Operation(summary = "审核帖子")
    @PutMapping("/post/{id}/audit")
    @PreAuthorize("hasAnyAuthority('admin')")
    @OperationLog(module = "树洞管理", value = "审核帖子")
    public R<Void> auditPost(@PathVariable Long id, @RequestParam Integer status) {
        ThPost post = postMapper.selectById(id);
        if (post == null) return R.fail(404, "帖子不存在");
        post.setStatus(status);
        postMapper.updateById(post);
        return R.ok();
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/post/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    @OperationLog(module = "树洞管理", value = "删除帖子")
    public R<Void> deletePost(@PathVariable Long id) {
        ThPost post = postMapper.selectById(id);
        if (post == null) return R.fail(404, "帖子不存在");
        post.setDeleted(1);
        post.setStatus(3);
        postMapper.updateById(post);
        return R.ok();
    }

    // ========== 评论管理 ==========

    @Operation(summary = "分页查询评论")
    @GetMapping("/comment/page")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<IPage<ThComment>> commentPage(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        Page<ThComment> page = new Page(pageNum, pageSize);
        LambdaQueryWrapper<ThComment> wrapper = new LambdaQueryWrapper<ThComment>()
                .eq(ThComment::getDeleted, 0)
                .orderByDesc(ThComment::getCreateTime);
        return R.ok(commentMapper.selectPage(page, wrapper));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/comment/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    @OperationLog(module = "树洞管理", value = "删除评论")
    public R<Void> deleteComment(@PathVariable Long id) {
        ThComment comment = commentMapper.selectById(id);
        if (comment == null) return R.fail(404, "评论不存在");
        comment.setDeleted(1);
        commentMapper.updateById(comment);
        return R.ok();
    }

    // ========== 分类管理 ==========

    @Operation(summary = "获取所有分类")
    @GetMapping("/category/list")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<List<ThCategory>> categoryList() {
        return R.ok(categoryMapper.selectList(new LambdaQueryWrapper<ThCategory>().eq(ThCategory::getDeleted, 0)));
    }

    @Operation(summary = "创建分类")
    @PostMapping("/category")
    @PreAuthorize("hasAnyAuthority('admin')")
    @OperationLog(module = "树洞管理", value = "创建分类")
    public R<Void> createCategory(@RequestBody ThCategory category) {
        category.setPostCount(0);
        categoryMapper.insert(category);
        return R.ok();
    }

    @Operation(summary = "更新分类")
    @PutMapping("/category/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    @OperationLog(module = "树洞管理", value = "更新分类")
    public R<Void> updateCategory(@PathVariable Long id, @RequestBody ThCategory category) {
        category.setId(id);
        categoryMapper.updateById(category);
        return R.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/category/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    @OperationLog(module = "树洞管理", value = "删除分类")
    public R<Void> deleteCategory(@PathVariable Long id) {
        ThCategory category = categoryMapper.selectById(id);
        if (category == null) return R.fail(404, "分类不存在");
        category.setDeleted(1);
        categoryMapper.updateById(category);
        return R.ok();
    }

    // ========== 举报管理 ==========

    @Operation(summary = "分页查询举报")
    @GetMapping("/report/page")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<IPage<ThReport>> reportPage(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize) {
        Page<ThReport> page = new Page(pageNum, pageSize);
        LambdaQueryWrapper<ThReport> wrapper = new LambdaQueryWrapper<ThReport>()
                .eq(ThReport::getDeleted, 0)
                .orderByDesc(ThReport::getCreateTime);
        return R.ok(reportMapper.selectPage(page, wrapper));
    }

    @Operation(summary = "处理举报")
    @PutMapping("/report/{id}/handle")
    @PreAuthorize("hasAnyAuthority('admin')")
    @OperationLog(module = "树洞管理", value = "处理举报")
    public R<Void> handleReport(@PathVariable Long id, @RequestParam Integer status, @RequestParam(required = false) String result) {
        ThReport report = reportMapper.selectById(id);
        if (report == null) return R.fail(404, "举报不存在");
        report.setStatus(status);
        report.setHandleResult(result);
        reportMapper.updateById(report);
        return R.ok();
    }

    // ========== 统计 ==========

    @Operation(summary = "统计数据")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Object>> statistics() {
        Map<String, Object> stats = new HashMap<>();

        // 帖子统计
        stats.put("totalPosts", postMapper.selectCount(new LambdaQueryWrapper<ThPost>().eq(ThPost::getDeleted, 0)));
        stats.put("pendingPosts", postMapper.selectCount(new LambdaQueryWrapper<ThPost>().eq(ThPost::getDeleted, 0).eq(ThPost::getStatus, 0)));

        // 评论统计
        stats.put("totalComments", commentMapper.selectCount(new LambdaQueryWrapper<ThComment>().eq(ThComment::getDeleted, 0)));

        // 举报统计
        stats.put("pendingReports", reportMapper.selectCount(new LambdaQueryWrapper<ThReport>().eq(ThReport::getDeleted, 0).eq(ThReport::getStatus, 0)));

        return R.ok(stats);
    }
}
