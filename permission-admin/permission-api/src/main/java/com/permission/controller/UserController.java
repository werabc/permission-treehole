package com.permission.controller;

import com.permission.common.R;
import com.permission.common.ResultCode;
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
import java.util.stream.Collectors;

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
        // Alibaba-Java: 安全规约【强制】无泛型集合赋值需类型安全检查
        Object idsObj = body.get("ids");
        if (!(idsObj instanceof List)) {
            return R.fail(ResultCode.BAD_REQUEST.getCode(), "ID列表格式错误");
        }
        List<Long> ids;
        try {
            ids = ((List<?>) idsObj).stream()
                    .map(o -> o instanceof Number ? ((Number) o).longValue() : Long.parseLong(o.toString()))
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return R.fail(ResultCode.BAD_REQUEST.getCode(), "ID列表包含非法值");
        }
        Object statusObj = body.get("status");
        Integer status = statusObj instanceof Number ? ((Number) statusObj).intValue() : null;
        userService.batchUpdateStatus(ids, status);
        return R.ok();
    }
}

