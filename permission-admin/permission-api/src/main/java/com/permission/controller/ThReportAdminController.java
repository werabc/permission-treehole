package com.permission.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.permission.common.R;
import com.permission.common.entity.ThReport;
import com.permission.common.entity.ThUser;
import com.permission.system.mapper.ThReportMapper;
import com.permission.system.mapper.ThUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "树洞举报管理")
@RestController
@RequestMapping("/api/admin/th/report")
@RequiredArgsConstructor
public class ThReportAdminController {

    private final ThReportMapper reportMapper;
    private final ThUserMapper userMapper;

    @Operation(summary = "举报列表")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<IPage<ThReport>> page(@RequestParam(defaultValue = "1") long pageNum,
                                    @RequestParam(defaultValue = "10") long pageSize,
                                    @RequestParam(required = false) Integer status) {
        Page<ThReport> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ThReport> wrapper = new LambdaQueryWrapper<ThReport>()
                .eq(ThReport::getDeleted, 0)
                .eq(status != null, ThReport::getStatus, status)
                .orderByDesc(ThReport::getCreateTime);
        IPage<ThReport> result = reportMapper.selectPage(page, wrapper);
        for (ThReport report : result.getRecords()) {
            ThUser reporter = userMapper.selectById(report.getReporterId());
            report.setReporterName(reporter != null ? reporter.getNickname() : "未知用户");
        }
        return R.ok(result);
    }

    @Operation(summary = "举报详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        ThReport report = reportMapper.selectById(id);
        if (report == null) return R.fail(404, "举报不存在");

        Map<String, Object> result = new HashMap<>();
        result.put("id", report.getId());
        result.put("targetType", report.getTargetType());
        result.put("targetId", report.getTargetId());
        result.put("reason", report.getReason());
        result.put("description", report.getDescription());
        result.put("evidenceImages", report.getEvidenceImages());
        result.put("status", report.getStatus());
        result.put("handleResult", report.getHandleResult());
        result.put("handleTime", report.getHandleTime());
        result.put("createTime", report.getCreateTime());

        ThUser reporter = userMapper.selectById(report.getReporterId());
        result.put("reporterName", reporter != null ? reporter.getNickname() : "未知用户");

        ThUser handler = userMapper.selectById(report.getHandlerId());
        result.put("handlerName", handler != null ? handler.getNickname() : null);

        return R.ok(result);
    }

    @Operation(summary = "处理举报")
    @PutMapping("/{id}/handle")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> handle(@PathVariable Long id,
                           @RequestParam Integer status,
                           @RequestParam(required = false) String result,
                           @AuthenticationPrincipal com.permission.common.dto.LoginUser loginUser) {
        ThReport report = reportMapper.selectById(id);
        if (report == null) return R.fail(404, "举报不存在");
        if (report.getStatus() != 0) return R.fail(400, "该举报已处理");

        report.setStatus(status);
        report.setHandleResult(result);
        report.setHandlerId(loginUser.getUserId());
        report.setHandleTime(java.time.LocalDateTime.now());
        reportMapper.updateById(report);
        return R.ok();
    }

    @Operation(summary = "批量处理")
    @PostMapping("/batch-handle")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> batchHandle(@RequestBody Map<String, Object> body,
                                @AuthenticationPrincipal com.permission.common.dto.LoginUser loginUser) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) body.get("ids");
        Integer status = (Integer) body.get("status");
        String result = (String) body.get("result");

        if (ids == null || ids.isEmpty()) return R.fail(400, "ID列表不能为空");

        for (Long id : ids) {
            ThReport report = reportMapper.selectById(id);
            if (report != null && report.getStatus() == 0) {
                report.setStatus(status);
                report.setHandleResult(result);
                report.setHandlerId(loginUser.getUserId());
                report.setHandleTime(java.time.LocalDateTime.now());
                reportMapper.updateById(report);
            }
        }
        return R.ok();
    }

    @Operation(summary = "举报统计")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Long>> stats() {
        Map<String, Long> result = new HashMap<>();
        result.put("total", reportMapper.selectCount(new LambdaQueryWrapper<ThReport>().eq(ThReport::getDeleted, 0)));
        result.put("pending", reportMapper.selectCount(new LambdaQueryWrapper<ThReport>().eq(ThReport::getDeleted, 0).eq(ThReport::getStatus, 0)));
        result.put("resolved", reportMapper.selectCount(new LambdaQueryWrapper<ThReport>().eq(ThReport::getDeleted, 0).eq(ThReport::getStatus, 1)));
        result.put("rejected", reportMapper.selectCount(new LambdaQueryWrapper<ThReport>().eq(ThReport::getDeleted, 0).eq(ThReport::getStatus, 2)));
        return R.ok(result);
    }
}
