package com.permission.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.permission.common.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.mapper.SysUserRoleMapper
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Mapper — MyBatis-Plus，标识映射接口，被 MapperScan 扫描注册
//   - BaseMapper<SysUserRole> — MyBatis-Plus 通用 Mapper，提供 sys_user_role 表的标准 CRUD
//
// 【关键依赖】
//   - 依赖 SysUserRole 实体 → 用户-角色关联表（sys_user_role）的 ORM 映射载体
//
// 【关联文件】
//   - 被 SysUserServiceImpl 调用，分配角色（assignRoles）、删除用户时清理关联
//   - 被 SysRoleServiceImpl 调用，删除角色前检查是否仍有用户关联
//   - 被 UserDetailsServiceImpl 调用，加载用户角色关系
//   - 被 DataInitializer 调用，初始化用户-角色关联数据
//   - 被 @MapperScan 扫描注册
//
// 【核心作用】
//   用户-角色关联表（sys_user_role）的数据访问接口，维护用户与角色之间的多对多绑定关系，
//   是 RBAC 权限体系的用户侧数据基础。
//
// 【设计必要性】
//   用户与角色之间是多对多关系，需通过中间表维护，独立 Mapper 统一封装对关联表的访问。
//
// 【注意事项/安全提示】
//   - 分配用户角色时采用"先删除再插入"策略（assignRoles），保证操作幂等
//   - 删除用户时需同步清除该用户的所有角色关联记录
// ============================================================
