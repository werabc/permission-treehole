package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private String roleName;
    private String roleCode;
    private String roleDesc;
    private Integer dataScope;
    private Integer status;

    @TableField(exist = false)
    private String dataScopeName;

    @TableField(exist = false)
    private String deptIds;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.entity.SysRole
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Data — Lombok，生成访问器
//   - @EqualsAndHashCode(callSuper = true) — 比较时含父类字段
//   - @TableName("sys_role") — MyBatis-Plus 映射角色表
//   - @TableField(exist = false) — dataScopeName 仅作展示字段，deptIds 为自定义部门集合
//
// 【关联文件】
//   - 继承 BaseEntity → BaseEntity.java
//   - 被 RoleService/Impl 读写 → service/*RoleService*.java
//   - 被 SysUserRole 关联 → SysUserRole.java
//   - 被 SysRoleMenu 关联 → SysRoleMenu.java
//   - 被 LoginUser.roles 引用 → dto/LoginUser.java
//   - dataScope 值对应 DataScope 枚举 → enums/DataScope.java
//
// 【核心作用】角色实体，承载角色名/编码/描述/数据权限范围。
//
// 【设计必要性】dataScope 决定该角色能看到的数据范围，关系型权限设计的中枢，
//   按钮级权限通过关联 sys_role_menu 实现。
//
// 【注意事项】
//   - dataScope=4（CUSTOM）表示自定义部门集合，读取 deptIds 才能生效
//   - roleCode 应与 Spring Security 的 ROLE_ 前缀约定一致
//   - 删除前需检查用户关联
// ============================================================
