package com.permission.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.permission.common.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.mapper.SysRoleMapper
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Mapper — MyBatis-Plus，标识映射接口，被 MapperScan 扫描注册
//   - BaseMapper<SysRole> — MyBatis-Plus 通用 Mapper，提供 sys_role 表的标准 CRUD
//
// 【关键依赖】
//   - 依赖 SysRole 实体 → 角色表（sys_role）的 ORM 映射载体
//
// 【关联文件】
//   - 被 SysRoleService / SysRoleServiceImpl 调用，提供角色数据的增删改查
//   - 被 UserDetailsServiceImpl 调用，加载用户角色信息（角色码与数据范围）
//   - 被 SysUserServiceImpl 查询已分配到角色的用户数
//   - 被 DataInitializer 调用，初始化角色数据
//   - 被 @MapperScan 扫描注册
//
// 【核心作用】
//   角色表（sys_role）的数据访问接口。角色是 RBAC 权限体系的核心实体，通过 dataScope 字段
//   定义角色的数据权限范围（全部/本部门/本人等），通过 roleCode 实现方法级鉴权。
//
// 【设计必要性】
//   角色是连接用户与权限的中间桥梁，独立 Mapper 便于角色 CRUD 与关联操作维护。
//
// 【注意事项/安全提示】
//   - 删除角色前需校验是否仍有用户关联该角色（业务层在 Service 中校验）
//   - roleCode 具有唯一约束，新增/修改时业务层做了唯一性校验
// ============================================================
