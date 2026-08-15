package com.permission.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.permission.common.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.mapper.SysDeptMapper
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Mapper — MyBatis-Plus，标识映射接口，被 MapperScan 扫描注册为 Mapper Bean
//   - BaseMapper<SysDept> — MyBatis-Plus 通用 Mapper，提供 CRUD 基础能力（insert/delete/
//     update/select），无需编写 XML 即可操作 sys_dept 表
//
// 【关键依赖】
//   - 依赖 SysDept 实体 → Dept 表（sys_dept）的 ORM 映射载体
//
// 【关联文件】
//   - 被 SysDeptService / SysDeptServiceImpl 调用，提供部门数据的增删改查
//   - 被 UserDetailsServiceImpl 调用，查询用户所属部门名称
//   - 被 DataInitializer 调用，初始化部门数据
//   - 被 @MapperScan 扫描注册
//
// 【核心作用】
//   部门表（sys_dept）的数据访问接口，通过 MyBatis-Plus 提供标准 CRUD 操作。部门表
//   支持层级结构（通过 parentId/ancestors 字段实现树形组织），对应企业的组织架构。
//
// 【设计必要性】
//   部门是企业 RBAC 中数据权限（dataScope）维度的核心实体，独立 Mapper 便于维护和复用。
//
// 【注意事项/安全提示】
//   - 树形结构通过 parentId + ancestors 维护，更新部门时需同步刷新 ancestors 路径
//   - 删除前需校验是否存在子部门或关联用户（业务层已在 Service 中校验）
// ============================================================
