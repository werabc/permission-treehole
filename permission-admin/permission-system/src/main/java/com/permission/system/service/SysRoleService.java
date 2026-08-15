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

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.service.SysRoleService
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - IService<SysRole> — MyBatis-Plus，继承通用 Service 接口，提供角色基础 CRUD 能力
//   - IPage / Set — MyBatis-Plus / JDK，分页查询与集合返回类型
//
// 【关键依赖】
//   - 依赖 SysRole 实体 → 角色业务操作的数据载体
//
// 【关联文件】
//   - 被 SysRoleServiceImpl 实现，封装角色业务逻辑（含名称/编码唯一性校验、关联清理）
//   - 被 RoleController 调用，提供角色管理 API
//   - 被 UserDetailsServiceImpl 调用（间接），加载用户角色信息
//
// 【核心作用】
//   角色业务服务接口，定义角色分页查询、详情查询、批量增删改、状态修改、菜单权限分配、
//   角色菜单 ID 查询以及全量角色下拉等业务方法。
//
// 【设计必要性】
//   接口与实现分离，便于在实现层封装角色名称/编码的联合唯一性校验、级联删除保护等复杂业务。
//
// 【注意事项/安全提示】
//   - 删除角色前实现层校验是否仍有用户关联该角色，防止产生孤儿角色导致权限失效
//   - assignsMenus 方法变更角色菜单关联，涉及权限变更，必须通过 @PreAuthorize 与 @OperationLog 双重保护
// ============================================================
