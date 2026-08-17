package com.permission.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.permission.common.R;
import com.permission.common.entity.ThComment;
import com.permission.common.entity.ThPost;
import com.permission.common.entity.ThUser;
import com.permission.system.mapper.ThCommentMapper;
import com.permission.system.mapper.ThPostMapper;
import com.permission.system.mapper.ThUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "树洞评论管理")
@RestController
@RequestMapping("/api/admin/th/comment")
@RequiredArgsConstructor
public class ThCommentAdminController {

    private final ThCommentMapper commentMapper;
    private final ThPostMapper postMapper;
    private final ThUserMapper userMapper;

    @Operation(summary = "评论列表")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<IPage<ThComment>> page(@RequestParam(defaultValue = "1") long pageNum,
                                     @RequestParam(defaultValue = "10") long pageSize,
                                     @RequestParam(required = false) Long postId) {
        Page<ThComment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ThComment> wrapper = new LambdaQueryWrapper<ThComment>()
                .eq(ThComment::getDeleted, 0)
                .eq(postId != null, ThComment::getPostId, postId)
                .orderByDesc(ThComment::getCreateTime);

        IPage<ThComment> result = commentMapper.selectPage(page, wrapper);
        for (ThComment comment : result.getRecords()) {
            if (comment.getIsAnonymous() != null && comment.getIsAnonymous() == 1) {
                comment.setAuthorName("匿名用户");
            } else {
                ThUser user = userMapper.selectById(comment.getUserId());
                comment.setAuthorName(user != null ? user.getNickname() : "未知用户");
            }
        }
        return R.ok(result);
    }

    @Operation(summary = "评论详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        ThComment comment = commentMapper.selectById(id);
        if (comment == null) return R.fail(404, "评论不存在");

        Map<String, Object> result = new HashMap<>();
        result.put("id", comment.getId());
        result.put("content", comment.getContent());
        result.put("isAnonymous", comment.getIsAnonymous());
        result.put("likeCount", comment.getLikeCount());
        result.put("status", comment.getStatus());
        result.put("ip", comment.getIp());
        result.put("createTime", comment.getCreateTime());

        // 评论人
        if (comment.getIsAnonymous() != null && comment.getIsAnonymous() == 1) {
            result.put("authorName", "匿名用户");
        } else {
            ThUser user = userMapper.selectById(comment.getUserId());
            result.put("authorName", user != null ? user.getNickname() : "未知用户");
        }

        // 帖子信息
        ThPost post = postMapper.selectById(comment.getPostId());
        result.put("postId", comment.getPostId());
        result.put("postContent", post != null ? post.getContent() : "原帖已删除");

        return R.ok(result);
    }

    @Operation(summary = "隐藏/恢复评论")
    @PutMapping("/{id}/hide")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> hide(@PathVariable Long id, @RequestParam Integer status) {
        ThComment comment = commentMapper.selectById(id);
        if (comment == null) return R.fail(404, "评论不存在");
        comment.setStatus(status);
        commentMapper.updateById(comment);
        return R.ok();
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> delete(@PathVariable Long id) {
        ThComment comment = commentMapper.selectById(id);
        if (comment == null) return R.fail(404, "评论不存在");
        comment.setDeleted(1);
        commentMapper.updateById(comment);

        // 减少帖子评论数（仅当帖子存在时）
        if (comment.getPostId() != null) {
            postMapper.decrementCommentCount(comment.getPostId());
        }
        return R.ok();
    }
}
