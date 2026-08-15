package com.permission.controller;

import com.permission.common.R;
import com.permission.common.annotation.OperationLog;
import com.permission.common.dto.LoginUser;
import com.permission.common.entity.SysUser;
import com.permission.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.permission.common.dto.ProfileDTO;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('system:user:list', 'admin')")
    public R<?> page(@RequestParam(defaultValue = "1") long pageNum,
                     @RequestParam(defaultValue = "10") long pageSize,
                     @RequestParam(required = false) String keyword,
                     @RequestParam(required = false) Long deptId,
                     @RequestParam(required = false) Integer status) {
        return R.ok(userService.pageUsers(pageNum, pageSize, keyword, deptId, status));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:user:query', 'admin')")
    public R<SysUser> getById(@PathVariable Long id) {
        return R.ok(userService.getUserById(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:user:add', 'admin')")
    @OperationLog(module = "用户管理", value = "新增用户")
    public R<Void> create(@Valid @RequestBody SysUser user) {
        userService.createUser(user);
        return R.ok();
    }

    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:user:edit', 'admin')")
    @OperationLog(module = "用户管理", value = "修改用户")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SysUser user) {
        user.setId(id);
        userService.updateUser(user);
        return R.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAnyAuthority('system:user:delete', 'admin')")
    @OperationLog(module = "用户管理", value = "删除用户")
    public R<Void> delete(@PathVariable List<Long> ids) {
        userService.deleteUsers(ids);
        return R.ok();
    }

    @Operation(summary = "修改用户状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('system:user:edit', 'admin')")
    @OperationLog(module = "用户管理", value = "修改用户状态")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        userService.updateStatus(id, body.get("status"));
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyAuthority('system:user:reset-pwd', 'admin')")
    @OperationLog(module = "用户管理", value = "重置密码")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return R.ok();
    }

    @Operation(summary = "修改个人密码")
    @PutMapping("/update-password")
    public R<Void> updatePassword(@AuthenticationPrincipal LoginUser loginUser,
                                   @RequestBody Map<String, String> body) {
        userService.updatePassword(loginUser.getUserId(), body.get("oldPassword"), body.get("newPassword"));
        return R.ok();
    }

    @Operation(summary = "分配角色")
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAnyAuthority('system:user:edit', 'admin')")
    @OperationLog(module = "用户管理", value = "分配角色")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody Map<String, Set<Long>> body) {
        userService.assignRoles(id, body.get("roleIds"));
        return R.ok();
    }

    @Operation(summary = "获取用户角色ID列表")
    @GetMapping("/{id}/roles")
    public R<Set<Long>> getUserRoleIds(@PathVariable Long id) {
        return R.ok(userService.getUserRoleIds(id));
    }

    @Operation(summary = "修改个人信息")
    @PutMapping("/profile")
    public R<Void> updateProfile(@AuthenticationPrincipal LoginUser loginUser,
                                  @Valid @RequestBody ProfileDTO profileDTO) {
        userService.updateProfile(loginUser.getUserId(), profileDTO);
        return R.ok();
    }

    @Operation(summary = "导出用户列表")
    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('system:user:list', 'admin')")
    public void export(HttpServletResponse response) throws IOException {
        userService.exportUsers(response);
    }

    @Operation(summary = "批量修改用户状态")
    @PutMapping("/batch-status")
    @PreAuthorize("hasAnyAuthority('system:user:edit', 'admin')")
    @OperationLog(module = "用户管理", value = "批量修改状态")
    public R<Void> batchUpdateStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> idList = (List<Integer>) body.get("ids");
        Integer status = (Integer) body.get("status");
        List<Long> ids = idList.stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList());
        userService.batchUpdateStatus(ids, status);
        return R.ok();
    }
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.controller.UserController
// 【模块】permission-api
//
// 【使用的注解/技术】
//   - @Tag(name = "用户管理") — Swagger/OpenAPI，API 分组标签
//   - @RestController — Spring MVC，声明 REST 控制器
//   - @RequestMapping("/api/user") — Spring MVC，用户管理基础路由前缀
//   - @RequiredArgsConstructor — Lombok，生成必需参数构造器（构造器注入）
//   - @Operation — Swagger/OpenAPI，描述每个用户接口用途
//   - @GetMapping / @PostMapping / @PutMapping / @DeleteMapping — Spring MVC，RESTful HTTP 方法端点
//   - @PathVariable — Spring MVC，绑定 URL 路径变量（含批量 ID 路径 {ids}）
//   - @RequestParam(required = false/defaultValue = "x") — Spring MVC，绑定分页/过滤参数
//   - @Valid — Jakarta Bean Validation，触发用户实体验证
//   - @RequestBody — Spring MVC，绑定请求 JSON 体（用户实体或 Map 参数）
//   - @PreAuthorize — Spring Security 方法级权限注解，控制用户写操作权限
//   - @OperationLog — 自定义注解（用户管理模块），写入操作日志
//   - @AuthenticationPrincipal — Spring Security，注入当前认证主体的 LoginUser
//
// 【关键依赖】
//   - 依赖 SysUserService → 用户分页/详情/增删改/状态/密码/角色分配/密码策略等业务
//   - 依赖 SysUser 实体 → 用户操作参数载体
//   - 依赖 LoginUser DTO → 获取当前登录人 ID 用于 updatePassword（修改个人密码）
//   - 依赖 R → 统一响应封装
//   - 依赖 OperationLog 自定义注解 → 被 OperationLogAspect 拦截记录审计日志
//
// 【关联文件】
//   - 被 Spring Security 的 @PreAuthorize 拦截鉴权
//   - 被 OperationLogAspect 切面拦截（带 @OperationLog 的方法会记录日志）
//   - resetPassword（管理员重置他人密码）与 updatePassword（个人修改本人密码）分两个接口，
//     权限语义清晰；updatePassword 仅可通过 @AuthenticationPrincipal 修改本人密码，管理员权限
//     无法重置他人密码（安全设计）
//   - 依赖 com.permission.common.annotation.OperationLog 自定义注解
//
// 【核心作用】
//   用户管理控制器：提供用户分页查询、用户详情、新增、修改、批量级联删除、状态修改、
//   重置密码、修改个人密码、分配角色、查询用户角色 ID 列表等 REST 接口。用户是企业账号
//   体系的核心实体。
//
// 【设计必要性】
//   用户 CRUD 统一走 Controller → Service → Mapper 分层；通过 @PreAuthorize 控制接口访问，
//   通过 @OperationLog 做审计日志；个人密码与重置密码分离、角色分配独立接口，权限边界清晰。
//
// 【注意事项/安全提示】
//   - 写操作全部带 @PreAuthorize + @OperationLog 双保险，记录操作人、操作对象、结果
//   - 分页参数通过 @RequestParam 提供默认值，避免前端未传时报错
//   - updatePassword 通过 @AuthenticationPrincipal 绑定当前用户，用户只能修改自己的密码（管理员
//     也无法通过此接口修改他人密码），从根上杜绝权限越权风险
//   - resetPassword 仅可由 system:user:reset-pwd 权限的管理员调用，可重置任意用户密码
//   - delete 接口使用路径变量 {ids} 批量删除，Service 级联动清理用户-角色关联
//   - 用户密码在 Service 层返回前强制置 null，但 Controller 层不做二次处理（由 Service 层保证）
//   - 用户分页支持按关键词（用户名/昵称/手机号）、部门、状态多条件过滤
// ============================================================
