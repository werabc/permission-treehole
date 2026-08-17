package com.permission.controller;

import com.permission.common.R;
import com.permission.system.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "仪表盘")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "综合仪表盘数据")
    @GetMapping("/overview")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Object>> overview() {
        return R.ok(dashboardService.getDashboardOverview());
    }

    @Operation(summary = "管理员系统统计")
    @GetMapping("/admin-stats")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Object>> adminStats() {
        return R.ok(dashboardService.getAdminStats());
    }

    @Operation(summary = "树洞系统统计")
    @GetMapping("/treehole-stats")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Object>> treeholeStats() {
        return R.ok(dashboardService.getTreeholeStats());
    }

    @Operation(summary = "待处理事项")
    @GetMapping("/pending")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Object>> pending() {
        return R.ok(dashboardService.getPendingItems());
    }

    @Operation(summary = "趋势数据")
    @GetMapping("/trends")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Object>> trends(@RequestParam(defaultValue = "7") int days) {
        return R.ok(dashboardService.getTrends(days));
    }

    @Operation(summary = "实时统计")
    @GetMapping("/realtime")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Object>> realtime() {
        return R.ok(dashboardService.getRealtimeStats());
    }

    @Operation(summary = "管理员系统统计(兼容旧接口)")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Map<String, Object>> statistics() {
        return R.ok(dashboardService.getStatistics());
    }
}
