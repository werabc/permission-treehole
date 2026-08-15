package com.permission.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataScope {
    ALL(1, "全部数据"),
    DEPT_AND_SUB(2, "本部门及子部门"),
    DEPT(3, "本部门"),
    CUSTOM(4, "自定义"),
    SELF(5, "仅本人");

    private final int code;
    private final String desc;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.enums.DataScope
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Getter — Lombok，生成 getter
//   - @AllArgsConstructor — Lombok 给枚举 (code, desc) 提供构造器
//   - enum — Java 枚举单例
//
// 【关联文件】
//   - 被 SysRole.dataScope 引用 → entity/SysRole.java
//   - 被 LoginUser.dataScope 放入 Security 上下文 → dto/LoginUser.java
//   - 被 DataScopeAspect 注解+切面解析为 SQL 过滤条件 → framework/aspect/DataScopeAspect.java
//
// 【核心作用】数据范围枚举，决定角色能查看的数据层级。
//
// 【设计必要性】数据权限与业务权限解耦：数据权限是"看哪些行"，业务权限是"看哪些按钮"。
//   枚举值存入角色后，由 DataScopeAspect 在 SQL 层动态追加 WHERE 条件。
//
// 【注意事项】
//   - CUSTOM(4) 对应角色 deptIds 列表中的部门
//   - SELF(5) 只允许读取本人 creator 的数据
// ============================================================
