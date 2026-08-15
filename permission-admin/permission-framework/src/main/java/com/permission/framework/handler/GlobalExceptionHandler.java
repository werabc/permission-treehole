package com.permission.framework.handler;

import com.permission.common.R;
import com.permission.common.ResultCode;
import com.permission.common.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception: code={}, msg={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return R.fail(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return R.fail(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
        return R.fail(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e) {
        return R.fail(ResultCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMissingParamException(MissingServletRequestParameterException e) {
        return R.fail(ResultCode.BAD_REQUEST, "缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("Internal server error", e);
        return R.fail(ResultCode.INTERNAL_ERROR);
    }
}
// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.framework.handler.GlobalExceptionHandler
// 【模块】permission-framework（安全与基础设施模块）
//
// 【使用的注解/技术】
//   - @Slf4j — Lombok，自动生成 log 字段
//   - @RestControllerAdvice — Spring MVC，全局控制器异常切面 + @ResponseBody，拦截所有 @Controller/@RestController
//   - @ExceptionHandler(XxxException.class) — Spring MVC，按异常类型匹配处理方法
//   - @ResponseStatus(HttpStatus.XXX) — 显式指定 HTTP 状态码兜底（同时 R 体中携带业务码）
//
// 【关键依赖/注入】
//   - 依赖 R — permission-common 模块统一响应包装，提供 fail(code,message) / ok(data)
//   - 依赖 ResultCode — 枚举类提供 FORBIDDEN / BAD_REQUEST / INTERNAL_ERROR 等标准错误码
//   - 捕获 BusinessException — 业务异常并直接使用其 getCode() 字段作为响应业务码
//   - 捕获 AccessDeniedException — Spring Security 鉴权失败抛出的异常，返回 403
//
// 【关联文件】
//   - 被 permission-api 模块启动类扫描装配（包扫描 com.permission.framework.handler）
//   - 与 permission-common 模块的 R、ResultCode、BusinessException 紧密耦合
//   - 为前端 axios 拦截器提供统一的错误码处理约定
//
// 【核心作用】将全应用所有未捕获异常转为统一的 R 响应 JSON，避免框架默认的 HTML 错误页或堆栈直出。
//
// 【设计必要性】
//   - 前后端分离架构下浏览器期望 JSON 而非 Tomcat 默认错误页；
//   - BusinessException 保留业务码实现分级告警（warn 级别），系统异常走 error 级别，日志检索更高效；
//   - 可在此统一给 500 错误发送告警（邮件/钉钉），无需业务层重复处理。
//
// 【注意事项/安全提示】
//   - ⚠️ 生产环境切不可在此处将完整堆栈（e.printStackTrace）返回给前端，避免泄露路径与 SQL 片段；
//   - 当前仅堆栈通过 log.error 写入日志文件，对外仅返回 ResultCode.INTERNAL_ERROR 文案；
//   - 新增业务异常类型时，在此添加对应 @ExceptionHandler 方法；
//   - @ResponseStatus 与 R.fail 中业务码同时存在，HTTP 状态码供网关限流/统计，业务码供前端精确判断。
// ============================================================
