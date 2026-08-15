package com.permission.controller;

import com.permission.common.R;
import com.permission.common.annotation.OperationLog;
import com.permission.common.entity.SysDept;
import com.permission.system.service.SysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "部门管理")
@RestController
@RequestMapping("/api/dept")
@RequiredArgsConstructor
public class DeptController {

    private final SysDeptService deptService;

    @Operation(summary = "获取部门树")
    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('system:dept:list', 'admin')")
    public R<List<SysDept>> tree(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Integer status) {
        return R.ok(deptService.getDeptTree(keyword, status));
    }

    @Operation(summary = "获取部门树（下拉选择用）")
    @GetMapping("/tree-select")
    public R<List<SysDept>> treeSelect() {
        return R.ok(deptService.getDeptTreeSelect());
    }

    @Operation(summary = "获取部门详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:dept:query', 'admin')")
    public R<SysDept> getById(@PathVariable Long id) {
        return R.ok(deptService.getDeptById(id));
    }

    @Operation(summary = "新增部门")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:dept:add', 'admin')")
    @OperationLog(module = "部门管理", value = "新增部门")
    public R<Void> create(@Valid @RequestBody SysDept dept) {
        deptService.createDept(dept);
        return R.ok();
    }

    @Operation(summary = "修改部门")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:dept:edit', 'admin')")
    @OperationLog(module = "部门管理", value = "修改部门")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SysDept dept) {
        dept.setId(id);
        deptService.updateDept(dept);
        return R.ok();
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:dept:delete', 'admin')")
    @OperationLog(module = "部门管理", value = "删除部门")
    public R<Void> delete(@PathVariable Long id) {
        deptService.deleteDept(id);
        return R.ok();
    }
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.controller.DeptController
// 【模块】permission-api
//
// 【使用的注解/技术】
//   - @Tag(name = "部门管理") — Swagger/OpenAPI，API 分组标签
//   - @RestController — Spring MVC，声明 REST 控制器
//   - @RequestMapping("/api/dept") — Spring MVC，部门管理基础路由前缀
//   - @RequiredArgsConstructor — Lombok，生成必需参数构造器（构造器注入）
//   - @Operation — Swagger/OpenAPI，描述每个部门接口用途
//   - @GetMapping / @PostMapping / @PutMapping / @DeleteMapping — Spring MVC，RESTful HTTP 方法端点
//   - @PathVariable — Spring MVC，绑定 URL 路径变量到方法参数
//   - @RequestParam(required = false) — Spring MVC，绑定可选查询参数
//   - @Valid — Jakarta Bean Validation，触发部门实体验证
//   - @RequestBody — Spring MVC，绑定请求 JSON 体到实体参数
//   - @PreAuthorize — Spring Security 方法级权限注解，基于角色码/权限码控制访问
//   - @OperationLog — 自定义注解（部门管理模块），写入操作日志
//
// 【关键依赖】
//   - 依赖 SysDeptService → 部门树查询/详情/增删改/下拉等业务
//   - 依赖 SysDept 实体 → 部门操作参数载体
//   - 依赖 R → 统一响应封装
//   - 依赖 OperationLog 自定义注解 → 被 OperationLogAspect 拦截记录审计日志
//
// 【关联文件】
//   - 被 Spring Security 的 @PreAuthorize 拦截鉴权
//   - 被 OperationLogAspect 切面拦截（带 @OperationLog 的方法会记录日志）
//   - 依赖 com.permission.common.annotation.OperationLog 自定义注解
//
// 【核心作用】
//   部门管理控制器：提供部门树查询、部门树下拉、部门详情、新增、修改、删除等 REST 接口。
//   部门是企业组织架构的核心实体，支撑数据权限维度的过滤。
//
// 【设计必要性】
//   部门 CRUD 统一走 Controller → Service → Mapper 分层，通过 @PreAuthorize 做方法级权限，
//   通过 @OperationLog 做审计日志，前后端解耦。
//
// 【注意事项/安全提示】
//   - 所有写操作（新增/修改/删除）都带 @PreAuthorize 权限校验 + @OperationLog 审计日志，
//     形成权限-审计双保险
//   - tree-select 接口无 @PreAuthority 保护（前端通用下拉使用，返回仅需 status=1 的有效数据）
//   - 部门 ID 通过 @PathVariable 绑定，需注意 ID 合法性由 Service 层校验
// ============================================================
