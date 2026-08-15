package com.permission.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MenuType {
    CATALOG(0, "目录"),
    MENU(1, "菜单"),
    BUTTON(2, "按钮");

    private final int code;
    private final String desc;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.enums.MenuType
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Getter — Lombok，生成 getter
//   - @AllArgsConstructor — Lombok 给枚举 (code, desc) 构造器
//   - enum — Java 枚举单例
//
// 【关联文件】
//   - 被 SysMenu.menuType 引用，对应 TINYINT 列 → entity/SysMenu.java
//   - 前端路由根据 menuType 决定路由/按钮渲染形态
//
// 【核心作用】目录/菜单/按钮三级资源分类。
//
// 【设计必要性】三类资源统一存于 sys_menu，通过类型字段减少表数量；
//   后端在加载权限树、生成菜单树与鉴权按钮时据此区分行为。
//
// 【注意事项】
//   - code 与数据库 TINYINT 严格对应，切勿随意改值
//   - BUTTON 不参与左侧菜单树渲染
// ============================================================
