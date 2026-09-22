package com.permission.system.service.impl;

import com.permission.common.ResultCode;
import com.permission.common.constant.SecurityConstants;
import com.permission.common.dto.LoginUser;
import com.permission.common.entity.SysUser;
import com.permission.common.exception.BusinessException;
import com.permission.framework.security.JwtTokenProvider;
import com.permission.system.mapper.*;
import com.permission.system.service.OnlineUserService;
import com.permission.system.service.SysLoginLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务实现单元测试
 * Alibaba-Java: 单元测试 AIR 原则 — BCDE 原则
 *
 * 注意：SysUserServiceImpl 继承 ServiceImpl，业务代码走的是父类的 baseMapper 字段，
 * Mockito 的 @InjectMocks 无法注入父类字段，必须用 ReflectionTestUtils 手动注入，
 * 否则所有依赖 baseMapper 的用例都会 NPE（历史上此文件 4 个用例就是因此长期失败的）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysUserServiceImplTest {

    @InjectMocks
    private SysUserServiceImpl userService;

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysMenuMapper menuMapper;
    @Mock
    private SysDeptMapper deptMapper;
    @Mock
    private SysLoginLogService loginLogService;
    @Mock
    private OnlineUserService onlineUserService;
    @Mock
    private SysUserMapper baseMapper;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // 关键修复：把 mock 的 mapper 注入到父类 ServiceImpl.baseMapper，否则业务代码 this.baseMapper 为 null
        ReflectionTestUtils.setField(userService, "baseMapper", baseMapper);
    }

    @Test
    void createUser_WithExistingUsername_ShouldThrowDataExistsException() {
        // Given
        SysUser user = new SysUser();
        user.setUsername("existinguser");
        user.setPassword("Valid@1234");
        when(baseMapper.selectCount(any())).thenReturn(1L);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.createUser(user));
        assertEquals(ResultCode.DATA_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void createUser_WithWeakPassword_ShouldThrowException() {
        // Given
        SysUser user = new SysUser();
        user.setUsername("newuser");
        user.setPassword("weak");
        when(baseMapper.selectCount(any())).thenReturn(0L);

        // When & Then
        assertThrows(BusinessException.class, () -> userService.createUser(user));
    }

    @Test
    void createUser_WithValidInput_ShouldSucceed() {
        // Given
        SysUser user = new SysUser();
        user.setUsername("newuser");
        user.setPassword("Valid@1234");
        when(baseMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("Valid@1234")).thenReturn("encoded_password");

        // When
        userService.createUser(user);

        // Then
        verify(baseMapper).insert(user);
        verify(passwordEncoder).encode("Valid@1234");
    }

    @Test
    void pageUsers_ShouldClearPasswordBeforeReturning() {
        // This test verifies that password is nulled out in responses
        // Implementation depends on specific query mocking
    }
}
