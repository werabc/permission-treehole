package com.permission.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.permission.common.R;
import com.permission.common.dto.LoginUser;
import com.permission.common.entity.*;
import com.permission.system.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 树洞公共接口 - 供树洞用户端调用
 */
@Tag(name = "树洞公共接口")
@RestController
@RequestMapping("/api/th")
@RequiredArgsConstructor
public class ThPublicController {

    private final ThPostService postService;
    private final ThCommentService commentService;
    private final ThReportService reportService;
    private final ThUserService userService;
    private final ThCategoryService categoryService;

    // ==================== 分类 ====================

    @Operation(summary = "分类列表")
    @GetMapping("/category/list")
    public R<List<ThCategory>> getCategoryList() {
        return R.ok(categoryService.listEnabled());
    }

    // ==================== 帖子 ====================

    @Operation(summary = "帖子列表")
    @GetMapping("/post/page")
    public R<IPage<ThPost>> getPostPage(@RequestParam(defaultValue = "1") long pageNum,
                                         @RequestParam(defaultValue = "10") long pageSize,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) String keyword) {
        return R.ok(postService.pagePosts(pageNum, pageSize, categoryId, keyword, 1));
    }

    @Operation(summary = "帖子详情")
    @GetMapping("/post/{id}")
    public R<ThPost> getPostDetail(@PathVariable Long id,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        ThPost post = postService.getById(id);
        if (post == null) return R.fail(404, "帖子不存在");
        postService.incrementViewCount(id);
        return R.ok(post);
    }

    @Operation(summary = "检查是否点赞")
    @GetMapping("/post/{id}/liked")
    public R<Boolean> isPostLiked(@PathVariable Long id,
                                   @AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return R.ok(false);
        return R.ok(postService.isLiked(id, loginUser.getUserId()));
    }

    @Operation(summary = "创建帖子")
    @PostMapping("/post")
    public R<Long> createPost(@RequestBody ThPost post,
                               @AuthenticationPrincipal LoginUser loginUser) {
        post.setUserId(loginUser.getUserId());
        postService.createPost(post);
        return R.ok(post.getId());
    }

    @Operation(summary = "点赞帖子")
    @PostMapping("/post/{id}/like")
    public R<Void> likePost(@PathVariable Long id,
                             @AuthenticationPrincipal LoginUser loginUser) {
        postService.likePost(id, loginUser.getUserId());
        return R.ok();
    }

    @Operation(summary = "取消点赞帖子")
    @DeleteMapping("/post/{id}/like")
    public R<Void> unlikePost(@PathVariable Long id,
                               @AuthenticationPrincipal LoginUser loginUser) {
        postService.unlikePost(id, loginUser.getUserId());
        return R.ok();
    }

    // ==================== 评论 ====================

    @Operation(summary = "评论列表")
    @GetMapping("/comment/page")
    public R<IPage<ThComment>> getCommentPage(@RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize,
                                               @RequestParam Long postId) {
        return R.ok(commentService.pageComments(pageNum, pageSize, postId));
    }

    @Operation(summary = "创建评论")
    @PostMapping("/comment")
    public R<Long> createComment(@RequestBody ThComment comment,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        comment.setUserId(loginUser.getUserId());
        commentService.createComment(comment);
        return R.ok(comment.getId());
    }

    @Operation(summary = "点赞评论")
    @PostMapping("/comment/{id}/like")
    public R<Void> likeComment(@PathVariable Long id,
                                @AuthenticationPrincipal LoginUser loginUser) {
        commentService.likeComment(id, loginUser.getUserId());
        return R.ok();
    }

    // ==================== 举报 ====================

    @Operation(summary = "提交举报")
    @PostMapping("/report")
    public R<Void> submitReport(@RequestBody ThReport report,
                                 @AuthenticationPrincipal LoginUser loginUser) {
        report.setReporterId(loginUser.getUserId());
        reportService.createReport(report);
        return R.ok();
    }

    // ==================== 个人中心 ====================

    @Operation(summary = "我的帖子")
    @GetMapping("/user/posts")
    public R<IPage<ThPost>> getMyPosts(@RequestParam(defaultValue = "1") long pageNum,
                                        @RequestParam(defaultValue = "10") long pageSize,
                                        @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(userService.getPosts(loginUser.getUserId(), pageNum, pageSize));
    }

    @Operation(summary = "我的评论")
    @GetMapping("/user/my-comments")
    public R<IPage<ThComment>> getMyComments(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(userService.getMyComments(loginUser.getUserId(), pageNum, pageSize));
    }

    @Operation(summary = "收到的评论")
    @GetMapping("/user/received-comments")
    public R<IPage<ThComment>> getReceivedComments(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(userService.getReceivedComments(loginUser.getUserId(), pageNum, pageSize));
    }

    @Operation(summary = "通知列表")
    @GetMapping("/user/notifications")
    public R<IPage<ThNotification>> getNotifications(@RequestParam(defaultValue = "1") long pageNum,
                                                      @RequestParam(defaultValue = "10") long pageSize,
                                                      @RequestParam(required = false) Boolean unreadOnly,
                                                      @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(userService.getNotifications(loginUser.getUserId(), pageNum, pageSize, unreadOnly));
    }

    @Operation(summary = "未读通知数量")
    @GetMapping("/user/unread-count")
    public R<Long> getUnreadCount(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(userService.getUnreadCount(loginUser.getUserId()));
    }

    @Operation(summary = "标记通知已读")
    @PutMapping("/user/notifications/read")
    public R<Void> markNotificationsRead(@RequestBody Map<String, List<Long>> body,
                                          @AuthenticationPrincipal LoginUser loginUser) {
        List<Long> ids = body.get("ids");
        userService.markNotificationsRead(loginUser.getUserId(), ids);
        return R.ok();
    }

    @Operation(summary = "更新个人资料")
    @PutMapping("/user/profile")
    public R<Void> updateProfile(@RequestBody ThUser user,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        user.setId(loginUser.getUserId());
        userService.updateProfile(user);
        return R.ok();
    }
}
