package com.permission.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.permission.common.R;
import com.permission.common.annotation.OperationLog;
import com.permission.common.dto.LoginUser;
import com.permission.common.dto.ThProfileDTO;
import com.permission.common.entity.*;
import com.permission.system.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.permission.common.ResultCode;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

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
    private final ThSettingsService settingsService;
    private final com.permission.system.service.ThCollectService collectService;
    private final com.permission.system.mapper.ThAnnouncementMapper announcementMapper;

    /**
     * 从认证主体中提取用户ID，若未登录则抛出401异常
     * Alibaba-Java: NPE防护【推荐→强制】防止 NPE 是程序员的基本修养
     */
    private Long requireUserId(LoginUser loginUser) {
        if (loginUser == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    ResultCode.UNAUTHORIZED.getMessage());
        }
        return loginUser.getUserId();
    }

    // ==================== 分类 ====================

    @Operation(summary = "分类列表")
    @GetMapping("/category/list")
    public R<List<ThCategory>> getCategoryList() {
        return R.ok(categoryService.listEnabled());
    }

    // ==================== 公告 ====================

    @Operation(summary = "树洞公告列表（公开）")
    @GetMapping("/announcements")
    public R<List<ThAnnouncement>> getActiveAnnouncements() {
        return R.ok(announcementMapper.selectActiveAnnouncements());
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
        if (post == null || post.getDeleted() == 1) return R.fail(404, "帖子不存在");
        if (post.getStatus() != 1) return R.fail(404, "帖子不存在");
        postService.incrementViewCount(id);
        // 清除敏感字段，防止信息泄露
        post.setIp(null);
        post.setAuditRemark(null);
        post.setAuditorId(null);
        if (post.getIsAnonymous() != null && post.getIsAnonymous() == 1) {
            post.setUserId(null);
            post.setAuthorName("匿名用户");
        }
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
    @OperationLog(module = "树洞帖子", value = "创建帖子")
    public R<Long> createPost(@RequestBody ThPost post,
                               @AuthenticationPrincipal LoginUser loginUser) {
        // 检查是否允许匿名发帖
        if (post.getIsAnonymous() != null && post.getIsAnonymous() == 1
                && !settingsService.isAnonymousAllowed()) {
            return R.fail(com.permission.common.ResultCode.BAD_REQUEST, "匿名发帖已关闭");
        }
        post.setUserId(requireUserId(loginUser));
        postService.createPost(post);
        return R.ok(post.getId());
    }

    @Operation(summary = "点赞帖子")
    @PostMapping("/post/{id}/like")
    public R<Void> likePost(@PathVariable Long id,
                             @AuthenticationPrincipal LoginUser loginUser) {
        postService.likePost(id, requireUserId(loginUser));
        return R.ok();
    }

    @Operation(summary = "取消点赞帖子")
    @DeleteMapping("/post/{id}/like")
    public R<Void> unlikePost(@PathVariable Long id,
                               @AuthenticationPrincipal LoginUser loginUser) {
        postService.unlikePost(id, requireUserId(loginUser));
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
        comment.setUserId(requireUserId(loginUser));
        commentService.createComment(comment);
        return R.ok(comment.getId());
    }

    @Operation(summary = "点赞评论")
    @PostMapping("/comment/{id}/like")
    public R<Void> likeComment(@PathVariable Long id,
                                @AuthenticationPrincipal LoginUser loginUser) {
        commentService.likeComment(id, requireUserId(loginUser));
        return R.ok();
    }

    // ==================== 举报 ====================

    @Operation(summary = "提交举报")
    @PostMapping("/report")
    public R<Void> submitReport(@RequestBody ThReport report,
                                 @AuthenticationPrincipal LoginUser loginUser) {
        report.setReporterId(requireUserId(loginUser));
        reportService.createReport(report);
        return R.ok();
    }

    @Operation(summary = "删除自己的帖子")
    @DeleteMapping("/post/{id}")
    public R<Void> deletePost(@PathVariable Long id,
                              @AuthenticationPrincipal LoginUser loginUser) {
        postService.deletePost(id, requireUserId(loginUser), false);
        return R.ok();
    }

    @Operation(summary = "删除自己的评论")
    @DeleteMapping("/comment/{id}")
    public R<Void> deleteComment(@PathVariable Long id,
                                 @AuthenticationPrincipal LoginUser loginUser) {
        commentService.deleteComment(id, requireUserId(loginUser), false);
        return R.ok();
    }

    // ==================== 搜索 ====================

    @Operation(summary = "搜索帖子（内容 + 作者昵称）")
    @GetMapping("/search")
    public R<IPage<ThPost>> searchPosts(@RequestParam String keyword,
                                        @RequestParam(defaultValue = "1") long pageNum,
                                        @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(postService.searchPosts(keyword, pageNum, pageSize));
    }

    // ==================== 收藏 ====================

    @Operation(summary = "切换收藏状态")
    @PostMapping("/collect/{postId}")
    public R<Boolean> toggleCollect(@PathVariable Long postId,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(collectService.toggle(requireUserId(loginUser), postId));
    }

    @Operation(summary = "是否已收藏")
    @GetMapping("/post/{id}/collected")
    public R<Boolean> isCollected(@PathVariable Long id,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return R.ok(false);
        return R.ok(collectService.isCollected(loginUser.getUserId(), id));
    }

    @Operation(summary = "我收藏的帖子")
    @GetMapping("/user/collects")
    public R<IPage<ThPost>> getMyCollects(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(postService.getCollectedPosts(requireUserId(loginUser), pageNum, pageSize));
    }

    @Operation(summary = "收藏数量")
    @GetMapping("/user/collect-count")
    public R<Long> getCollectCount(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(collectService.countByUser(requireUserId(loginUser)));
    }

    // ==================== 他人主页 ====================

    @Operation(summary = "用户公开主页（不含隐私字段）")
    @GetMapping("/user/public/{id}")
    public R<Map<String, Object>> getPublicProfile(@PathVariable Long id) {
        return R.ok(userService.getPublicProfile(id));
    }

    @Operation(summary = "指定用户的公开帖子")
    @GetMapping("/user/{id}/posts")
    public R<IPage<ThPost>> getUserPosts(@PathVariable Long id,
                                         @RequestParam(defaultValue = "1") long pageNum,
                                         @RequestParam(defaultValue = "10") long pageSize) {
        IPage<ThPost> page = userService.getPosts(id, pageNum, pageSize);
        // 仅返回已通过审核的帖子，避免待审/下架内容外泄
        page.setRecords(page.getRecords().stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .collect(java.util.stream.Collectors.toList()));
        return R.ok(page);
    }

    // ==================== 个人中心 ====================

    @Operation(summary = "我的帖子")
    @GetMapping("/user/posts")
    public R<IPage<ThPost>> getMyPosts(@RequestParam(defaultValue = "1") long pageNum,
                                        @RequestParam(defaultValue = "10") long pageSize,
                                        @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(userService.getPosts(requireUserId(loginUser), pageNum, pageSize));
    }

    @Operation(summary = "我的评论")
    @GetMapping("/user/my-comments")
    public R<IPage<ThComment>> getMyComments(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(userService.getMyComments(requireUserId(loginUser), pageNum, pageSize));
    }

    @Operation(summary = "收到的评论")
    @GetMapping("/user/received-comments")
    public R<IPage<ThComment>> getReceivedComments(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(userService.getReceivedComments(requireUserId(loginUser), pageNum, pageSize));
    }

    @Operation(summary = "通知列表")
    @GetMapping("/user/notifications")
    public R<IPage<ThNotification>> getNotifications(@RequestParam(defaultValue = "1") long pageNum,
                                                      @RequestParam(defaultValue = "10") long pageSize,
                                                      @RequestParam(required = false) Boolean unreadOnly,
                                                      @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(userService.getNotifications(requireUserId(loginUser), pageNum, pageSize, unreadOnly));
    }

    @Operation(summary = "未读通知数量")
    @GetMapping("/user/unread-count")
    public R<Long> getUnreadCount(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(userService.getUnreadCount(requireUserId(loginUser)));
    }

    @Operation(summary = "标记通知已读")
    @PutMapping("/user/notifications/read")
    public R<Void> markNotificationsRead(@RequestBody Map<String, List<Long>> body,
                                          @AuthenticationPrincipal LoginUser loginUser) {
        List<Long> ids = body.get("ids");
        userService.markNotificationsRead(requireUserId(loginUser), ids);
        return R.ok();
    }

    @Operation(summary = "更新个人资料")
    @PutMapping("/user/profile")
    public R<Void> updateProfile(@RequestBody @Valid ThProfileDTO dto,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        userService.updateProfile(requireUserId(loginUser), dto);
        return R.ok();
    }
}
