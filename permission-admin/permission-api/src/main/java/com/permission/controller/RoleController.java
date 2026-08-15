package com.permission.controller;

import com.permission.common.R;
import com.permission.common.annotation.OperationLog;
import com.permission.common.entity.SysRole;
import com.permission.system.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleService roleService;

    @Operation(summary = "分页查询角色")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('system:role:list', 'admin')")
    public R<?> page(@RequestParam(defaultValue = "1") long pageNum,
                     @RequestParam(defaultValue = "10") long pageSize,
                     @RequestParam(required = false) String keyword) {
        return R.ok(roleService.pageRoles(pageNum, pageSize, keyword));
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:role:query', 'admin')")
    public R<SysRole> getById(@PathVariable Long id) {
        return R.ok(roleService.getRoleById(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:role:add', 'admin')")
    @OperationLog(module = "角色管理", value = "新增角色")
    public R<Void> create(@Valid @RequestBody SysRole role) {
        roleService.createRole(role);
        return R.ok();
    }

    @Operation(summary = "修改角色")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:role:edit', 'admin')")
    @OperationLog(module = "角色管理", value = "修改角色")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SysRole role) {
        role.setId(id);
        roleService.updateRole(role);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAnyAuthority('system:role:delete', 'admin')")
    @OperationLog(module = "角色管理", value = "删除角色")
    public R<Void> delete(@PathVariable List<Long> ids) {
        roleService.deleteRoles(ids);
        return R.ok();
    }

    @Operation(summary = "修改角色状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('system:role:edit', 'admin')")
    @OperationLog(module = "角色管理", value = "修改角色状态")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        roleService.updateStatus(id, body.get("status"));
        return R.ok();
    }

    @Operation(summary = "分配菜单权限")
    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAnyAuthority('system:role:edit', 'admin')")
    @OperationLog(module = "角色管理", value = "分配权限")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody Map<String, Set<Long>> body) {
        roleService.assignMenus(id, body.get("menuIds"));
        return R.ok();
    }

    @Operation(summary = "获取角色菜单ID列表")
    @GetMapping("/{id}/menus")
    public R<Set<Long>> getRoleMenuIds(@PathVariable Long id) {
        return R.ok(roleService.getRoleMenuIds(id));
    }

    @Operation(summary = "获取所有角色（下拉框用）")
    @GetMapping("/all")
    public R<List<SysRole>> getAll() {
        return R.ok(roleService.getAllRoles());
    }
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.controller.RoleController
// 【模块】permission-api
//
// 【使用的注解/技术】
//   - @Tag(name = "角色管理") — Swagger/OpenAPI，API 分组标签
//   - @RestController — Spring MVC，声明 REST 控制器
//   - @RequestMapping("/api/role") — Spring MVC，角色管理基础路由前缀
//   - @RequiredArgsConstructor — Lombok，生成必需参数构造器（构造器注入）
//   - @Operation — Swagger/OpenAPI，描述每个角色接口用途
//   - @GetMapping / @PostMapping / @PutMapping / @DeleteMapping — Spring MVC，RESTful HTTP 方法端点
//   - @PathVariable — Spring MVC，绑定 URL 路径变量（含批量 ID 路径 {ids}）
//   - @RequestParam(required = false) — Spring MVC，绑定可选查询参数
//   - @Valid — Jakarta Bean Validation，触发角色实体验证
//   - @RequestBody — Spring MVC，绑定请求 JSON 体（角色实体或 Map 参数）
//   - @PreAuthorize — Spring Security 方法级权限注解，控制角色写操作权限
//   - @OperationLog — 自定义注解（角色管理模块），写入操作日志
//
// 【关键依赖】
//   - 依赖 SysRoleService → 角色分页/详情/增删改/状态/菜单分配/全量下拉等业务
//   - 依赖 SysRole 实体 → 角色操作参数载体
//   - 依赖 R → 统一响应封装
//   - 依赖 OperationLog 自定义注解 → 被 OperationLogAspect 拦截记录审计日志
//
// 【关联文件】
//   - 被 Spring Security 的 @PreAuthorize 拦截鉴权
//   - 被 OperationLogAspect 切面拦截（带 @OperationLog 的方法会记录日志）
//   - getRoleMenuIds 返回角色的菜单 ID 列表，供前端角色管理时的权限分配 UI 使用
//   - 依赖 com.permission.common.annotation.OperationLog 自定义注解
//
// 【核心作用】
//   角色管理控制器：提供角色分页查询、角色详情、新增、修改、批量级联删除、状态修改、
//   分配菜单权限、查询角色菜单 ID 列表以及全量可用角色下拉等 REST 接口。角色是 RBAC 权限
//   体系的核心枢纽，连接用户与菜单/按钮权限。
//
// 【设计必要性】
//   角色 CRUD 与权限分配操作统一在此层管理；角色关联菜单变更属于安全敏感操作，配合
//   @PreAuthorize + @OperationLog 双保险；独立 assignMenus/getRoleMenuIds 两个接口将角色
//   资源管理与权限分配职责清晰分离。
//
// 【注意事项/安全提示】
//   - 所有写操作（新增/修改/删除/状态/分配菜单）都带 @PreAuthorize + @OperationLog 双保险
//   - delete 接口使用路径变量 {ids} 接收批量删除 ID，调用 Service 级联校验无用户关联才执行删除
//   - updateStatus 通过 @RequestBody Map 接收参数（{status: 0|1}），实现单接口复用
//   - assignMenus 通过 @RequestBody Map 接收参数（{menuIds: [...]}），实现菜单权限的"全量替换式"分配
//   - getRoleMenuIds 与 getAll 接口无 @PreAuthority 保护（前端通用下拉使用，返回的数据不敏感）
// ============================================================
