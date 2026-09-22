package com.permission.system.security;

import cn.hutool.core.collection.CollUtil;
import com.permission.common.dto.LoginUser;
import com.permission.common.entity.*;
import com.permission.common.enums.DataScope;
import com.permission.common.enums.UserStatus;
import com.permission.framework.security.CustomUserDetailsService;
import com.permission.system.mapper.*;
import com.permission.system.support.DataScopeHelper;
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
    private final SysRoleDeptMapper roleDeptMapper;

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
        SysDept selfDept = null;
        if (user.getDeptId() != null) {
            selfDept = deptMapper.selectById(user.getDeptId());
            if (selfDept != null) {
                deptName = selfDept.getDeptName();
            }
        }

        // ===== 数据权限：取最宽范围（code 越小范围越大），并预计算可见部门集合 =====
        Integer dataScope = DataScope.SELF.getCode();
        Integer limitLevel = null;
        for (SysRole role : roleList) {
            if (role.getStatus() != null && role.getStatus() != 1) continue;
            if (role.getDataScope() != null && role.getDataScope() < dataScope) {
                dataScope = role.getDataScope();
                limitLevel = role.getDataScopeLevel();
            }
        }

        Long groupId = DataScopeHelper.resolveGroupId(selfDept);
        Long companyId = DataScopeHelper.resolveCompanyId(selfDept);
        List<Long> deptIds = Collections.emptyList();
        List<Long> deptTreeIds = Collections.emptyList();
        try {
            DataScope scope = DataScope.of(dataScope);
            if (scope != DataScope.ALL && scope != DataScope.SELF) {
                List<Long> customDeptIds = scope == DataScope.CUSTOM
                        ? roleDeptMapper.selectDeptIdsByRoleIds(roleIds)
                        : Collections.emptyList();
                // 组织树规模小，一次性加载用于子树解析（避免运行时递归查询）
                List<SysDept> allDepts = deptMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDept>()
                                .eq(SysDept::getDeleted, 0));
                deptIds = DataScopeHelper.resolveVisibleDeptIds(dataScope, limitLevel,
                        selfDept, allDepts, groupId, companyId, customDeptIds);
                // 部门树查询需要连带祖先，否则父节点被过滤会导致整棵树渲染不出来
                deptTreeIds = DataScopeHelper.withAncestors(deptIds, allDepts);
            }
        } catch (Exception e) {
            // 数据范围解析失败时按最保守策略（仅本人）降级，避免放大权限
            log.error("数据范围解析失败，降级为仅本人 userId={} dataScope={}", user.getId(), dataScope, e);
            dataScope = DataScope.SELF.getCode();
            deptIds = Collections.emptyList();
            deptTreeIds = Collections.emptyList();
        }

        return LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .nickname(user.getNickname() != null ? user.getNickname() : user.getUsername())
                .deptId(user.getDeptId())
                .deptName(deptName)
                .dataScope(dataScope)
                .deptIds(deptIds)
                .deptTreeIds(deptTreeIds)
                .deptLevel(selfDept == null ? null
                        : (selfDept.getDeptLevel() != null ? selfDept.getDeptLevel()
                        : DataScopeHelper.calcLevel(selfDept.getAncestors())))
                .companyId(companyId)
                .groupId(groupId)
                .permissions(permissions)
                .roles(roles)
                .build();
    }
}

