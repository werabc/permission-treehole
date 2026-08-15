package com.permission.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误"),
    USER_ACCOUNT_LOCKED(1002, "账号已被锁定"),
    USER_ACCOUNT_DISABLED(1003, "账号已被禁用"),
    TOKEN_EXPIRED(1004, "Token已过期"),
    TOKEN_INVALID(1005, "Token无效"),
    OLD_PASSWORD_ERROR(1006, "原密码错误"),
    CAPTCHA_ERROR(1007, "验证码错误"),
    PASSWORD_WEAK(1008, "密码强度不足：至少8位，包含大小写字母、数字和特殊字符"),
    RATE_LIMITED(1009, "请求过于频繁，请稍后重试"),
    ACCOUNT_TEMP_LOCKED(1010, "账号已被临时锁定，请30分钟后再试"),
    ROLE_NAME_EXISTS(2001, "角色名称已存在"),
    ROLE_CODE_EXISTS(2002, "角色编码已存在"),
    MENU_NAME_EXISTS(3001, "菜单名称已存在"),
    DEPT_NAME_EXISTS(4001, "部门名称已存在"),
    DEPT_HAS_CHILDREN(4002, "部门存在子部门，无法删除"),
    DEPT_HAS_USERS(4003, "部门下存在用户，无法删除"),
    ROLE_HAS_USERS(2003, "角色已分配用户，无法删除"),
    DATA_EXISTS(5001, "数据已存在"),
    DATA_FORBIDDEN(5002, "无权操作此数据");

    private final int code;
    private final String message;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.ResultCode
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Getter — Lombok，仅生成 getter（code/message 已 final 无需 setter）
//   - @AllArgsConstructor — Lombok，自动生成 (int code, String message) 构造器，
//     供枚举常量 SUCCESS(200,"...") 这种声明语法使用
//   - 枚举实现 — Java enum，天然单例，线程安全
//
// 【关联文件】
//   - 被 R.ok() / R.fail() 指定失败编码时引用 → R.java
//   - 被 GlobalExceptionHandler 捕获异常时映射为 R.fail(resultCode) → framework/.../GlobalExceptionHandler.java
//   - 被 BusinessException 携带编码 → BusinessException.java
//
// 【核心作用】集中管理业务状态码与提示文案，避免魔法数字散落。
//
// 【设计必要性】用枚举而不是常量类，既提供类型约束（只能传合法 ResultCode），
//   又能在 R/BusinessException/异常处理中统一使用，新增编码只需加一行。
//
// 【注意事项】
//   - 错误码按模块分段：10xx 认证用户、20xx 角色、30xx 菜单、40xx 部门、50xx 通用
//   - message 是面向用户的中文提示，不要写技术栈细节
// ============================================================
