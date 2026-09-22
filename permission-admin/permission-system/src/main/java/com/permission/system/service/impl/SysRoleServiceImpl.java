package com.permission.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.ResultCode;
import com.permission.common.entity.*;
import com.permission.common.enums.DataScope;
import com.permission.common.exception.BusinessException;
import com.permission.system.mapper.*;
import com.permission.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleDeptMapper roleDeptMapper;

    /** 内置角色编码：不允许删除、不允许改编码，避免把自己锁在门外 */
    private static final Set<String> BUILTIN_CODES = Set.of("admin");

    @Override
    public IPage<SysRole> pageRoles(long pageNum, long pageSize, String keyword) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            // 转义LIKE通配符，防止通配符注入
            String safeKeyword = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            wrapper.and(w -> w.like(SysRole::getRoleName, safeKeyword)
                    .or().like(SysRole::getRoleCode, safeKeyword));
        }
        wrapper.orderByAsc(SysRole::getCreateTime);
        IPage<SysRole> result = baseMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::fillScopeDesc);
        return result;
    }

    @Override
    public SysRole getRoleById(Long id) {
        SysRole role = baseMapper.selectById(id);
        if (role != null) {
            fillScopeDesc(role);
            // 自定义数据范围时回显已选部门，供前端部门树回填
            List<Long> deptIds = roleDeptMapper.selectDeptIdsByRoleId(id);
            role.setDeptIds(deptIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        }
        return role;
    }

    /** 填充数据范围名称与内置标记（前端下拉/标签展示用） */
    private void fillScopeDesc(SysRole role) {
        role.setDataScopeName(DataScope.of(role.getDataScope()).getDesc());
        role.setBuiltin(role.getRoleCode() != null && BUILTIN_CODES.contains(role.getRoleCode().toLowerCase()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(SysRole role) {
        validateRoleNameUnique(role.getRoleName(), null);
        validateRoleCodeUnique(role.getRoleCode(), null);
        validateDataScope(role);
        baseMapper.insert(role);
        saveRoleDepts(role.getId(), role.getDataScope(), role.getDeptIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(SysRole role) {
        SysRole existing = baseMapper.selectById(role.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        // 内置角色不允许修改编码（编码即权限标识，改了会直接断链）
        boolean builtin = existing.getRoleCode() != null && BUILTIN_CODES.contains(existing.getRoleCode().toLowerCase());
        if (builtin && role.getRoleCode() != null && !existing.getRoleCode().equals(role.getRoleCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "内置角色不允许修改角色编码");
        }
        validateRoleNameUnique(role.getRoleName(), role.getId());
        validateRoleCodeUnique(role.getRoleCode(), role.getId());
        validateDataScope(role);
        baseMapper.updateById(role);
        if (role.getDataScope() != null) {
            saveRoleDepts(role.getId(), role.getDataScope(), role.getDeptIds());
        }
    }

    /** 数据范围参数校验 */
    private void validateDataScope(SysRole role) {
        if (role.getDataScope() == null) return;
        DataScope scope = DataScope.of(role.getDataScope());
        if (scope.getCode() != role.getDataScope()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "数据范围取值非法");
        }
        if (scope == DataScope.CUSTOM && StrUtil.isBlank(role.getDeptIds())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "自定义数据范围至少需要选择一个部门");
        }
        if (scope == DataScope.DEPT_AND_SUB_LEVEL) {
            Integer n = role.getDataScopeLevel();
            if (n == null || n < 1 || n > 10) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "层级深度 N 需在 1-10 之间");
            }
        }
    }

    /** 覆盖式保存自定义数据范围部门（非自定义范围时清空，避免脏数据残留） */
    private void saveRoleDepts(Long roleId, Integer dataScope, String deptIds) {
        roleDeptMapper.deleteByRoleId(roleId);
        if (DataScope.of(dataScope) != DataScope.CUSTOM || StrUtil.isBlank(deptIds)) {
            return;
        }
        for (String seg : deptIds.split(",")) {
            String s = seg.trim();
            if (s.isEmpty()) continue;
            try {
                SysRoleDept rd = new SysRoleDept();
                rd.setRoleId(roleId);
                rd.setDeptId(Long.parseLong(s));
                roleDeptMapper.insert(rd);
            } catch (NumberFormatException e) {
                log.warn("角色 {} 自定义部门ID非法，已跳过: {}", roleId, s);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoles(List<Long> ids) {
        if (CollUtil.isNotEmpty(ids)) {
            for (Long id : ids) {
                SysRole role = baseMapper.selectById(id);
                // 内置角色不允许删除，避免把超级管理员体系删空导致无人可管理
                if (role != null && role.getRoleCode() != null
                        && BUILTIN_CODES.contains(role.getRoleCode().toLowerCase())) {
                    throw new BusinessException(ResultCode.FORBIDDEN,
                            "内置角色【" + role.getRoleName() + "】不允许删除");
                }
                long count = userRoleMapper.selectCount(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
                if (count > 0) {
                    throw new BusinessException(ResultCode.ROLE_HAS_USERS,
                            "角色【" + (role != null ? role.getRoleName() : id) + "】已分配给用户，无法删除");
                }
            }
            baseMapper.deleteBatchIds(ids);
            roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, ids));
            roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().in(SysRoleDept::getRoleId, ids));
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setStatus(status);
        baseMapper.updateById(role);
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, Set<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        if (CollUtil.isNotEmpty(menuIds)) {
            for (Long menuId : menuIds) {
                SysRoleMenu roleMenu = new SysRoleMenu();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                roleMenuMapper.insert(roleMenu);
            }
        }
    }

    @Override
    public Set<Long> getRoleMenuIds(Long roleId) {
        List<SysRoleMenu> list = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        return list.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
    }

    @Override
    public List<SysRole> getAllRoles() {
        return baseMapper.selectList(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getStatus, 1));
    }

    private void validateRoleNameUnique(String roleName, Long excludeId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleName, roleName);
        if (excludeId != null) {
            wrapper.ne(SysRole::getId, excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.ROLE_NAME_EXISTS);
        }
    }

    private void validateRoleCodeUnique(String roleCode, Long excludeId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode);
        if (excludeId != null) {
            wrapper.ne(SysRole::getId, excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.ROLE_CODE_EXISTS);
        }
    }
}

