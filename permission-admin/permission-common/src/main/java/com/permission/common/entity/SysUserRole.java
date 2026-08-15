package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("sys_user_role")
public class SysUserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long roleId;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.entity.SysUserRole
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Data — Lombok，生成访问器
//   - @TableName("sys_user_role") — MyBatis-Plus 映射用户-角色关联表
//   - implements Serializable — 保证可序列化
//
// 【关联文件】
//   - 关联 SysUser → SysUser.java
//   - 关联 SysRole → SysRole.java
//   - 在 DetailServiceImpl.loadUserByUsername 中通过 LoginUser.roles 授权 →
//     service/impl/DetailServiceImpl.java
//   - 由 UserService/Impl 在分配角色时读写 → service/*UserService*.java
//
// 【核心作用】用户与角色多对多关联实体。
//
// 【设计必要性】多对多关联必须独立成表；不继承 BaseEntity 因为纯外键一对。
//
// 【注意事项】
//   - 无自增 id，依赖联合唯一索引 (userId, roleId)
//   - 删除用户/角色前应及时清理此表
// ============================================================
