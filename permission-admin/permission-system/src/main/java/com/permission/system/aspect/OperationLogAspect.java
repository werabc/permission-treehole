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
            logEntry.setRequestParams(JSONUtil.toJsonStr(args));

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
            operationLogMapper.insert(logEntry);

            return result;
        } catch (Throwable e) {
            logEntry.setExecuteTime(System.currentTimeMillis() - startTime);
            logEntry.setStatus(0);
            logEntry.setErrorMsg(e.getMessage());
            logEntry.setCreateTime(LocalDateTime.now());
            operationLogMapper.insert(logEntry);
            throw e;
        }
    }
}
