package com.permission.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.ResultCode;
import com.permission.common.entity.*;
import com.permission.common.exception.BusinessException;
import com.permission.system.mapper.*;
import com.permission.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public IPage<SysRole> pageRoles(long pageNum, long pageSize, String keyword) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SysRole::getRoleName, keyword)
                    .or().like(SysRole::getRoleCode, keyword));
        }
        wrapper.orderByAsc(SysRole::getCreateTime);
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public SysRole getRoleById(Long id) {
        SysRole role = baseMapper.selectById(id);
        if (role != null) {
            Set<Long> menuIds = getRoleMenuIds(id);
            role.setDeptIds(menuIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        }
        return role;
    }

    @Override
    @Transactional
    public void createRole(SysRole role) {
        validateRoleNameUnique(role.getRoleName(), null);
        validateRoleCodeUnique(role.getRoleCode(), null);
        baseMapper.insert(role);
    }

    @Override
    @Transactional
    public void updateRole(SysRole role) {
        SysRole existing = baseMapper.selectById(role.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        validateRoleNameUnique(role.getRoleName(), role.getId());
        validateRoleCodeUnique(role.getRoleCode(), role.getId());
        baseMapper.updateById(role);
    }

    @Override
    @Transactional
    public void deleteRoles(List<Long> ids) {
        if (CollUtil.isNotEmpty(ids)) {
            for (Long id : ids) {
                long count = userRoleMapper.selectCount(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
                if (count > 0) {
                    SysRole role = baseMapper.selectById(id);
                    throw new BusinessException(ResultCode.ROLE_HAS_USERS,
                            "角色【" + (role != null ? role.getRoleName() : id) + "】已分配给用户，无法删除");
                }
            }
            baseMapper.deleteBatchIds(ids);
            roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, ids));
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
