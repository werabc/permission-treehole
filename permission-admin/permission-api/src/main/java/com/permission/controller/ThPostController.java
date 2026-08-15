package com.permission.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.permission.common.R;
import com.permission.common.entity.ThPost;
import com.permission.system.service.ThPostService;
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

    @Operation(summary = "分页查询帖子 (公开)")
    @GetMapping("/page")
    public R<IPage<ThPost>> page(@RequestParam(defaultValue = "1") long pageNum,
                                  @RequestParam(defaultValue = "10") long pageSize,
                                  @RequestParam(required = false) Long categoryId,
                                  @RequestParam(required = false) String keyword) {
        return R.ok(postService.pagePosts(pageNum, pageSize, categoryId, keyword, 1));
    }

    @Operation(summary = "获取帖子详情 (公开)")
    @GetMapping("/{id}")
    public R<ThPost> getById(@PathVariable Long id) {
        ThPost post = postService.getById(id);
        if (post == null) return R.fail(404, "帖子不存在");
        postService.incrementViewCount(id);
        return R.ok(post);
    }

    @Operation(summary = "发布帖子 (需登录)")
    @PostMapping
    public R<Long> create(@RequestBody ThPost post, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return R.fail(401, "请先登录");
        post.setUserId(userId);
        postService.createPost(post);
        return R.ok(post.getId());
    }

    @Operation(summary = "点赞帖子 (需登录)")
    @PostMapping("/{id}/like")
    public R<Void> like(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return R.fail(401, "请先登录");
        postService.likePost(id, userId);
        return R.ok();
    }

    @Operation(summary = "取消点赞 (需登录)")
    @DeleteMapping("/{id}/like")
    public R<Void> unlike(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return R.fail(401, "请先登录");
        postService.unlikePost(id, userId);
        return R.ok();
    }

    @Operation(summary = "检查是否已点赞 (需登录)")
    @GetMapping("/{id}/liked")
    public R<Boolean> isLiked(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return R.ok(false);
        return R.ok(postService.isLiked(id, userId));
    }

    private Long getUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StrUtil.isBlank(header) || !header.startsWith("Bearer ")) return null;
        String token = header.substring(7);
        try {
            String payload = token.split("\\.")[1];
            String json = new String(java.util.Base64.getUrlDecoder().decode(payload));
            return cn.hutool.json.JSONUtil.parseObj(json).getLong("sub");
        } catch (Exception e) {
            return null;
        }
    }
}
