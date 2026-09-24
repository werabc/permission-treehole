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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "树洞举报管理")
@RestController
@RequestMapping("/api/admin/th/report")
@RequiredArgsConstructor
public class ThReportAdminController {

    private final ThReportMapper reportMapper;
    private final ThUserMapper userMapper;
    private final com.permission.system.service.ThReportService reportService;

    @Operation(summary = "举报列表")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('th:report:list', 'admin')")
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
    @PreAuthorize("hasAnyAuthority('th:report:view', 'admin')")
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
    @PreAuthorize("hasAnyAuthority('th:report:handle', 'admin')")
    public R<Void> handle(@PathVariable Long id,
                           @RequestParam Integer status,
                           @RequestParam(required = false) String result,
                           @AuthenticationPrincipal com.permission.common.dto.LoginUser loginUser) {
        // 统一走 Service：举报成立会触发"内容下架 + 违规计分 + 自动处罚 + 通知举报人"闭环
        // （原实现直接 updateById，闭环逻辑形同虚设，此为 E2E 测试发现的缺陷）
        reportService.handleReport(id, status, result,
                loginUser == null ? null : loginUser.getUserId());
        return R.ok();
    }

    @Operation(summary = "批量处理")
    @PostMapping("/batch-handle")
    @PreAuthorize("hasAnyAuthority('th:report:handle', 'admin')")
    public R<Void> batchHandle(@RequestBody Map<String, Object> body,
                                @AuthenticationPrincipal com.permission.common.dto.LoginUser loginUser) {
        // Alibaba-Java: 安全规约【强制】无泛型集合赋值需类型安全检查
        Object idsObj = body.get("ids");
        if (!(idsObj instanceof List)) {
            return R.fail(400, "ID列表格式错误");
        }
        List<Long> ids;
        try {
            ids = ((List<?>) idsObj).stream()
                    .map(o -> o instanceof Number ? ((Number) o).longValue() : Long.parseLong(o.toString()))
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return R.fail(400, "ID列表包含非法值");
        }
        Object statusObj = body.get("status");
        Integer status = statusObj instanceof Number ? ((Number) statusObj).intValue() : null;
        String result = body.get("result") != null ? body.get("result").toString() : null;

        if (ids.isEmpty()) return R.fail(400, "ID列表不能为空");
        if (status == null || (status != 1 && status != 2)) return R.fail(400, "处理状态无效");

        int count = 0;
        for (Long id : ids) {
            ThReport report = reportMapper.selectById(id);
            if (report != null && report.getStatus() == 0) {
                // 同样走 Service，保证每条举报都触发完整闭环（下架/计分/通知）
                reportService.handleReport(id, status, result,
                        loginUser == null ? null : loginUser.getUserId());
                count++;
            }
        }
        log.info("Batch handled {} reports with status={}", count, status);
        return R.ok();
    }

    @Operation(summary = "举报统计")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('th:report:list', 'admin')")
    public R<Map<String, Long>> stats() {
        Map<String, Long> result = new HashMap<>();
        result.put("total", reportMapper.selectCount(new LambdaQueryWrapper<ThReport>().eq(ThReport::getDeleted, 0)));
        result.put("pending", reportMapper.selectCount(new LambdaQueryWrapper<ThReport>().eq(ThReport::getDeleted, 0).eq(ThReport::getStatus, 0)));
        result.put("resolved", reportMapper.selectCount(new LambdaQueryWrapper<ThReport>().eq(ThReport::getDeleted, 0).eq(ThReport::getStatus, 1)));
        result.put("rejected", reportMapper.selectCount(new LambdaQueryWrapper<ThReport>().eq(ThReport::getDeleted, 0).eq(ThReport::getStatus, 2)));
        return R.ok(result);
    }
}
