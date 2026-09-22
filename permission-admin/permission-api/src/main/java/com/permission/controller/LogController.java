package com.permission.controller;

import com.permission.common.R;
import com.permission.system.service.SysOperationLogService;
import com.permission.system.service.SysLoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "日志管理")
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class LogController {

    private final SysOperationLogService operationLogService;
    private final SysLoginLogService loginLogService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/operation/page")
    @PreAuthorize("hasAnyAuthority('system:log:list', 'admin')")
    public R<?> operationLogPage(@RequestParam(defaultValue = "1") long pageNum,
                                  @RequestParam(defaultValue = "10") long pageSize,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String module,
                                  @RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate) {
        return R.ok(operationLogService.pageLogs(pageNum, pageSize, keyword, module, startDate, endDate));
    }

    @Operation(summary = "分页查询登录日志")
    @GetMapping("/login/page")
    @PreAuthorize("hasAnyAuthority('system:log:list', 'admin')")
    public R<?> loginLogPage(@RequestParam(defaultValue = "1") long pageNum,
                              @RequestParam(defaultValue = "10") long pageSize,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate) {
        return R.ok(loginLogService.pageLogs(pageNum, pageSize, keyword, startDate, endDate));
    }
}

