package com.permission.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.permission.common.R;
import com.permission.common.entity.ThAnnouncement;
import com.permission.common.entity.ThUser;
import com.permission.system.mapper.ThAnnouncementMapper;
import com.permission.system.mapper.ThUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "树洞公告管理")
@RestController
@RequestMapping("/api/admin/th/announcement")
@RequiredArgsConstructor
public class ThAnnouncementController {

    private final ThAnnouncementMapper announcementMapper;
    private final ThUserMapper userMapper;

    @Operation(summary = "公告列表")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<IPage<ThAnnouncement>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) String type) {
        Page<ThAnnouncement> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ThAnnouncement> wrapper = new LambdaQueryWrapper<ThAnnouncement>()
                .eq(ThAnnouncement::getDeleted, 0)
                .eq(type != null && !type.isEmpty(), ThAnnouncement::getType, type)
                .orderByDesc(ThAnnouncement::getCreateTime);
        IPage<ThAnnouncement> result = announcementMapper.selectPage(page, wrapper);
        for (ThAnnouncement ann : result.getRecords()) {
            ThUser creator = userMapper.selectById(ann.getCreatorId());
            ann.setCreatorName(creator != null ? creator.getNickname() : "未知");
        }
        return R.ok(result);
    }

    @Operation(summary = "创建公告")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Long> create(@RequestBody ThAnnouncement announcement,
                           @AuthenticationPrincipal com.permission.common.dto.LoginUser loginUser) {
        announcement.setCreatorId(loginUser.getUserId());
        announcement.setCreateTime(java.time.LocalDateTime.now());
        announcementMapper.insert(announcement);
        return R.ok(announcement.getId());
    }

    @Operation(summary = "编辑公告")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> update(@PathVariable Long id, @RequestBody ThAnnouncement announcement) {
        ThAnnouncement existing = announcementMapper.selectById(id);
        if (existing == null) return R.fail(404, "公告不存在");
        existing.setTitle(announcement.getTitle());
        existing.setContent(announcement.getContent());
        existing.setType(announcement.getType());
        existing.setStatus(announcement.getStatus());
        existing.setPublishTime(announcement.getPublishTime());
        existing.setExpireTime(announcement.getExpireTime());
        announcementMapper.updateById(existing);
        return R.ok();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> delete(@PathVariable Long id) {
        ThAnnouncement announcement = announcementMapper.selectById(id);
        if (announcement == null) return R.fail(404, "公告不存在");
        announcement.setDeleted(1);
        announcementMapper.updateById(announcement);
        return R.ok();
    }
}
