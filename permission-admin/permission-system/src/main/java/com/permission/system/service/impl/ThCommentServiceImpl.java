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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThCommentServiceImpl extends ServiceImpl<ThCommentMapper, ThComment> implements ThCommentService {

    private final ThCommentMapper commentMapper;
    private final ThPostMapper postMapper;
    private final ThUserMapper userMapper;
    private final ThLikeMapper likeMapper;
    private final ThNotificationMapper notificationMapper;

    // 最大分页大小限制
    private static final long MAX_PAGE_SIZE = 100;

    @Override
    public IPage<ThComment> pageComments(long pageNum, long pageSize, Long postId) {
        // 限制分页大小
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
            log.warn("Page size exceeds maximum, capped to {}", MAX_PAGE_SIZE);
        }

        Page<ThComment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ThComment> wrapper = new LambdaQueryWrapper<ThComment>()
                .eq(ThComment::getDeleted, 0)
                .eq(postId != null, ThComment::getPostId, postId)
                .eq(ThComment::getStatus, 1)
                .orderByDesc(ThComment::getCreateTime);

        IPage<ThComment> result = commentMapper.selectPage(page, wrapper);

        // 批量填充用户信息，避免 N+1 查询
        fillCommentExtras(result.getRecords());

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

        // 验证帖子是否存在
        ThPost post = postMapper.selectById(comment.getPostId());
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "帖子不存在");
        }

        comment.setStatus(1);
        comment.setLikeCount(0);
        comment.setIsAnonymous(comment.getIsAnonymous() != null ? comment.getIsAnonymous() : 0);
        commentMapper.insert(comment);

        // 更新帖子评论数
        postMapper.incrementCommentCount(comment.getPostId());

        // 创建通知（如果评论的不是自己的帖子）
        if (post.getUserId() != null && !post.getUserId().equals(comment.getUserId())) {
            try {
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
                if (content != null && content.length() > 50) content = content.substring(0, 50) + "...";
                notification.setContent(commenterName + " 评论了你的帖子: " + content);
                notification.setIsRead(0);
                notificationMapper.insert(notification);
                log.debug("Created COMMENT notification for user={} from={}", post.getUserId(), comment.getUserId());
            } catch (Exception e) {
                // Alibaba-Java: 异常日志【强制】异常信息应包括案发现场信息和异常堆栈信息
                log.error("Failed to create COMMENT notification for postId={}", comment.getPostId(), e);
            }
        }

        log.info("Created comment id={} postId={} userId={}", comment.getId(), comment.getPostId(), comment.getUserId());
    }

    @Override
    public void likeComment(Long id, Long userId) {
        ThComment comment = commentMapper.selectById(id);
        if (comment == null || comment.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }

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

        // 使用原子操作递增点赞数
        commentMapper.incrementLikeCount(id);
        log.debug("Liked comment={} by user={}", id, userId);
    }

    /**
     * 批量填充评论作者名和被回复人名，避免 N+1 查询
     */
    private void fillCommentExtras(List<ThComment> comments) {
        if (comments == null || comments.isEmpty()) return;

        // 收集所有用户ID
        Set<Long> userIds = new HashSet<>();
        for (ThComment comment : comments) {
            if (comment.getIsAnonymous() == null || comment.getIsAnonymous() != 1) {
                if (comment.getUserId() != null) userIds.add(comment.getUserId());
            }
            if (comment.getReplyUserId() != null) userIds.add(comment.getReplyUserId());
        }

        // 批量查询用户
        Map<Long, ThUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<ThUser> users = userMapper.selectBatchIds(userIds);
            for (ThUser user : users) {
                userMap.put(user.getId(), user);
            }
        }

        // 填充
        for (ThComment comment : comments) {
            if (comment.getIsAnonymous() != null && comment.getIsAnonymous() == 1) {
                comment.setAuthorName("匿名用户");
                comment.setUserId(null);  // 匿名评论清除 userId，防止去匿名化
            } else {
                ThUser user = userMap.get(comment.getUserId());
                comment.setAuthorName(user != null ? user.getNickname() : "未知用户");
            }
            if (comment.getReplyUserId() != null) {
                ThUser replyUser = userMap.get(comment.getReplyUserId());
                comment.setReplyUserName(replyUser != null ? replyUser.getNickname() : "未知用户");
            }
            // 公开接口不返回敏感字段
            comment.setIp(null);
        }
    }
}
