package com.permission.framework.security;

import com.permission.common.dto.LoginUser;

public interface CustomUserDetailsService {
    LoginUser loadUserById(Long userId);
}
