package com.permission.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.permission.common.R;
import com.permission.common.entity.ThComment;
import com.permission.common.entity.ThPost;
import com.permission.system.mapper.ThCommentMapper;
import com.permission.system.mapper.ThPostMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "树洞内容审核")
@RestController
@RequestMapping("/api/admin/th/moderation")
@RequiredArgsConstructor
public class ThModerationController {

    private final ThPostMapper postMapper;
    private final ThCommentMapper commentMapper;

    @Operation(summary = "待审核帖子")
    @GetMapping("/posts")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<IPage<ThPost>> pendingPosts(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "20") long pageSize) {
        Page<ThPost> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ThPost> wrapper = new LambdaQueryWrapper<ThPost>()
                .eq(ThPost::getDeleted, 0)
                .eq(ThPost::getStatus, 0)
                .orderByDesc(ThPost::getCreateTime);
        return R.ok(postMapper.selectPage(page, wrapper));
    }

    @Operation(summary = "待审核评论")
    @GetMapping("/comments")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<IPage<ThComment>> pendingComments(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "20") long pageSize) {
        Page<ThComment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ThComment> wrapper = new LambdaQueryWrapper<ThComment>()
                .eq(ThComment::getDeleted, 0)
                .eq(ThComment::getStatus, 0)
                .orderByDesc(ThComment::getCreateTime);
        return R.ok(commentMapper.selectPage(page, wrapper));
    }

    @Operation(summary = "批量审核")
    @PostMapping("/batch-audit")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> batchAudit(@RequestBody Map<String, Object> body) {
        String type = (String) body.get("type");
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) body.get("ids");
        Integer status = (Integer) body.get("status");

        if (ids == null || ids.isEmpty()) return R.fail(400, "ID列表不能为空");

        if ("post".equals(type)) {
            for (Long id : ids) {
                ThPost post = postMapper.selectById(id);
                if (post != null) {
                    post.setStatus(status);
                    post.setAuditTime(java.time.LocalDateTime.now());
                    postMapper.updateById(post);
                }
            }
        } else if ("comment".equals(type)) {
            for (Long id : ids) {
                ThComment comment = commentMapper.selectById(id);
                if (comment != null) {
                    comment.setStatus(status);
                    commentMapper.updateById(comment);
                }
            }
        }
        return R.ok();
    }

    @Operation(summary = "审核统计")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Long>> stats() {
        Map<String, Long> result = new HashMap<>();
        result.put("pendingPosts", postMapper.selectCount(new LambdaQueryWrapper<ThPost>()
                .eq(ThPost::getDeleted, 0).eq(ThPost::getStatus, 0)));
        result.put("pendingComments", commentMapper.selectCount(new LambdaQueryWrapper<ThComment>()
                .eq(ThComment::getDeleted, 0).eq(ThComment::getStatus, 0)));
        result.put("approvedPosts", postMapper.selectCount(new LambdaQueryWrapper<ThPost>()
                .eq(ThPost::getDeleted, 0).eq(ThPost::getStatus, 1)));
        result.put("rejectedPosts", postMapper.selectCount(new LambdaQueryWrapper<ThPost>()
                .eq(ThPost::getDeleted, 0).eq(ThPost::getStatus, 2)));
        return R.ok(result);
    }
}
