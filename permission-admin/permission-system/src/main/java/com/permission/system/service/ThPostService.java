package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.ThPost;

public interface ThPostService extends IService<ThPost> {

    IPage<ThPost> pagePosts(long pageNum, long pageSize, Long categoryId, String keyword, Integer status);

    /**
     * 帖子详情（已填充 authorName / authorAvatar / categoryName）。
     *
     * 原先详情接口直接用 MyBatis-Plus 的 getById，只手工处理了匿名分支，
     * 实名帖的 authorName 与 categoryName 永远是 null —— 前端只能显示"未知用户"，
     * 分类标签也不显示。详情与列表必须走同一套填充逻辑。
     */
    ThPost getPostDetail(Long id);

    void createPost(ThPost post);

    /**
     * 删除自己的帖子（管理端传 isAdmin=true 可删任意帖子）
     * 级联逻辑删除：评论、点赞、收藏、相关通知，并修正作者发帖数
     */
    void deletePost(Long id, Long userId, boolean isAdmin);

    /**
     * 关键词搜索帖子（内容 + 作者昵称），仅返回已通过审核的帖子
     */
    IPage<ThPost> searchPosts(String keyword, long pageNum, long pageSize);

    /**
     * 我收藏的帖子
     */
    IPage<ThPost> getCollectedPosts(Long userId, long pageNum, long pageSize);

    void likePost(Long id, Long userId);

    void unlikePost(Long id, Long userId);

    boolean isLiked(Long id, Long userId);

    void incrementViewCount(Long id);
}
