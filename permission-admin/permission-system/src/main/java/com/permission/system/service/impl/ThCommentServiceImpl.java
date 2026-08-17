package com.permission.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.entity.*;
import com.permission.common.exception.BusinessException;
import com.permission.common.ResultCode;
import com.permission.system.mapper.ThCommentMapper;
import com.permission.system.mapper.ThLikeMapper;
import com.permission.system.mapper.ThNotificationMapper;
import com.permission.system.mapper.ThPostMapper;
import com.permission.system.mapper.ThUserMapper;
import com.permission.system.service.ThCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThCommentServiceImpl extends ServiceImpl<ThCommentMapper, ThComment> implements ThCommentService {

    private final ThCommentMapper commentMapper;
    private final ThPostMapper postMapper;
    private final ThUserMapper userMapper;
    private final ThLikeMapper likeMapper;
    private final ThNotificationMapper notificationMapper;

    @Override
    public IPage<ThComment> pageComments(long pageNum, long pageSize, Long postId) {
        Page<ThComment> page = new Page(pageNum, pageSize);
        LambdaQueryWrapper<ThComment> wrapper = new LambdaQueryWrapper<ThComment>()
                .eq(ThComment::getDeleted, 0)
                .eq(postId != null, ThComment::getPostId, postId)
                .eq(ThComment::getStatus, 1)
                .orderByDesc(ThComment::getCreateTime);

        IPage<ThComment> result = commentMapper.selectPage(page, wrapper);

        for (ThComment comment : result.getRecords()) {
            if (comment.getIsAnonymous() != null && comment.getIsAnonymous() == 1) {
                comment.setAuthorName("匿名用户");
            } else {
                ThUser user = userMapper.selectById(comment.getUserId());
                comment.setAuthorName(user != null ? user.getNickname() : "未知用户");
            }
            // 填充被回复人名称
            if (comment.getReplyUserId() != null) {
                ThUser replyUser = userMapper.selectById(comment.getReplyUserId());
                comment.setReplyUserName(replyUser != null ? replyUser.getNickname() : "未知用户");
            }
        }

        return result;
    }

    @Override
    public void createComment(ThComment comment) {
        if (StrUtil.isBlank(comment.getContent())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论内容不能为空");
        }
        if (comment.getContent().length() > 2000) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论不能超过2000字");
        }
        if (comment.getPostId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "帖子ID不能为空");
        }

        comment.setStatus(1);
        comment.setLikeCount(0);
        comment.setIsAnonymous(comment.getIsAnonymous() != null ? comment.getIsAnonymous() : 0);
        commentMapper.insert(comment);

        // 更新帖子评论数
        postMapper.incrementCommentCount(comment.getPostId());

        // 创建通知（如果评论的不是自己的帖子）
        ThPost post = postMapper.selectById(comment.getPostId());
        if (post != null && post.getUserId() != null && !post.getUserId().equals(comment.getUserId())) {
            ThNotification notification = new ThNotification();
            notification.setUserId(post.getUserId());
            notification.setSenderId(comment.getUserId());
            notification.setType("COMMENT");
            notification.setTargetType("POST");
            notification.setTargetId(comment.getPostId());

            String commenterName = "匿名用户";
            if (comment.getIsAnonymous() == 0) {
                ThUser commenter = userMapper.selectById(comment.getUserId());
                commenterName = commenter != null ? commenter.getNickname() : "有人";
            }
            String content = comment.getContent();
            if (content.length() > 50) content = content.substring(0, 50) + "...";
            notification.setContent(commenterName + " 评论了你的帖子: " + content);
            notification.setIsRead(0);
            notificationMapper.insert(notification);
        }
    }

    @Override
    public void likeComment(Long id, Long userId) {
        Long count = likeMapper.selectCount(new LambdaQueryWrapper<ThLike>()
                .eq(ThLike::getUserId, userId)
                .eq(ThLike::getTargetType, "COMMENT")
                .eq(ThLike::getTargetId, id)
                .eq(ThLike::getDeleted, 0));
        if (count > 0) return;

        ThLike like = new ThLike();
        like.setUserId(userId);
        like.setTargetType("COMMENT");
        like.setTargetId(id);
        likeMapper.insert(like);

        ThComment comment = commentMapper.selectById(id);
        if (comment != null) {
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentMapper.updateById(comment);
        }
    }
}
