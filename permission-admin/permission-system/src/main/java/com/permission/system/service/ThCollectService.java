package com.permission.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.ThCollect;

public interface ThCollectService extends IService<ThCollect> {

    /**
     * 切换收藏状态
     *
     * @return true=已收藏，false=已取消收藏
     */
    boolean toggle(Long userId, Long postId);

    /** 是否已收藏 */
    boolean isCollected(Long userId, Long postId);

    /** 收藏总数 */
    long countByUser(Long userId);
}
