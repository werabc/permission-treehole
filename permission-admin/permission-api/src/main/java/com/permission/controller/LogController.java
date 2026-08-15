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

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.controller.LogController
// 【模块】permission-api
//
// 【使用的注解/技术】
//   - @Tag(name = "日志管理") — Swagger/OpenAPI，API 分组标签
//   - @RestController — Spring MVC，声明 REST 控制器
//   - @RequestMapping("/api/log") — Spring MVC，日志管理基础路由前缀
//   - @RequiredArgsConstructor — Lombok，生成必需参数构造器（构造器注入）
//   - @Operation — Swagger/OpenAPI，描述每个日志接口用途
//   - @GetMapping — Spring MVC，GET 端点
//   - @RequestParam — Spring MVC，绑定分页/查询参数（含 defaultValue 与 required=false）
//   - @PreAuthorize — Spring Security 方法级权限注解，控制日志读取权限
//
// 【关键依赖】
//   - 依赖 SysOperationLogService → 操作日志分页查询
//   - 依赖 SysLoginLogService → 登录日志分页查询
//   - 依赖 R → 统一响应封装
//
// 【关联文件】
//   - 被 SysOperationLogServiceImpl / SysLoginLogServiceImpl 提供业务逻辑
//   - 被 @PreAuthorize 拦截鉴权，仅 system:log:list 权限或 admin 角色用户可访问
//
// 【核心作用】
//   日志管理控制器：提供操作日志分页查询（可按操作者/动作关键词、模块、时间区间筛选）与
//   登录日志分页查询（可按用户名/IP关键词、时间区间筛选）两个只读接口，供审计人员查看。
//
// 【设计必要性】
//   操作日志与登录日志是系统合规审计的两大核心数据源，集中到一个日志管理 Controller 中
//   提供统一的分页查询接口，便于权限维护与后续扩展（如导出、清理）。
//
// 【注意事项/安全提示】
//   - 两个查询接口均通过 @PreAuthorize 严格控制权限，防止无权限的审计数据泄露
//   - 分页参数通过 @RequestParam 提供默认值（pageNum=1, pageSize=10），避免前端未传分页参数报错
//   - 日志数据属于审计敏感信息，接口只读不可修改；生产环境可通过 @Profile 或权限配置控制访问
//   - 日志记录的实际写入由 OperationLogAspect（操作日志）与 SysUserServiceImpl（登录日志）负责，
//     与查询接口解耦
// ============================================================
