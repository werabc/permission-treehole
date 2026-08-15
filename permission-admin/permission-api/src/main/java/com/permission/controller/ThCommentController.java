package com.permission.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.permission.common.R;
import com.permission.common.entity.ThComment;
import com.permission.system.service.ThCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "树洞评论")
@RestController
@RequestMapping("/api/th/comment")
@RequiredArgsConstructor
public class ThCommentController {

    private final ThCommentService commentService;

    @Operation(summary = "分页查询评论")
    @GetMapping("/page")
    public R<IPage<ThComment>> page(@RequestParam(defaultValue = "1") long pageNum,
                                     @RequestParam(defaultValue = "20") long pageSize,
                                     @RequestParam Long postId) {
        return R.ok(commentService.pageComments(pageNum, pageSize, postId));
    }

    @Operation(summary = "发表评论")
    @PostMapping
    public R<Long> create(@RequestBody ThComment comment, HttpServletRequest request) {
        if (StrUtil.isBlank(comment.getContent())) {
            return R.fail(400, "评论内容不能为空");
        }
        if (comment.getContent().length() > 2000) {
            return R.fail(400, "评论不能超过2000字");
        }
        if (comment.getPostId() == null) {
            return R.fail(400, "帖子ID不能为空");
        }
        comment.setIp(getClientIp(request));
        commentService.createComment(comment);
        return R.ok(comment.getId());
    }

    @Operation(summary = "点赞评论")
    @PostMapping("/{id}/like")
    public R<Void> like(@PathVariable Long id, HttpServletRequest request) {
        commentService.likeComment(id, getClientIp(request));
        return R.ok();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
