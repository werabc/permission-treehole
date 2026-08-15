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
        comment.setUserId(userId);
        commentService.createComment(comment);
        return R.ok(comment.getId());
    }

    @Operation(summary = "点赞评论 (需登录)")
    @PostMapping("/{id}/like")
    public R<Void> like(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return R.fail(401, "请先登录");
        commentService.likeComment(id, userId);
        return R.ok();
    }

    private Long getUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StrUtil.isBlank(header) || !header.startsWith("Bearer ")) return null;
        String token = header.substring(7);
        try {
            String json = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]));
            return cn.hutool.json.JSONUtil.parseObj(json).getLong("sub");
        } catch (Exception e) {
            return null;
        }
    }
}
