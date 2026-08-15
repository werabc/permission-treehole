package com.permission.system.aspect;

import cn.hutool.json.JSONUtil;
import com.permission.common.annotation.OperationLog;
import com.permission.common.dto.LoginUser;
import com.permission.common.entity.SysOperationLog;
import com.permission.system.mapper.SysOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperationLogMapper operationLogMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        SysOperationLog logEntry = new SysOperationLog();
        logEntry.setModule(operationLog.module());
        logEntry.setAction(operationLog.value());

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        logEntry.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                logEntry.setRequestUrl(request.getRequestURI());
                logEntry.setRequestMethod(request.getMethod());
                logEntry.setOperatorIp(request.getRemoteAddr());
            }

            Object[] args = joinPoint.getArgs();
            String paramsJson = JSONUtil.toJsonStr(args);
            // Sanitize sensitive fields
            paramsJson = paramsJson.replaceAll("\"(password|oldPassword|newPassword|confirmPassword|captchaCode)\":\"[^\"]*\"", "\"$1\":\"***\"");
            // Truncate if too long (prevent DB overflow)
            if (paramsJson.length() > 4000) {
                paramsJson = paramsJson.substring(0, 4000) + "...[truncated]";
            }
            logEntry.setRequestParams(paramsJson);

            org.springframework.security.core.Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
                logEntry.setOperator(loginUser.getUsername());
            }

            Object result = joinPoint.proceed();

            logEntry.setExecuteTime(System.currentTimeMillis() - startTime);
            logEntry.setResponseResult(JSONUtil.toJsonStr(result));
            logEntry.setStatus(1);
            logEntry.setCreateTime(LocalDateTime.now());
            try {
                operationLogMapper.insert(logEntry);
            } catch (Exception logEx) {
                log.warn("Failed to persist operation log: {}", logEx.getMessage());
            }

            return result;
        } catch (Throwable e) {
            logEntry.setExecuteTime(System.currentTimeMillis() - startTime);
            logEntry.setStatus(0);
            logEntry.setErrorMsg(e.getMessage());
            logEntry.setCreateTime(LocalDateTime.now());
            try {
                operationLogMapper.insert(logEntry);
            } catch (Exception logEx) {
                log.warn("Failed to persist operation log: {}", logEx.getMessage());
            }
            throw e;
        }
    }
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.aspect.OperationLogAspect
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Slf4j — Lombok，注入日志对象
//   - @Aspect — Spring AOP，声明切面组件
//   - @Component — Spring，注册为容器组件
//   - @RequiredArgsConstructor — Lombok，生成必需参数构造器（实现构造器注入）
//   - @Around("@annotation(operationLog)") — Spring AOP 环绕通知，拦截 @OperationLog 注解的方法
//   - MethodSignature — AspectJ，获取被拦截方法的签名信息
//   - RequestContextHolder / ServletRequestAttributes — Spring，从请求上下文获取 HttpServletRequest
//   - SecurityContextHolder — Spring Security，从安全上下文获取当前用户信息
//   - JSONUtil — Hutool，JSON 序列化（请求参数/响应结果）
//
// 【关键依赖】
//   - 依赖 SysOperationLogMapper → 持久化操作日志到 sys_operation_log 表
//   - 依赖 OperationLog 注解（自定义） → 仅拦截标注了该注解的控制器方法
//   - 依赖 LoginUser DTO → 获取当前登录人用户名
//
// 【关联文件】
//   - 被 OperationLog 注解声明拦截点（com.permission.common.annotation.OperationLog）
//   - 拦截 AuthController/DeptController/MenuController/RoleController/UserController/LogController
//   - 依赖 SysOperationLog 实体（数据载体）
//
// 【核心作用】
//   通过 AOP 环绕通知机制，自动记录标注了 @OperationLog 的接口的操作日志，包括：请求
//   参数、请求 URL/Method、操作者 IP、执行耗时、响应结果、成功/失败状态、错误信息。
//
// 【设计必要性】
//   操作日志逻辑与业务逻辑解耦：Controller 只需加注解即可记录日志，避免在每个业务方法中
//   手写日志代码，统一格式、降低耦合、便于审计。
//
// 【注意事项/安全提示】
//   - 参数脱敏：密码/旧密码/新密码/确认密码/验证码字段值会被替换为 ***，防止明文落库
//   - 参数截断：超长参数（>4000 字符）会被截断并追加 标记，防止数据库字段溢出
//   - 日志失败不中断请求：log insert 异常只 warn 记录，不抛出，保证业务请求不受日志写入失败影响
//   - 异常分支也会记录日志（status=0 + errorMsg），确保失败操作同样可审计
// ============================================================
