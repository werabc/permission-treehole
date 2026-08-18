package com.permission.framework.handler;

import com.permission.common.R;
import com.permission.common.ResultCode;
import com.permission.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全局异常处理器单元测试
 * Alibaba-Java: 单元测试 AIR 原则
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleBusinessException_ShouldReturnFailResponse() {
        BusinessException exception = new BusinessException(ResultCode.DATA_EXISTS, "数据已存在");
        R<Void> response = exceptionHandler.handleBusinessException(exception);

        assertNotNull(response);
        assertEquals(ResultCode.DATA_EXISTS.getCode(), response.getCode());
        assertEquals("数据已存在", response.getMessage());
    }

    @Test
    void handleAccessDeniedException_ShouldReturnForbiddenResponse() {
        AccessDeniedException exception = new AccessDeniedException("无权限访问");
        R<Void> response = exceptionHandler.handleAccessDeniedException(exception);

        assertNotNull(response);
        assertEquals(ResultCode.FORBIDDEN.getCode(), response.getCode());
    }

    @Test
    void handleAuthenticationException_ShouldReturnUnauthorizedResponse() {
        AuthenticationCredentialsNotFoundException exception =
                new AuthenticationCredentialsNotFoundException("未登录");
        R<Void> response = exceptionHandler.handleAuthenticationException(exception);

        assertNotNull(response);
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), response.getCode());
    }

    @Test
    void handleException_ShouldReturnInternalErrorResponse() {
        Exception exception = new RuntimeException("系统错误");
        R<Void> response = exceptionHandler.handleException(exception);

        assertNotNull(response);
        assertEquals(ResultCode.INTERNAL_ERROR.getCode(), response.getCode());
    }
}
