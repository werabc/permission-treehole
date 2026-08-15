package com.permission.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.permission.common.R;
import com.permission.common.entity.ThPost;
import com.permission.system.service.ThPostService;
import com.permission.system.service.ThCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "树洞帖子")
@RestController
@RequestMapping("/api/th/post")
@RequiredArgsConstructor
public class ThPostController {

    private final ThPostService postService;
    private final ThCategoryService categoryService;

    @Operation(summary = "分页查询帖子")
    @GetMapping("/page")
    public R<IPage<ThPost>> page(@RequestParam(defaultValue = "1") long pageNum,
                                  @RequestParam(defaultValue = "10") long pageSize,
                                  @RequestParam(required = false) Long categoryId,
                                  @RequestParam(required = false) String keyword) {
        // 防止 SQL 注入 - 限制 keyword 长度
        if (StrUtil.isNotBlank(keyword) && keyword.length() > 100) {
            keyword = keyword.substring(0, 100);
        }
        return R.ok(postService.pagePosts(pageNum, pageSize, categoryId, keyword));
    }

    @Operation(summary = "获取帖子详情")
    @GetMapping("/{id}")
    public R<ThPost> getById(@PathVariable Long id) {
        ThPost post = postService.getById(id);
        if (post == null) {
            return R.fail(404, "帖子不存在");
        }
        postService.incrementViewCount(id);
        return R.ok(post);
    }

    @Operation(summary = "发布帖子")
    @PostMapping
    public R<Long> create(@RequestBody ThPost post, HttpServletRequest request) {
        // 内容校验
        if (StrUtil.isBlank(post.getContent())) {
            return R.fail(400, "内容不能为空");
        }
        if (post.getContent().length() > 5000) {
            return R.fail(400, "内容不能超过5000字");
        }
        post.setIp(getClientIp(request));
        postService.createPost(post);
        return R.ok(post.getId());
    }

    @Operation(summary = "点赞帖子")
    @PostMapping("/{id}/like")
    public R<Void> like(@PathVariable Long id, HttpServletRequest request) {
        postService.likePost(id, getClientIp(request));
        return R.ok();
    }

    @Operation(summary = "取消点赞")
    @DeleteMapping("/{id}/like")
    public R<Void> unlike(@PathVariable Long id, HttpServletRequest request) {
        postService.unlikePost(id, getClientIp(request));
        return R.ok();
    }

    @Operation(summary = "检查是否已点赞")
    @GetMapping("/{id}/liked")
    public R<Boolean> isLiked(@PathVariable Long id, HttpServletRequest request) {
        return R.ok(postService.isLiked(id, getClientIp(request)));
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
