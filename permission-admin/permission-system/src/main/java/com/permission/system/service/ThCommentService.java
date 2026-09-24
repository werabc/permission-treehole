package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.ThComment;

public interface ThCommentService extends IService<ThComment> {

    IPage<ThComment> pageComments(long pageNum, long pageSize, Long postId);

    /**
     * 带当前登录用户的分页：额外回填 liked（"我是否已点赞"）。
     * 不返回用户态的话前端只能靠本地猜，刷新后点赞状态就丢了。
     */
    IPage<ThComment> pageComments(long pageNum, long pageSize, Long postId, Long currentUserId);

    void createComment(ThComment comment);

    /**
     * 删除自己的评论（管理端传 isAdmin=true 可删任意评论）
     * 级联：评论点赞记录 + 帖子评论数 + 作者评论数
     */
    void deleteComment(Long id, Long userId, boolean isAdmin);

    void likeComment(Long id, Long userId);

    /** 取消点赞评论（幂等：未点赞时静默返回） */
    void unlikeComment(Long id, Long userId);
}
