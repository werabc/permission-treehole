package com.permission.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.permission.common.R;
import com.permission.common.entity.ThComment;
import com.permission.framework.security.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "分页查询评论 (公开)")
    @GetMapping("/page")
    public R<IPage<ThComment>> page(@RequestParam(defaultValue = "1") long pageNum,
                                     @RequestParam(defaultValue = "20") long pageSize,
                                     @RequestParam Long postId) {
        return R.ok(commentService.pageComments(pageNum, pageSize, postId));
    }

    @Operation(summary = "发表评论 (需登录)")
    @PostMapping
    public R<Long> create(@RequestBody ThComment comment, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return R.fail(401, "请先登录");

        if (StrUtil.isBlank(comment.getContent())) {
            return R.fail(400, "评论内容不能为空");
        }
        if (comment.getContent().length() > 2000) {
            return R.fail(400, "评论不能超过2000字");
        }
        if (comment.getPostId() == null) {
            return R.fail(400, "帖子ID不能为空");
        }
        comment.setUserId(userId);
        comment.setIp(String.valueOf(userId));
        commentService.createComment(comment);
        return R.ok(comment.getId());
    }

    @Operation(summary = "点赞评论 (需登录)")
    @PostMapping("/{id}/like")
    public R<Void> like(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return R.fail(401, "请先登录");
        commentService.likeComment(id, String.valueOf(userId));
        return R.ok();
    }

    private Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token) || !token.startsWith("Bearer ")) return null;
        token = token.substring(7);
        try {
            return jwtTokenProvider.getUserId(token);
        } catch (Exception e) {
            return null;
        }
    }
}
