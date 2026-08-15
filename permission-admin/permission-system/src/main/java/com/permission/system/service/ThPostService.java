package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.ThPost;

public interface ThPostService extends IService<ThPost> {

    IPage<ThPost> pagePosts(long pageNum, long pageSize, Long categoryId, String keyword, Integer status);

    void createPost(ThPost post);

    void likePost(Long id, Long userId);

    void unlikePost(Long id, Long userId);

    boolean isLiked(Long id, Long userId);

    void incrementViewCount(Long id);
}
