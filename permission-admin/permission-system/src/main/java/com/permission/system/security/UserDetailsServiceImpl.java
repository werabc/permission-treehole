package com.permission.system.security;

import cn.hutool.core.collection.CollUtil;
import com.permission.common.dto.LoginUser;
import com.permission.common.entity.*;
import com.permission.common.enums.UserStatus;
import com.permission.framework.security.CustomUserDetailsService;
import com.permission.system.mapper.*;
import com.permission.common.entity.ThUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService, CustomUserDetailsService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final ThUserMapper thUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if (user.getStatus() != null && user.getStatus() == UserStatus.DISABLED.getCode()) {
            throw new UsernameNotFoundException("账号已被禁用: " + username);
        }

        return buildLoginUser(user);
    }

    @Override
    public LoginUser loadUserById(Long userId) {
        // 先查管理员表
        SysUser sysUser = userMapper.selectById(userId);
        if (sysUser != null) {
            return buildLoginUser(sysUser);
        }
        // 再查树洞用户表
        ThUser thUser = thUserMapper.selectById(userId);
        if (thUser != null) {
            return buildTreeholeLoginUser(thUser);
        }
        return null;
    }

    private LoginUser buildTreeholeLoginUser(ThUser user) {
        return LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .nickname(user.getNickname() != null ? user.getNickname() : user.getUsername())
                .permissions(new HashSet<>())
                .roles(new HashSet<>())
                .build();
    }

    private LoginUser buildLoginUser(SysUser user) {
        Set<String> permissions = new HashSet<>();
        Set<String> roles = new HashSet<>();

        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getId()));
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();

        // Load roles once; reused for role codes, permissions, and dataScope (avoid redundant DB query)
        List<SysRole> roleList = CollUtil.isNotEmpty(roleIds) ? roleMapper.selectBatchIds(roleIds) : Collections.emptyList();

        if (CollUtil.isNotEmpty(roleList)) {
            for (SysRole role : roleList) {
                if (role.getStatus() != null && role.getStatus() == 1) {
                    roles.add(role.getRoleCode());
                }
            }

            List<SysRoleMenu> roleMenus = new ArrayList<>();
            for (Long roleId : roleIds) {
                roleMenus.addAll(menuMapper.selectRoleMenusByRoleId(roleId));
            }
            List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).distinct().toList();

            if (CollUtil.isNotEmpty(menuIds)) {
                List<SysMenu> menuList = menuMapper.selectBatchIds(menuIds);
                permissions.addAll(
                        menuList.stream()
                                .filter(m -> m.getPermission() != null && !m.getPermission().isEmpty())
                                .map(SysMenu::getPermission)
                                .collect(Collectors.toSet())
                );
            }
        }

        String deptName = "";
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) {
                deptName = dept.getDeptName();
            }
        }

        // Compute dataScope from already-loaded roles (lower value = broader scope)
        Integer dataScope = 5; // Default: SELF
        for (SysRole role : roleList) {
            if (role.getDataScope() != null && role.getDataScope() < dataScope) {
                dataScope = role.getDataScope();
            }
        }

        // deptIds remains empty for now: CUSTOM scope filtering not fully implemented
        List<Long> deptIds = new ArrayList<>();

        return LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .nickname(user.getNickname() != null ? user.getNickname() : user.getUsername())
                .deptId(user.getDeptId())
                .deptName(deptName)
                .dataScope(dataScope)
                .deptIds(deptIds)
                .permissions(permissions)
                .roles(roles)
                .build();
    }
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.security.UserDetailsServiceImpl
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Slf4j — Lombok，注入日志对象
//   - @Service — Spring，声明业务层组件
//   - @RequiredArgsConstructor — Lombok，生成必需参数构造器（构造器注入）
//   - @Override — Java，标识重写接口方法（loadUserByUsername / loadUserById）
//   - UserDetailsService — Spring Security，标准用户加载接口（loadUserByUsername）
//   - CustomUserDetailsService — 自定义接口，扩展按 ID 加载用户（loadUserById）
//   - UsernameNotFoundException — Spring Security，用户不存在/禁用时抛出的标准异常
//
// 【关键依赖】
//   - 依赖 SysUserMapper → 按用户名/ID 查询用户
//   - 依赖 SysRoleMapper → 批量查询角色（含 status、dataScope）
//   - 依赖 SysMenuMapper → 查询角色关联菜单（selectRoleMenusByRoleId）
//   - 依赖 SysDeptMapper → 查询用户所属部门名称
//   - 依赖 SysUserRoleMapper → 查询用户关联的角色
//   - 依赖 LoginUser DTO → 承载加载后的用户完整信息（给 Spring Security 使用）
//   - 依赖 UserStatus 枚举 → 判断用户是否被禁用
//   - 依赖 CollUtil — Hutool，集合工具，避免空集合遍历
//
// 【关联文件】
//   - 被 Spring Security 框架自动调用（loadUserByUsername，登录认证时）
//   - 被 JwtAuthenticationFilter 调用（loadUserById，认证过滤器中按 token 重建登录用户）
//   - 被 @MapperScan 扫描注册（通过依赖的 Mapper 间接）
//   - 产出 LoginUser 被 Authentication 携带，供 @PreAuthorize 与方法参数注入使用
//
// 【核心作用】
//   Spring Security 的核心入口：根据用户名（或 ID）加载用户信息，构建包含完整角色码、权限码、
//   部门信息、数据范围（dataScope）的 LoginUser 对象，供鉴权框架使用。
//
// 【设计必要性】
//   统一的用户信息加载点：登录认证、Token 解析后的用户重建都通过此 UserDetailsServiceImpl
//   完成，保证用户权限信息的一致性；同时避免重复查询（角色一次加载后复用计算 roleCodes、
//   permissions、dataScope）。
//
// 【注意事项/安全提示】
//   - 禁用用户拦截：null-safe status 检查，用户 status=DISABLED 时抛出 UsernameNotFoundException，
//     阻止被禁用账号登录；空值判断防止旧数据未设 status 字段时出现 NPE
//   - dataScope 优化：从单次已加载的角色集合中迭代计算最小 dataScope（最宽权限），
//     避免二次数据库查询
//   - deptName 通过 deptId 关联查询补充，避免用户表 JOIN 部门表
//   - LoginUser 构建时不包含密码明文（密码 hash 由 Spring Security 内部比较后清除），安全可控
// ============================================================
