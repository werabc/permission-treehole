package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.ThComment;

public interface ThCommentService extends IService<ThComment> {

    IPage<ThComment> pageComments(long pageNum, long pageSize, Long postId);

    void createComment(ThComment comment);

    void likeComment(Long id, Long userId);
}
