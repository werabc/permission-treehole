package com.permission.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus {
    ENABLED(1, "启用"),
    DISABLED(0, "禁用");

    private final int code;
    private final String desc;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.enums.UserStatus
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Getter — Lombok，生成 getter
//   - @AllArgsConstructor — Lombok 给枚举 (code, desc) 构造器
//   - enum — Java 枚举单例
//
// 【关联文件】
//   - 被 SysUser.status 字段引用 → entity/SysUser.java
//   - 登录时由 AuthServiceImpl 比对账户状态 → service/impl/AuthServiceImpl.java
//   - 被 Service 层切换启用/禁用状态时使用
//
// 【核心作用】账号启用/禁用状态枚举。
//
// 【设计必要性】禁用账号（而不是删除）可以保留数据完整，保留用户 ID 历史引用，
//   一个字段加枚举即可实现"软逻辑删除用户"。
//
// 【注意事项】
//   - code 与 sys_user.status 列严格一致
//   - 禁用后登录应被拦截，提示 ACCOUNT_DISABLED（1003）
// ============================================================
