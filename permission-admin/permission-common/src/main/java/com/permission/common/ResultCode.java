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

