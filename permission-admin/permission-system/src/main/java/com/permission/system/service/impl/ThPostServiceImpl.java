package com.permission.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.entity.*;
import com.permission.common.exception.BusinessException;
import com.permission.common.ResultCode;
import com.permission.system.mapper.ThCategoryMapper;
import com.permission.system.mapper.ThLikeMapper;
import com.permission.system.mapper.ThNotificationMapper;
import com.permission.system.mapper.ThPostMapper;
import com.permission.system.mapper.ThUserMapper;
import com.permission.system.service.ThPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThPostServiceImpl extends ServiceImpl<ThPostMapper, ThPost> implements ThPostService {

    private final ThPostMapper postMapper;
    private final ThCategoryMapper categoryMapper;
    private final ThUserMapper userMapper;
    private final ThLikeMapper likeMapper;
    private final ThNotificationMapper notificationMapper;

    @Override
    public IPage<ThPost> pagePosts(long pageNum, long pageSize, Long categoryId, String keyword, Integer status) {
        Page<ThPost> page = new Page(pageNum, pageSize);
        LambdaQueryWrapper<ThPost> wrapper = new LambdaQueryWrapper<ThPost>()
                .eq(ThPost::getDeleted, 0)
                .eq(status != null, ThPost::getStatus, status)
                .eq(categoryId != null, ThPost::getCategoryId, categoryId)
                .like(StrUtil.isNotBlank(keyword), ThPost::getContent, keyword)
                .orderByDesc(ThPost::getIsTop)
                .orderByDesc(ThPost::getCreateTime);

        IPage<ThPost> result = postMapper.selectPage(page, wrapper);

        // 填充作者名和分类名
        for (ThPost post : result.getRecords()) {
            if (post.getIsAnonymous() != null && post.getIsAnonymous() == 1) {
                post.setAuthorName("匿名用户");
            } else {
                ThUser user = userMapper.selectById(post.getUserId());
                post.setAuthorName(user != null ? user.getNickname() : "未知用户");
            }
            if (post.getCategoryId() != null) {
                ThCategory cat = categoryMapper.selectById(post.getCategoryId());
                post.setCategoryName(cat != null ? cat.getName() : "未分类");
            }
        }

        return result;
    }

    @Override
    public void createPost(ThPost post) {
        if (StrUtil.isBlank(post.getContent())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "内容不能为空");
        }
        if (post.getContent().length() > 5000) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "内容不能超过5000字");
        }
        post.setStatus(0); // 待审核
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setReportCount(0);
        post.setIsTop(0);
        post.setIsAnonymous(post.getIsAnonymous() != null ? post.getIsAnonymous() : 0);
        postMapper.insert(post);
    }

    @Override
    public void likePost(Long id, Long userId) {
        ThPost post = postMapper.selectById(id);
        if (post == null) throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");

        // 检查是否已点赞
        Long count = likeMapper.selectCount(new LambdaQueryWrapper<ThLike>()
                .eq(ThLike::getUserId, userId)
                .eq(ThLike::getTargetType, "POST")
                .eq(ThLike::getTargetId, id)
                .eq(ThLike::getDeleted, 0));
        if (count > 0) return; // 已点赞

        ThLike like = new ThLike();
        like.setUserId(userId);
        like.setTargetType("POST");
        like.setTargetId(id);
        likeMapper.insert(like);

        postMapper.incrementLikeCount(id);

        // 创建通知（如果不是点赞自己的帖子）
        if (post.getUserId() != null && !post.getUserId().equals(userId)) {
            ThNotification notification = new ThNotification();
            notification.setUserId(post.getUserId());
            notification.setSenderId(userId);
            notification.setType("LIKE");
            notification.setTargetType("POST");
            notification.setTargetId(id);

            ThUser liker = userMapper.selectById(userId);
            String likerName = liker != null ? liker.getNickname() : "有人";
            notification.setContent(likerName + " 点赞了你的帖子");
            notification.setIsRead(0);
            notificationMapper.insert(notification);
        }
    }

    @Override
    public void unlikePost(Long id, Long userId) {
        LambdaQueryWrapper<ThLike> wrapper = new LambdaQueryWrapper<ThLike>()
                .eq(ThLike::getUserId, userId)
                .eq(ThLike::getTargetType, "POST")
                .eq(ThLike::getTargetId, id)
                .eq(ThLike::getDeleted, 0);
        ThLike like = likeMapper.selectOne(wrapper);
        if (like != null) {
            like.setDeleted(1);
            likeMapper.updateById(like);
            postMapper.decrementLikeCount(id);
        }
    }

    @Override
    public boolean isLiked(Long id, Long userId) {
        return likeMapper.selectCount(new LambdaQueryWrapper<ThLike>()
                .eq(ThLike::getUserId, userId)
                .eq(ThLike::getTargetType, "POST")
                .eq(ThLike::getTargetId, id)
                .eq(ThLike::getDeleted, 0)) > 0;
    }

    @Override
    public void incrementViewCount(Long id) {
        postMapper.incrementViewCount(id);
    }
}
