package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.SysRole;

import java.util.List;
import java.util.Set;

public interface SysRoleService extends IService<SysRole> {

    IPage<SysRole> pageRoles(long pageNum, long pageSize, String keyword);

    SysRole getRoleById(Long id);

    void createRole(SysRole role);

    void updateRole(SysRole role);

    void deleteRoles(List<Long> ids);

    void updateStatus(Long id, Integer status);

    void assignMenus(Long roleId, Set<Long> menuIds);

    Set<Long> getRoleMenuIds(Long roleId);

    List<SysRole> getAllRoles();
}

