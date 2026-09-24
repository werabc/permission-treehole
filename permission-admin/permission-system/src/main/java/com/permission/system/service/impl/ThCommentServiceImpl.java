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
import com.permission.system.service.SensitiveWordService;
import com.permission.system.service.ThCommentService;
import com.permission.system.service.ThSettingsService;
import com.permission.system.support.ThRateLimiter;
import com.permission.system.support.ThUserGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SensitiveWordService sensitiveWordService;
    private final ThUserGuard userGuard;
    private final ThRateLimiter rateLimiter;
    private final ThSettingsService settingsService;

    // 最大分页大小限制
    private static final long MAX_PAGE_SIZE = 100;

    @Override
    public IPage<ThComment> pageComments(long pageNum, long pageSize, Long postId) {
        return pageComments(pageNum, pageSize, postId, null);
    }

    @Override
    public IPage<ThComment> pageComments(long pageNum, long pageSize, Long postId, Long currentUserId) {
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

        // 批量填充用户信息与当前用户的点赞态，避免 N+1 查询
        fillCommentExtras(result.getRecords(), currentUserId);

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

        // ========== 内容治理三道闸门（禁言 → 限额 → 敏感词） ==========
        userGuard.checkMuted(comment.getUserId());
        rateLimiter.checkDaily(comment.getUserId(), "comment",
                settingsService.getMaxCommentPerDay(), "今日评论");
        int status = settingsService.isCommentNeedAudit() ? 0 : 1;
        if (sensitiveWordService.isEnabled()) {
            SensitiveWordService.SensitiveHit hit = sensitiveWordService.check(comment.getContent());
            if (hit != null) {
                if (hit.level() == 1) {
                    log.warn("评论命中拦截词 userId={} word={}", comment.getUserId(), hit.word());
                    throw new BusinessException(ResultCode.BAD_REQUEST,
                            "评论包含违规词「" + hit.word() + "」，请修改后重新发送");
                }
                status = 0;
                log.info("评论命中转审词 userId={} word={}，转人工审核", comment.getUserId(), hit.word());
            }
        }

        comment.setStatus(status);
        comment.setLikeCount(0);
        comment.setIsAnonymous(comment.getIsAnonymous() != null ? comment.getIsAnonymous() : 0);
        commentMapper.insert(comment);

        // 更新帖子评论数 + 作者评论数统计
        postMapper.incrementCommentCount(comment.getPostId());
        userMapper.incrementCommentCount(comment.getUserId());

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
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id, Long userId, boolean isAdmin) {
        ThComment comment = commentMapper.selectById(id);
        if (comment == null || comment.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        if (!isAdmin && !java.util.Objects.equals(comment.getUserId(), userId)) {
            throw new BusinessException(ResultCode.DATA_FORBIDDEN, "只能删除自己的评论");
        }

        commentMapper.deleteById(id);
        likeMapper.delete(new LambdaQueryWrapper<ThLike>()
                .eq(ThLike::getTargetType, "COMMENT")
                .eq(ThLike::getTargetId, id));
        postMapper.decrementCommentCount(comment.getPostId());
        if (comment.getUserId() != null) {
            userMapper.decrementCommentCount(comment.getUserId());
        }

        log.info("Deleted comment id={} postId={} operator={} admin={}",
                id, comment.getPostId(), userId, isAdmin);
    }

    @Override
    public void likeComment(Long id, Long userId) {
        userGuard.checkMuted(userId);
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
     * 批量填充评论作者名、头像、被回复人名，以及"当前用户是否已点赞"，避免 N+1 查询
     *
     * @param currentUserId 当前登录用户，未登录传 null；用于回填 liked 状态
     */
    private void fillCommentExtras(List<ThComment> comments, Long currentUserId) {
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

        // 批量查询"我点过赞的评论"，一次 IN 查询解决，避免每条评论一次 count
        Set<Long> likedIds = new HashSet<>();
        if (currentUserId != null) {
            List<Long> commentIds = comments.stream().map(ThComment::getId).toList();
            if (!commentIds.isEmpty()) {
                List<ThLike> likes = likeMapper.selectList(new LambdaQueryWrapper<ThLike>()
                        .eq(ThLike::getUserId, currentUserId)
                        .eq(ThLike::getTargetType, "COMMENT")
                        .eq(ThLike::getDeleted, 0)
                        .in(ThLike::getTargetId, commentIds));
                likes.forEach(l -> likedIds.add(l.getTargetId()));
            }
        }

        // 填充
        for (ThComment comment : comments) {
            if (comment.getIsAnonymous() != null && comment.getIsAnonymous() == 1) {
                comment.setAuthorName("匿名用户");
                comment.setAuthorAvatar(null);   // 匿名不给头像，避免去匿名化
                comment.setUserId(null);         // 匿名评论清除 userId，防止去匿名化
            } else {
                ThUser user = userMap.get(comment.getUserId());
                comment.setAuthorName(user != null ? user.getNickname() : "未知用户");
                comment.setAuthorAvatar(user != null ? user.getAvatar() : null);
            }
            if (comment.getReplyUserId() != null) {
                ThUser replyUser = userMap.get(comment.getReplyUserId());
                comment.setReplyUserName(replyUser != null ? replyUser.getNickname() : "未知用户");
            }
            comment.setLiked(likedIds.contains(comment.getId()));
            // 公开接口不返回敏感字段
            comment.setIp(null);
        }
    }

    @Override
    public void unlikeComment(Long id, Long userId) {
        LambdaQueryWrapper<ThLike> wrapper = new LambdaQueryWrapper<ThLike>()
                .eq(ThLike::getUserId, userId)
                .eq(ThLike::getTargetType, "COMMENT")
                .eq(ThLike::getTargetId, id)
                .eq(ThLike::getDeleted, 0);
        ThLike like = likeMapper.selectOne(wrapper);
        if (like != null) {
            likeMapper.deleteById(like.getId());
            commentMapper.decrementLikeCount(id);
        }
    }
}
