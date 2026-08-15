package com.permission.common.exception;

import com.permission.common.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.exception.BusinessException
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Getter — Lombok，生成 code 字段的 getter（code 已 final 无需 setter）
//   - extends RuntimeException — 非受检异常，默认会触发 Spring 事务回滚
//
// 【关联文件】
//   - 各业务 Service 抛出 → service/*ServiceImpl.java
//   - 被 GlobalExceptionHandler 捕获后转为 R.fail(code, msg) → framework/.../GlobalExceptionHandler.java
//   - 引用 ResultCode 编码 → ResultCode.java
//   - 与 R.fail(ResultCode) 形成对称的失败语义
//
// 【核心作用】业务异常基类，携带错误 code，由全局异常处理器转换为统一响应。
//
// 【设计必要性】区别于系统异常，业务异常有稳定 code 让前端展示固定文案并做跳转；
//   继承 RuntimeException 使得出错自动回滚且不需要每个方法 throws 声明。
//
// 【注意事项】
//   - 建议都以 ResultCode 构造，避免随意 code 导致前端无法识别
//   - 传 cause 便于日志定位根因
// ============================================================
