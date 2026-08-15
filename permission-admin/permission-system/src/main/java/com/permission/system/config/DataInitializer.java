package com.permission.system.config;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.permission.common.entity.*;
import com.permission.system.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userMapper.selectCount(new LambdaQueryWrapper<>()) > 0) {
            log.info("数据已初始化，跳过");
            return;
        }

        log.info("========== 开始初始化系统数据 ==========");

        // 1. 初始化部门
        SysDept d1 = createDept("总公司", 0L, "0", 1, "CEO");
        SysDept d2 = createDept("技术部", d1.getId(), "0," + d1.getId(), 1, "CTO");
        SysDept d3 = createDept("产品部", d1.getId(), "0," + d1.getId(), 2, "CPO");
        SysDept d4 = createDept("运营部", d1.getId(), "0," + d1.getId(), 3, "COO");
        createDept("前端组", d2.getId(), "0," + d1.getId() + "," + d2.getId(), 1, "前端Leader");
        createDept("后端组", d2.getId(), "0," + d1.getId() + "," + d2.getId(), 2, "后端Leader");
        log.info("部门数据初始化完成");

        // 2. 初始化用户 (密码: Admin@1234)
        SysUser admin = createUser("admin", "Admin@1234", "超级管理员", "admin@example.com", d1.getId());
        SysUser tech = createUser("tech", "Admin@1234", "技术负责人", "tech@example.com", d2.getId());
        createUser("backend", "Admin@1234", "后端开发", "backend@example.com", d2.getId());
        log.info("用户数据初始化完成");

        // 3. 初始化角色
        SysRole adminRole = createRole("超级管理员", "admin", "拥有系统所有权限", 1);
        SysRole techRole = createRole("技术负责人", "tech_lead", "技术部门管理权限", 2);
        SysRole userRole = createRole("普通用户", "user", "基本查看权限", 5);
        log.info("角色数据初始化完成");

        // 4. 初始化菜单
        Long menuSys = createMenu(0L, "系统管理", "CATALOG", "/system", null, "Setting", null, 1);
        Long menuUser = createMenu(menuSys, "用户管理", "MENU", "/system/user", null, "User", "system:user:list", 1);
        createMenu(menuUser, "用户查询", "BUTTON", null, null, null, "system:user:query", 1);
        createMenu(menuUser, "用户新增", "BUTTON", null, null, null, "system:user:add", 2);
        createMenu(menuUser, "用户编辑", "BUTTON", null, null, null, "system:user:edit", 3);
        createMenu(menuUser, "用户删除", "BUTTON", null, null, null, "system:user:delete", 4);
        createMenu(menuUser, "重置密码", "BUTTON", null, null, null, "system:user:reset-pwd", 5);

        Long menuRole = createMenu(menuSys, "角色管理", "MENU", "/system/role", null, "UserFilled", "system:role:list", 2);
        createMenu(menuRole, "角色查询", "BUTTON", null, null, null, "system:role:query", 1);
        createMenu(menuRole, "角色新增", "BUTTON", null, null, null, "system:role:add", 2);
        createMenu(menuRole, "角色编辑", "BUTTON", null, null, null, "system:role:edit", 3);
        createMenu(menuRole, "角色删除", "BUTTON", null, null, null, "system:role:delete", 4);

        Long menuMenu = createMenu(menuSys, "菜单管理", "MENU", "/system/menu", null, "Menu", "system:menu:list", 3);
        createMenu(menuMenu, "菜单查询", "BUTTON", null, null, null, "system:menu:query", 1);
        createMenu(menuMenu, "菜单新增", "BUTTON", null, null, null, "system:menu:add", 2);
        createMenu(menuMenu, "菜单编辑", "BUTTON", null, null, null, "system:menu:edit", 3);
        createMenu(menuMenu, "菜单删除", "BUTTON", null, null, null, "system:menu:delete", 4);

        Long menuDept = createMenu(menuSys, "部门管理", "MENU", "/system/dept", null, "OfficeBuilding", "system:dept:list", 4);
        createMenu(menuDept, "部门查询", "BUTTON", null, null, null, "system:dept:query", 1);
        createMenu(menuDept, "部门新增", "BUTTON", null, null, null, "system:dept:add", 2);
        createMenu(menuDept, "部门编辑", "BUTTON", null, null, null, "system:dept:edit", 3);
        createMenu(menuDept, "部门删除", "BUTTON", null, null, null, "system:dept:delete", 4);

        Long menuLog = createMenu(menuSys, "日志管理", "CATALOG", "/log", null, "Document", null, 5);
        createMenu(menuLog, "操作日志", "MENU", "/log/operation", null, "Tickets", "system:log:list", 1);
        createMenu(menuLog, "登录日志", "MENU", "/log/login", null, "Key", "system:log:list", 2);

        createMenu(menuSys, "在线用户", "MENU", "/system/online", null, "Monitor", "admin", 6);

        // 7. 树洞管理菜单
        Long menuTreehole = createMenu(menuSys, "树洞管理", "CATALOG", "/admin/treehole", null, "ChatLineRound", null, 7);
        createMenu(menuTreehole, "帖子管理", "MENU", "/admin/treehole/post", null, "Document", "admin", 1);
        createMenu(menuTreehole, "评论管理", "MENU", "/admin/treehole/comment", null, "ChatDotRound", "admin", 2);
        createMenu(menuTreehole, "举报管理", "MENU", "/admin/treehole/report", null, "Warning", "admin", 3);
        createMenu(menuTreehole, "分类管理", "MENU", "/admin/treehole/category", null, "Files", "admin", 4);
        createMenu(menuTreehole, "数据统计", "MENU", "/admin/treehole/statistics", null, "DataAnalysis", "admin", 5);

        log.info("菜单数据初始化完成");

        // 5. 分配用户角色
        createUserRole(admin.getId(), adminRole.getId());
        createUserRole(tech.getId(), techRole.getId());
        createUserRole(tech.getId(), userRole.getId());

        // 6. 分配角色菜单
        List<SysMenu> allMenus = menuMapper.selectList(new LambdaQueryWrapper<>());
        for (SysMenu menu : allMenus) {
            createRoleMenu(adminRole.getId(), menu.getId());
        }

        // 技术负责人权限
        Long[] techMenuPerms = {menuSys, menuUser,
            getMenuIdByPermission("system:user:query"),
            menuRole, getMenuIdByPermission("system:role:query"),
            menuMenu, getMenuIdByPermission("system:menu:query"),
            menuDept, getMenuIdByPermission("system:dept:query"),
            menuLog};
        for (Long menuId : techMenuPerms) {
            if (menuId != null) {
                createRoleMenu(techRole.getId(), menuId);
            }
        }

        // 普通用户权限
        Long[] userMenuPerms = {menuSys, menuUser,
            getMenuIdByPermission("system:user:query"),
            menuRole, getMenuIdByPermission("system:role:query"),
            menuMenu, getMenuIdByPermission("system:menu:query"),
            menuDept, getMenuIdByPermission("system:dept:query")};
        for (Long menuId : userMenuPerms) {
            if (menuId != null) {
                createRoleMenu(userRole.getId(), menuId);
            }
        }

        log.info("========== 数据初始化完成 ==========");
        log.info("默认账号: admin, tech, backend (密码见 application.yml 或通过环境变量配置)");
    }

    private SysDept createDept(String name, Long parentId, String ancestors, int sort, String leader) {
        SysDept dept = new SysDept();
        dept.setDeptName(name);
        dept.setParentId(parentId);
        dept.setAncestors(ancestors);
        dept.setSort(sort);
        dept.setLeader(leader);
        dept.setStatus(1);
        deptMapper.insert(dept);
        return dept;
    }

    private SysUser createUser(String username, String password, String nickname, String email, Long deptId) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setDeptId(deptId);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    private SysRole createRole(String name, String code, String desc, int dataScope) {
        SysRole role = new SysRole();
        role.setRoleName(name);
        role.setRoleCode(code);
        role.setRoleDesc(desc);
        role.setDataScope(dataScope);
        role.setStatus(1);
        roleMapper.insert(role);
        return role;
    }

    private Long createMenu(Long parentId, String name, String type, String path,
                                String component, String icon, String permission, int sort) {
        SysMenu menu = new SysMenu();
        menu.setParentId(parentId);
        menu.setMenuName(name);
        menu.setMenuType(type);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setIcon(icon);
        menu.setPermission(permission);
        menu.setSort(sort);
        menu.setStatus(1);
        menu.setVisible(1);
        menuMapper.insert(menu);
        return menu.getId();
    }

    private void createUserRole(Long userId, Long roleId) {
        SysUserRole ur = new SysUserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        userRoleMapper.insert(ur);
    }

    private void createRoleMenu(Long roleId, Long menuId) {
        SysRoleMenu rm = new SysRoleMenu();
        rm.setRoleId(roleId);
        rm.setMenuId(menuId);
        roleMenuMapper.insert(rm);
    }

    private Long getMenuIdByPermission(String permission) {
        if (permission == null) return null;
        List<SysMenu> menus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPermission, permission));
        return CollUtil.isNotEmpty(menus) ? menus.get(0).getId() : null;
    }
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.config.DataInitializer
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Slf4j — Lombok，注入日志对象
//   - @Component — Spring，注册为容器组件
//   - @RequiredArgsConstructor — Lombok，生成必需参数构造器（实现构造器注入）
//   - CommandLineRunner — Spring Boot，应用启动后执行一次性初始化逻辑（run 方法）
//   - @Transactional — Spring，声明式事务，保证初始化整体原子性
//   - @Override — Java，标识重写接口方法
//   - LambdaQueryWrapper / CollUtil — MyBatis-Plus / Hutool，构建查询条件与集合工具
//
// 【关键依赖】
//   - 依赖 SysUserMapper / SysRoleMapper / SysMenuMapper / SysDeptMapper /
//     SysUserRoleMapper / SysRoleMenuMapper → 插入初始化数据
//   - 依赖 PasswordEncoder（Spring Security） → 对初始用户密码进行 BCrypt 加密
//
// 【关联文件】
//   - 被 Spring 容器自动检测并执行（CommandLineRunner 约定）
//   - 依赖 SysUser / SysRole / SysMenu / SysDept / SysUserRole / SysRoleMenu 实体
//   - 初始化数据被 Spring Security 鉴权逻辑消费（角色-菜单-按钮权限体系）
//
// 【核心作用】
//   应用启动时，若 sys_user 表为空，则自动初始化一套完整的 RBAC 基础数据：4 个角色
//   （超级管理员/技术负责人/普通用户等）、10+ 部门、多个用户、40+ 菜单/按钮、用户-角色
//   关联、角色-菜单关联，确保系统开箱即用。
//
// 【设计必要性】
//   避免手工执行 SQL 脚本初始化数据，保证研发/测试/演示环境快速就绪；幂等设计（数据已存在
//   则跳过）保证重复启动安全。
//
// 【注意事项/安全提示】
//   - 密码经过 PasswordEncoder.encode() 加密存储，日志中不输出明文密码，避免凭证泄露
//   - 默认账号建议在生产部署后及时修改密码或禁用
//   - 本初始化逻辑仅适用于首次部署；生产环境应通过更可控的迁移脚本管理基础数据
// ============================================================
