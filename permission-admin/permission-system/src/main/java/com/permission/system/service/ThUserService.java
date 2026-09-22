package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.dto.LoginDTO;
import com.permission.common.dto.ThProfileDTO;
import com.permission.common.entity.ThComment;
import com.permission.common.entity.ThNotification;
import com.permission.common.entity.ThPost;
import com.permission.common.entity.ThUser;

import java.util.List;
import java.util.Map;

public interface ThUserService extends IService<ThUser> {

    ThUser register(LoginDTO loginDTO);

    Map<String, String> login(LoginDTO loginDTO);

    ThUser getUserById(Long id);

    IPage<ThPost> getPosts(Long userId, long pageNum, long pageSize);

    IPage<ThComment> getMyComments(Long userId, long pageNum, long pageSize);

    IPage<ThComment> getReceivedComments(Long userId, long pageNum, long pageSize);

    IPage<ThNotification> getNotifications(Long userId, long pageNum, long pageSize, Boolean unreadOnly);

    long getUnreadCount(Long userId);

    void markNotificationsRead(Long userId, List<Long> ids);

    /**
     * 更新树洞用户资料 — 使用 DTO 白名单，仅更新允许的字段
     */
    void updateProfile(Long userId, ThProfileDTO dto);

    /**
     * 修改密码 — 校验原密码 + 强密码策略，并使已签发 Token 失效
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 公开的用户主页信息（他人可见，仅返回昵称/头像/简介/统计，不含邮箱等隐私）
     */
    Map<String, Object> getPublicProfile(Long userId);
}
