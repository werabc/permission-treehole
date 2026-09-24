package com.permission.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.permission.common.R;
import com.permission.common.dto.LoginUser;
import com.permission.common.entity.ThUser;
import com.permission.common.entity.ThUserLog;
import com.permission.system.mapper.ThCommentMapper;
import com.permission.system.mapper.ThPostMapper;
import com.permission.system.mapper.ThUserLogMapper;
import com.permission.system.mapper.ThUserMapper;
import com.permission.system.support.ThUserGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    private final ThUserGuard userGuard;

    @Operation(summary = "用户列表")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('th:user:list', 'admin')")
    public R<IPage<ThUser>> page(@RequestParam(defaultValue = "1") long pageNum,
                                  @RequestParam(defaultValue = "10") long pageSize,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Integer status) {
        Page<ThUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ThUser> wrapper = new LambdaQueryWrapper<ThUser>()
                .eq(ThUser::getDeleted, 0)
                .eq(status != null, ThUser::getStatus, status)
                .orderByDesc(ThUser::getCreateTime);
        // 转义LIKE通配符，防止通配符注入
        if (StrUtil.isNotBlank(keyword)) {
            String safeKeyword = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            wrapper.and(w -> w.like(ThUser::getUsername, safeKeyword)
                    .or().like(ThUser::getNickname, safeKeyword));
        }
        IPage<ThUser> result = userMapper.selectPage(page, wrapper);
        // 清除密码字段，防止泄露
        result.getRecords().forEach(u -> u.setPassword(null));
        return R.ok(result);
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('th:user:view', 'admin')")
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
    @PreAuthorize("hasAnyAuthority('th:user:mute', 'admin')")
    public R<Void> mute(@PathVariable Long id,
                        @RequestParam(required = false) Integer hours,
                        @RequestParam(required = false) Integer days,
                        @RequestParam(required = false) String reason,
                        @AuthenticationPrincipal LoginUser operator) {
        // 兼容旧调用（hours）：1 天 = 24 小时
        int parsedDays = days != null ? days : (hours != null ? Math.max(hours / 24, 1) : 0);
        userGuard.mute(id, parsedDays <= 0 ? 36500 : parsedDays,
                StrUtil.blankToDefault(reason, "管理员操作"), operatorId(operator));
        return R.ok();
    }

    @Operation(summary = "取消禁言")
    @PutMapping("/{id}/unmute")
    @PreAuthorize("hasAnyAuthority('th:user:mute', 'admin')")
    public R<Void> unmute(@PathVariable Long id, @AuthenticationPrincipal LoginUser operator) {
        userGuard.release(id, false, operatorId(operator));
        return R.ok();
    }

    @Operation(summary = "封号")
    @PutMapping("/{id}/ban")
    @PreAuthorize("hasAnyAuthority('th:user:ban', 'admin')")
    public R<Void> ban(@PathVariable Long id,
                       @RequestParam(required = false) Integer days,
                       @RequestParam(required = false) String reason,
                       @AuthenticationPrincipal LoginUser operator) {
        // days<=0 视为永久封号（内部会置 status=0）
        userGuard.ban(id, days == null ? 0 : days,
                StrUtil.blankToDefault(reason, "管理员操作"), operatorId(operator));
        return R.ok();
    }

    @Operation(summary = "解封")
    @PutMapping("/{id}/unban")
    @PreAuthorize("hasAnyAuthority('th:user:ban', 'admin')")
    public R<Void> unban(@PathVariable Long id, @AuthenticationPrincipal LoginUser operator) {
        userGuard.release(id, false, operatorId(operator));
        return R.ok();
    }

    @Operation(summary = "解除全部处罚（可选清零违规分）")
    @PutMapping("/{id}/release")
    @PreAuthorize("hasAnyAuthority('th:user:release', 'admin')")
    public R<Void> release(@PathVariable Long id,
                           @RequestParam(defaultValue = "false") boolean resetViolation,
                           @AuthenticationPrincipal LoginUser operator) {
        userGuard.release(id, resetViolation, operatorId(operator));
        return R.ok();
    }

    @Operation(summary = "手动加减违规分")
    @PutMapping("/{id}/violation")
    @PreAuthorize("hasAnyAuthority('th:user:violation', 'admin')")
    public R<Integer> addViolation(@PathVariable Long id,
                                   @RequestParam Integer score,
                                   @RequestParam(required = false) String reason) {
        int total = userGuard.addViolation(id, score,
                StrUtil.blankToDefault(reason, "管理员手动调整"));
        return R.ok(total);
    }

    private Long operatorId(LoginUser operator) {
        return operator == null ? null : operator.getUserId();
    }

    @Operation(summary = "用户行为日志")
    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAnyAuthority('th:user:view', 'admin')")
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
    @PreAuthorize("hasAnyAuthority('th:user:view', 'admin')")
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
    @PreAuthorize("hasAnyAuthority('th:user:view', 'admin')")
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
