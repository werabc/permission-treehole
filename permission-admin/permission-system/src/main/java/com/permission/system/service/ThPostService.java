package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.ThPost;

import java.util.List;

public interface ThPostService extends IService<ThPost> {

    IPage<ThPost> pagePosts(long pageNum, long pageSize, Long categoryId, String keyword);

    void createPost(ThPost post);

    void likePost(Long id, String userId);

    void unlikePost(Long id, String userId);

    boolean isLiked(Long id, String userId);

    void incrementViewCount(Long id);
}
