package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    private Long parentId;
    private String menuName;
    private String menuType;
    private String path;
    private String component;
    private String icon;
    private String permission;
    private Integer sort;
    private Integer status;
    private Integer visible;

    @TableField(exist = false)
    private List<SysMenu> children;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.entity.SysMenu
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Data — Lombok，生成访问器
//   - @EqualsAndHashCode(callSuper = true) — 含父类字段比较
//   - @TableName("sys_menu") — MyBatis-Plus 映射菜单表
//   - @TableField(exist = false) — children 内存组装的树节点，非数据库列
//
// 【关联文件】
//   - 继承 BaseEntity → BaseEntity.java
//   - 被 MenuService/Impl 读写 → service/*MenuService*.java
//   - 被 SysRoleMenu 关联 → SysRoleMenu.java
//   - 由 LoginUser.permissions 引用作为 @PreAuthorize 鉴权目标
//   - 前端路由表根据 path/component/icon 动态生成菜单
//
// 【核心作用】菜单/权限实体，字段 roleType 区分目录/菜单/按钮，permission 字符串承载
//   @PreAuthorize 鉴权码。
//
// 【设计必要性】把目录、菜单、按钮统一存储在一张表+类型字段，而不是三张表，
//   使得权限字符串在服务加载时一次全量缓存。children 仅在左侧菜单树查询时组装。
//
// 【注意事项】
//   - menuType 对应 MenuType 枚举（0目录/1菜单/2按钮）
//   - permission 字符串约定以 "system:" 等前缀命名。
//   - visible=0 控制前端隐藏，不再控制查询删除
// ============================================================
