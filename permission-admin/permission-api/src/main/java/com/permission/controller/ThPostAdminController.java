package com.permission.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.permission.common.R;
import com.permission.common.dto.LoginUser;
import com.permission.common.entity.ThPost;
import com.permission.system.mapper.ThPostMapper;
import com.permission.system.mapper.ThCategoryMapper;
import com.permission.common.entity.ThCategory;
import com.permission.system.mapper.ThUserMapper;
import com.permission.common.entity.ThUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "树洞帖子管理")
@RestController
@RequestMapping("/api/admin/th/post")
@RequiredArgsConstructor
public class ThPostAdminController {

    private final ThPostMapper postMapper;
    private final ThCategoryMapper categoryMapper;
    private final ThUserMapper userMapper;

    @Operation(summary = "帖子列表")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<IPage<ThPost>> page(@RequestParam(defaultValue = "1") long pageNum,
                                  @RequestParam(defaultValue = "10") long pageSize,
                                  @RequestParam(required = false) Integer status,
                                  @RequestParam(required = false) Long categoryId,
                                  @RequestParam(required = false) String keyword) {
        Page<ThPost> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ThPost> wrapper = new LambdaQueryWrapper<ThPost>()
                .eq(ThPost::getDeleted, 0)
                .eq(status != null, ThPost::getStatus, status)
                .eq(categoryId != null, ThPost::getCategoryId, categoryId)
                .like(keyword != null && !keyword.isEmpty(), ThPost::getContent, keyword)
                .orderByDesc(ThPost::getIsTop)
                .orderByDesc(ThPost::getCreateTime);

        IPage<ThPost> result = postMapper.selectPage(page, wrapper);
        for (ThPost post : result.getRecords()) {
            if (post.getIsAnonymous() != null && post.getIsAnonymous() == 1) {
                post.setAuthorName("匿名用户");
            } else {
                ThUser user = userMapper.selectById(post.getUserId());
                post.setAuthorName(user != null ? user.getNickname() : "未知用户");
            }
            if (post.getCategoryId() != null) {
                ThCategory cat = categoryMapper.selectById(post.getCategoryId());
                post.setCategoryName(cat != null ? cat.getName() : "未分类");
            }
            post.setUserId(null); // 隐藏用户ID
        }
        return R.ok(result);
    }

    @Operation(summary = "帖子详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        ThPost post = postMapper.selectById(id);
        if (post == null) return R.fail(404, "帖子不存在");

        Map<String, Object> result = new HashMap<>();
        result.put("id", post.getId());
        result.put("content", post.getContent());
        result.put("images", post.getImages());
        result.put("isAnonymous", post.getIsAnonymous());
        result.put("isTop", post.getIsTop());
        result.put("status", post.getStatus());
        result.put("viewCount", post.getViewCount());
        result.put("likeCount", post.getLikeCount());
        result.put("commentCount", post.getCommentCount());
        result.put("reportCount", post.getReportCount());
        result.put("ip", post.getIp());
        result.put("auditRemark", post.getAuditRemark());
        result.put("auditTime", post.getAuditTime());
        result.put("createTime", post.getCreateTime());

        // 作者信息
        if (post.getIsAnonymous() != null && post.getIsAnonymous() == 1) {
            result.put("authorName", "匿名用户");
        } else {
            ThUser user = userMapper.selectById(post.getUserId());
            result.put("authorName", user != null ? user.getNickname() : "未知用户");
            result.put("authorId", post.getUserId());
        }

        // 分类信息
        if (post.getCategoryId() != null) {
            ThCategory cat = categoryMapper.selectById(post.getCategoryId());
            result.put("categoryName", cat != null ? cat.getName() : "未分类");
        }

        return R.ok(result);
    }

    @Operation(summary = "审核帖子")
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> audit(@PathVariable Long id,
                          @RequestParam Integer status,
                          @RequestParam(required = false) String auditRemark,
                          @AuthenticationPrincipal LoginUser loginUser) {
        ThPost post = postMapper.selectById(id);
        if (post == null) return R.fail(404, "帖子不存在");
        post.setStatus(status);
        post.setAuditRemark(auditRemark);
        post.setAuditorId(loginUser.getUserId());
        post.setAuditTime(LocalDateTime.now());
        postMapper.updateById(post);
        return R.ok();
    }

    @Operation(summary = "置顶/取消置顶")
    @PutMapping("/{id}/pin")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> pin(@PathVariable Long id, @RequestParam Integer isTop) {
        ThPost post = postMapper.selectById(id);
        if (post == null) return R.fail(404, "帖子不存在");
        post.setIsTop(isTop);
        postMapper.updateById(post);
        return R.ok();
    }

    @Operation(summary = "隐藏/恢复")
    @PutMapping("/{id}/hide")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> hide(@PathVariable Long id, @RequestParam Integer status) {
        ThPost post = postMapper.selectById(id);
        if (post == null) return R.fail(404, "帖子不存在");
        post.setStatus(status);
        postMapper.updateById(post);
        return R.ok();
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> delete(@PathVariable Long id) {
        ThPost post = postMapper.selectById(id);
        if (post == null) return R.fail(404, "帖子不存在");
        post.setDeleted(1);
        postMapper.updateById(post);
        return R.ok();
    }
}
