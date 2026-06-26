package com.permission.controller;

import com.permission.common.R;
import com.permission.common.annotation.OperationLog;
import com.permission.common.dto.LoginUser;
import com.permission.common.entity.SysMenu;
import com.permission.system.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final SysMenuService menuService;

    @Operation(summary = "获取菜单树")
    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('system:menu:list', 'admin')")
    public R<List<SysMenu>> tree(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Integer status) {
        return R.ok(menuService.getMenuTree(keyword, status));
    }

    @Operation(summary = "获取菜单树（下拉选择用）")
    @GetMapping("/tree-select")
    public R<List<SysMenu>> treeSelect() {
        return R.ok(menuService.getMenuTreeSelect());
    }

    @Operation(summary = "获取用户菜单（动态路由）")
    @GetMapping("/user-menus")
    public R<List<SysMenu>> userMenus(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(menuService.getUserMenus(loginUser.getUserId()));
    }

    @Operation(summary = "获取菜单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:menu:query', 'admin')")
    public R<SysMenu> getById(@PathVariable Long id) {
        return R.ok(menuService.getMenuById(id));
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:menu:add', 'admin')")
    @OperationLog(module = "菜单管理", value = "新增菜单")
    public R<Void> create(@Valid @RequestBody SysMenu menu) {
        menuService.createMenu(menu);
        return R.ok();
    }

    @Operation(summary = "修改菜单")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:menu:edit', 'admin')")
    @OperationLog(module = "菜单管理", value = "修改菜单")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SysMenu menu) {
        menu.setId(id);
        menuService.updateMenu(menu);
        return R.ok();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:menu:delete', 'admin')")
    @OperationLog(module = "菜单管理", value = "删除菜单")
    public R<Void> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return R.ok();
    }
}
