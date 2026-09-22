package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.dto.LoginDTO;
import com.permission.common.dto.ProfileDTO;
import com.permission.common.dto.TokenVO;
import com.permission.common.entity.SysUser;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Set;

public interface SysUserService extends IService<SysUser> {

    TokenVO login(LoginDTO loginDTO);

    TokenVO refreshToken(String refreshToken);

    void logout(String token);

    IPage<SysUser> pageUsers(long pageNum, long pageSize, String keyword, Long deptId, Integer status);

    SysUser getUserById(Long id);

    void createUser(SysUser user);

    void updateUser(SysUser user);

    void deleteUsers(List<Long> ids);

    void updateStatus(Long id, Integer status);

    void resetPassword(Long id, String newPassword);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    void assignRoles(Long userId, Set<Long> roleIds);

    Set<Long> getUserRoleIds(Long userId);

    void updateProfile(Long userId, ProfileDTO profileDTO);

    void exportUsers(HttpServletResponse response) throws java.io.IOException;

    void batchUpdateStatus(List<Long> ids, Integer status);
}

