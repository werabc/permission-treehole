package com.permission.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.permission.common.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.mapper.SysRoleMenuMapper
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Mapper — MyBatis-Plus，标识映射接口，被 MapperScan 扫描注册
//   - BaseMapper<SysRoleMenu> — MyBatis-Plus 通用 Mapper，提供 sys_role_menu 表的标准 CRUD
//
// 【关键依赖】
//   - 依赖 SysRoleMenu 实体 → 角色-菜单关联表（sys_role_menu）的 ORM 映射载体
//
// 【关联文件】
//   - 被 SysRoleServiceImpl 调用，provide 角色菜单关联的增删改查（assignMenus/getRoleMenuIds）
//   - 被 SysMenuServiceImpl 调用，删除菜单时同步清理角色菜单关联
//   - 被 DataInitializer 调用，初始化角色-菜单关联数据
//   - 被 @MapperScan 扫描注册
//
// 【核心作用】
//   角色-菜单（按钮权限）关联表（sys_role_menu）的数据访问接口。实现角色与菜单/按钮之间的
//   多对多绑定关系，是 RBAC 权限鉴权的数据基础。
//
// 【设计必要性】
//   角色与菜单/按钮权限之间是多对多关系，需通过中间表维护；独立 Mapper 统一封装对该表的
//   访问，避免业务层直接拼接 SQL。
//
// 【注意事项/安全提示】
//   - 分配菜单时采用"先删除再插入"策略，保证幂等（重复分配结果一致）
//   - 删除菜单时需清除该菜单在所有角色下的关联记录（业务层已在 Service 中处理）
// ============================================================
