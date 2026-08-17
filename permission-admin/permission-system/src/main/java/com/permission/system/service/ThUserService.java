package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.dto.LoginDTO;
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

    void updateProfile(ThUser user);
}
