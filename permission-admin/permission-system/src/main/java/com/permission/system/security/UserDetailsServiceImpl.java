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
        // 默认按管理员加载（兼容旧调用）
        return loadUserById(userId, false);
    }

    @Override
    public LoginUser loadUserById(Long userId, boolean isTreehole) {
        if (isTreehole) {
            // 树洞端：只查树洞用户表
            ThUser thUser = thUserMapper.selectById(userId);
            if (thUser != null) {
                return buildTreeholeLoginUser(thUser);
            }
            return null;
        } else {
            // 管理端：只查管理员表
            SysUser sysUser = userMapper.selectById(userId);
            if (sysUser != null) {
                return buildLoginUser(sysUser);
            }
            return null;
        }
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

