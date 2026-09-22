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

