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
                // Alibaba-Java: 异常日志【强制】异常信息应包括案发现场信息和异常堆栈信息
                log.warn("Failed to persist operation log", logEx);
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
                // Alibaba-Java: 异常日志【强制】异常信息应包括案发现场信息和异常堆栈信息
                log.warn("Failed to persist operation log", logEx);
            }
            throw e;
        }
    }
}

