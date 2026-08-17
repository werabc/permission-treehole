package com.permission.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.permission.common.R;
import com.permission.common.entity.ThUser;
import com.permission.common.entity.ThUserLog;
import com.permission.system.mapper.ThCommentMapper;
import com.permission.system.mapper.ThPostMapper;
import com.permission.system.mapper.ThUserLogMapper;
import com.permission.system.mapper.ThUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "树洞用户管理")
@RestController
@RequestMapping("/api/admin/th/user")
@RequiredArgsConstructor
public class ThUserAdminController {

    private final ThUserMapper userMapper;
    private final ThPostMapper postMapper;
    private final ThCommentMapper commentMapper;
    private final ThUserLogMapper userLogMapper;

    @Operation(summary = "用户列表")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<IPage<ThUser>> page(@RequestParam(defaultValue = "1") long pageNum,
                                  @RequestParam(defaultValue = "10") long pageSize,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Integer status) {
        Page<ThUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ThUser> wrapper = new LambdaQueryWrapper<ThUser>()
                .eq(ThUser::getDeleted, 0)
                .like(keyword != null && !keyword.isEmpty(), ThUser::getUsername, keyword)
                .eq(status != null, ThUser::getStatus, status)
                .orderByDesc(ThUser::getCreateTime);
        IPage<ThUser> result = userMapper.selectPage(page, wrapper);
        // 清除密码字段，防止泄露
        result.getRecords().forEach(u -> u.setPassword(null));
        return R.ok(result);
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        ThUser user = userMapper.selectById(id);
        if (user == null) return R.fail(404, "用户不存在");

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());
        result.put("bio", user.getBio());
        result.put("gender", user.getGender());
        result.put("status", user.getStatus());
        result.put("muteUntil", user.getMuteUntil());
        result.put("banUntil", user.getBanUntil());
        result.put("postCount", user.getPostCount());
        result.put("commentCount", user.getCommentCount());
        result.put("violationCount", user.getViolationCount());
        result.put("lastPostTime", user.getLastPostTime());
        result.put("lastLoginIp", user.getLastLoginIp());
        result.put("createTime", user.getCreateTime());
        // 不返回密码字段

        // 统计
        long postCnt = postMapper.selectCount(new LambdaQueryWrapper<com.permission.common.entity.ThPost>()
                .eq(com.permission.common.entity.ThPost::getUserId, id)
                .eq(com.permission.common.entity.ThPost::getDeleted, 0));
        long commentCnt = commentMapper.selectCount(new LambdaQueryWrapper<com.permission.common.entity.ThComment>()
                .eq(com.permission.common.entity.ThComment::getUserId, id)
                .eq(com.permission.common.entity.ThComment::getDeleted, 0));
        result.put("totalPosts", postCnt);
        result.put("totalComments", commentCnt);

        return R.ok(result);
    }

    @Operation(summary = "禁言用户")
    @PutMapping("/{id}/mute")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> mute(@PathVariable Long id, @RequestParam(required = false) Integer hours) {
        ThUser user = userMapper.selectById(id);
        if (user == null) return R.fail(404, "用户不存在");

        if (hours != null && hours > 0) {
            user.setMuteUntil(LocalDateTime.now().plusHours(hours));
        } else {
            user.setMuteUntil(LocalDateTime.now().plusYears(100)); // 永久禁言
        }
        userMapper.updateById(user);
        return R.ok();
    }

    @Operation(summary = "取消禁言")
    @PutMapping("/{id}/unmute")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> unmute(@PathVariable Long id) {
        ThUser user = userMapper.selectById(id);
        if (user == null) return R.fail(404, "用户不存在");
        user.setMuteUntil(null);
        userMapper.updateById(user);
        return R.ok();
    }

    @Operation(summary = "封号")
    @PutMapping("/{id}/ban")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> ban(@PathVariable Long id, @RequestParam(required = false) Integer days) {
        ThUser user = userMapper.selectById(id);
        if (user == null) return R.fail(404, "用户不存在");

        if (days != null && days > 0) {
            user.setBanUntil(LocalDateTime.now().plusDays(days));
        } else {
            user.setBanUntil(LocalDateTime.now().plusYears(100)); // 永久封号
        }
        user.setStatus(0);
        userMapper.updateById(user);
        return R.ok();
    }

    @Operation(summary = "解封")
    @PutMapping("/{id}/unban")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> unban(@PathVariable Long id) {
        ThUser user = userMapper.selectById(id);
        if (user == null) return R.fail(404, "用户不存在");
        user.setBanUntil(null);
        user.setStatus(1);
        userMapper.updateById(user);
        return R.ok();
    }

    @Operation(summary = "用户行为日志")
    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<IPage<ThUserLog>> logs(@PathVariable Long id,
                                     @RequestParam(defaultValue = "1") long pageNum,
                                     @RequestParam(defaultValue = "20") long pageSize) {
        Page<ThUserLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ThUserLog> wrapper = new LambdaQueryWrapper<ThUserLog>()
                .eq(ThUserLog::getUserId, id)
                .orderByDesc(ThUserLog::getCreateTime);
        return R.ok(userLogMapper.selectPage(page, wrapper));
    }

    @Operation(summary = "用户发帖记录")
    @GetMapping("/{id}/posts")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<IPage<com.permission.common.entity.ThPost>> posts(@PathVariable Long id,
                                                               @RequestParam(defaultValue = "1") long pageNum,
                                                               @RequestParam(defaultValue = "20") long pageSize) {
        Page<com.permission.common.entity.ThPost> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<com.permission.common.entity.ThPost> wrapper = new LambdaQueryWrapper<com.permission.common.entity.ThPost>()
                .eq(com.permission.common.entity.ThPost::getUserId, id)
                .eq(com.permission.common.entity.ThPost::getDeleted, 0)
                .orderByDesc(com.permission.common.entity.ThPost::getCreateTime);
        return R.ok(postMapper.selectPage(page, wrapper));
    }

    @Operation(summary = "用户评论记录")
    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<IPage<com.permission.common.entity.ThComment>> comments(@PathVariable Long id,
                                                                     @RequestParam(defaultValue = "1") long pageNum,
                                                                     @RequestParam(defaultValue = "20") long pageSize) {
        Page<com.permission.common.entity.ThComment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<com.permission.common.entity.ThComment> wrapper = new LambdaQueryWrapper<com.permission.common.entity.ThComment>()
                .eq(com.permission.common.entity.ThComment::getUserId, id)
                .eq(com.permission.common.entity.ThComment::getDeleted, 0)
                .orderByDesc(com.permission.common.entity.ThComment::getCreateTime);
        return R.ok(commentMapper.selectPage(page, wrapper));
    }
}
