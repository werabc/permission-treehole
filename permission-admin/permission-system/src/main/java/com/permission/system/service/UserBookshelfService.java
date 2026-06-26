package com.permission.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.UserBookshelf;

import java.util.List;

public interface UserBookshelfService extends IService<UserBookshelf> {

    void addToBookshelf(Long userId, Long novelId);

    void removeFromBookshelf(Long userId, Long novelId);

    boolean isInBookshelf(Long userId, Long novelId);

    List<UserBookshelf> listByUser(Long userId);
}
