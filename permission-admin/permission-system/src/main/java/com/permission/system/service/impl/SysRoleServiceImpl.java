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
            // Do NOT set deptIds to menu IDs - frontend calls /{id}/menus separately
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

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.service.impl.SysRoleServiceImpl
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Service — Spring，声明业务层组件
//   - @RequiredArgsConstructor — Lombok，生成必需参数构造器（构造器注入）
//   - @Override — Java，标识重写接口/父类方法
//   - @Transactional — Spring，声明式事务，保证角色写操作与关联清理的原子性
//     （删除角色+清理关联、分配菜单、更新状态等）
//   - ServiceImpl<SysRoleMapper, SysRole> — MyBatis-Plus，继承通用 Service 实现基类
//   - Page / IPage / LambdaQueryWrapper — MyBatis-Plus，分页查询与条件构造器
//   - StrUtil / CollUtil — Hutool，字符串/集合工具
//   - BusinessException / ResultCode — 自定义业务异常与错误码（ROLE_HAS_USERS/
//     ROLE_NAME_EXISTS/ROLE_CODE_EXISTS 等）
//
// 【关键依赖】
//   - 依赖 SysRoleMapper → 角色数据访问
//   - 依赖 SysRoleMenuMapper → 角色菜单关联表访问（分配/查询菜单 + 删除时级联清理）
//   - 依赖 SysUserRoleMapper → 删除角色前校验是否仍有用户关联
//   - 依赖 SysMenuMapper → 维护角色菜单关系时使用
//   - 依赖 SysRole 实体 → 角色业务操作载体
//   - 依赖 SysRoleService 接口 → 实现该接口契约
//
// 【关联文件】
//   - 被 RoleController 调用，提供角色管理业务逻辑
//   - 被 UserDetailsServiceImpl 调用（间接通过 Mapper），加载用户角色信息
//   - 被 SysUserServiceImpl 调用（间接），分配用户角色时校验关联
//   - 被 OperationLogAspect 切面拦截（写操作会记录 @OperationLog）
//
// 【核心作用】
//   角色业务服务实现：提供角色分页查询、单角色详情、新增角色（名称/编码唯一性校验）、
//   修改角色、批量级联删除角色（无用户关联才允许）并清理关联、状态修改、菜单权限分配、
//   角色菜单 ID 查询、全量可用角色下拉。
//
// 【设计必要性】
//   角色是企业 RBAC 权限体系的核心枢纽，涉及名称/编码唯一性、级联删除保护、权限变更审计
//   等复杂业务规则，统一封装在 Service 层避免散落在 Controller 或 Mapper 中。
//
// 【注意事项/安全提示】
//   - deleteRoles 严格校验：删除前遍历每个角色，若仍有用户关联则拒绝并提示角色名；通过校验
//     后再批量删除角色 + 级联清理 role_menu 关联，保证数据一致性（@Transactional 保证原子性）
//   - validateRoleNameUnique / validateRoleCodeUnique：新增和修改均校验唯一性，修改时可排除自身 ID
//   - assignMenus 采用"先删除再插入"策略分配菜单，保证幂等（重复分配结果一致）
//   - getRoleById 明确说明不把 deptIds 混为 menuIds，前端通过 /{id}/menus 接口单独获取菜单
// ============================================================
