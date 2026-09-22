package com.permission.framework.security;

import com.permission.common.dto.LoginUser;

public interface CustomUserDetailsService {
    LoginUser loadUserById(Long userId);

    /**
     * 根据用户ID和类型加载用户
     * @param userId 用户ID
     * @param isTreehole true=树洞端用户, false=管理端用户
     * @return LoginUser
     */
    LoginUser loadUserById(Long userId, boolean isTreehole);
}

