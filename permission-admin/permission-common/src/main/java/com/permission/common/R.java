package com.permission.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok() {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> R<T> fail() {
        return new R<>(ResultCode.INTERNAL_ERROR.getCode(), ResultCode.INTERNAL_ERROR.getMessage(), null);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(ResultCode.INTERNAL_ERROR.getCode(), message, null);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> R<T> fail(ResultCode resultCode, String message) {
        return new R<>(resultCode.getCode(), message, null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.R
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Data — Lombok，生成 getter/setter/toString/equals/hashCode
//   - @NoArgsConstructor / @AllArgsConstructor — Lombok，无参/全参构造，
//     全参构造用于静态工厂方法中的 new R<>(...)
//   - implements Serializable — 统一响应体可序列化
//
// 【关联文件】
//   - 引用 ResultCode 编码 → ResultCode.java
//   - Controller 层把业务结果封装为 R<T> 返回前端
//   - GlobalExceptionHandler 在捕获异常后也返回 R<?> 格式
//   - 前端 axios 拦截器统一解析 R 的 code/message/data
//
// 【核心作用】全局统一响应结构，所有 Controller 最终都返回 R<T>。
//
// 【设计必要性】泛型 R<T> 让成功时携带 data、失败时只携带 message，配合静态工厂
//   ok()/fail() 极大简化 Controller 书写；Serializable 保证 Redis 或 Feign 传输安全。
//
// 【注意事项】
//   - 默认 ok()/fail() 绑定的编码来自 ResultCode.SUCCESS/INTERNAL_ERROR，
//     需要自定义编码时请使用 fail(ResultCode) / fail(int, String) 重载
// ============================================================
