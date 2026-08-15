package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("sys_role_menu")
public class SysRoleMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long roleId;
    private Long menuId;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.entity.SysRoleMenu
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Data — Lombok，生成访问器
//   - @TableName("sys_role_menu") — MyBatis-Plus 映射角色-菜单关联表
//   - implements Serializable — 保证可序列化
//
// 【关联文件】
//   - 关联 SysRole → SysRole.java
//   - 关联 SysMenu → SysMenu.java
//   - 由 RoleService/Impl 在分配菜单时批量写入 → service/*RoleService*.java
//
// 【核心作用】角色与菜单的多对多关联表实体。
//
// 【设计必要性】纯粹多对多中间表，没有继承 BaseEntity 的必要；只持两个外键，
//   在角色授权菜单时通过 service 批量写入来实现。
//
// 【注意事项】
//   - 无自动 id，主键为 (roleId, menuId)（依靠数据库联合唯一索引）
//   - 删除角色前要清理此表的关联行
// ============================================================
