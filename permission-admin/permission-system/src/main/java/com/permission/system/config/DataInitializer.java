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
        log.info("默认账号: admin / Admin@1234");
        log.info("默认账号: tech / Admin@1234");
        log.info("默认账号: backend / Admin@1234");
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
