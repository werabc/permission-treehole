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

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.controller.MenuController
// 【模块】permission-api
//
// 【使用的注解/技术】
//   - @Tag(name = "菜单管理") — Swagger/OpenAPI，API 分组标签
//   - @RestController — Spring MVC，声明 REST 控制器
//   - @RequestMapping("/api/menu") — Spring MVC，菜单管理基础路由前缀
//   - @RequiredArgsConstructor — Lombok，生成必需参数构造器（构造器注入）
//   - @Operation — Swagger/OpenAPI，描述每个菜单接口用途
//   - @GetMapping / @PostMapping / @PutMapping / @DeleteMapping — Spring MVC，RESTful HTTP 方法端点
//   - @PathVariable — Spring MVC，绑定 URL 路径变量
//   - @RequestParam(required = false) — Spring MVC，绑定可选查询参数
//   - @Valid — Jakarta Bean Validation，触发菜单实体验证
//   - @RequestBody — Spring MVC，绑定请求 JSON 体到实体参数
//   - @PreAuthorize — Spring Security 方法级权限注解，基于角色码/权限码控制访问
//   - @OperationLog — 自定义注解（菜单管理模块），写入操作日志
//   - @AuthenticationPrincipal — Spring Security，注入当前认证主体的 LoginUser
//
// 【关键依赖】
//   - 依赖 SysMenuService → 菜单树查询/详情/增删改/下拉/用户菜单等业务
//   - 依赖 SysMenu 实体 → 菜单操作参数载体
//   - 依赖 LoginUser DTO → 获取当前登录人 ID 用于 userMenus 查询
//   - 依赖 R → 统一响应封装
//   - 依赖 OperationLog 自定义注解 → 被 OperationLogAspect 拦截记录审计日志
//
// 【关联文件】
//   - 被 Spring Security 的 @PreAuthorize 拦截鉴权
//   - 被 OperationLogAspect 切面拦截（带 @OperationLog 的方法会记录日志）
//   - userMenus 接口返回的动态路由菜单供前端路由渲染
//   - 依赖 com.permission.common.annotation.OperationLog 自定义注解
//
// 【核心作用】
//   菜单管理控制器：提供菜单树查询、菜单树下拉、当前用户菜单（动态路由）、菜单详情、新增、
//   修改、删除等 REST 接口。菜单/按钮权限是 RBAC 权限体系的核心资源。
//
// 【设计必要性】
//   菜单 CRUD 统一走 Controller → Service → Mapper 分层，通过 @PreAuthorize 做方法级权限，
//   通过 @OperationLog 做审计日志，前后端解耦；userMenus 接口按当前用户权限动态生成菜单树，
//   实现基于角色的前端路由控制。
//
// 【注意事项/安全提示】
//   - 所有写操作（新增/修改/删除）都带 @PreAuthorize 权限校验 + @OperationLog 审计日志，
//     形成权限-审计双保险
//   - tree-select / userMenus 接口无 @PreAuthorize 保护（前端通用下拉/路由使用，无需敏感写权限）
//   - 菜单类型包括 CATALOG（目录）、MENU（菜单）、BUTTON（按钮），分别用于不同层级与权限控制
//   - userMenus 按当前登录人 @AuthenticationPrincipal 获取 userId，安全可控
// ============================================================
