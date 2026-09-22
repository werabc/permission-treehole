package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.ThComment;

public interface ThCommentService extends IService<ThComment> {

    IPage<ThComment> pageComments(long pageNum, long pageSize, Long postId);

    void createComment(ThComment comment);

    /**
     * 删除自己的评论（管理端传 isAdmin=true 可删任意评论）
     * 级联：评论点赞记录 + 帖子评论数 + 作者评论数
     */
    void deleteComment(Long id, Long userId, boolean isAdmin);

    void likeComment(Long id, Long userId);
}
