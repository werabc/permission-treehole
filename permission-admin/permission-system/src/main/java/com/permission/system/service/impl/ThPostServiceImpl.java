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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThPostServiceImpl extends ServiceImpl<ThPostMapper, ThPost> implements ThPostService {

    private final ThPostMapper postMapper;
    private final ThCategoryMapper categoryMapper;
    private final ThUserMapper userMapper;
    private final ThLikeMapper likeMapper;
    private final ThNotificationMapper notificationMapper;

    // 最大分页大小限制
    private static final long MAX_PAGE_SIZE = 100;

    @Override
    public IPage<ThPost> pagePosts(long pageNum, long pageSize, Long categoryId, String keyword, Integer status) {
        // 限制分页大小，防止恶意请求
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
            log.warn("Page size exceeds maximum, capped to {}", MAX_PAGE_SIZE);
        }

        Page<ThPost> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ThPost> wrapper = new LambdaQueryWrapper<ThPost>()
                .eq(ThPost::getDeleted, 0)
                .eq(status != null, ThPost::getStatus, status)
                .eq(categoryId != null, ThPost::getCategoryId, categoryId)
                .orderByDesc(ThPost::getIsTop)
                .orderByDesc(ThPost::getCreateTime);
        // Alibaba-Java: 安全规约 — 转义LIKE通配符，防止通配符注入
        if (StrUtil.isNotBlank(keyword)) {
            String safeKeyword = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            wrapper.like(ThPost::getContent, safeKeyword);
        }

        IPage<ThPost> result = postMapper.selectPage(page, wrapper);

        // 批量填充作者名和分类名，避免 N+1 查询
        fillPostExtras(result.getRecords());

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
        // 验证分类是否存在
        if (post.getCategoryId() != null) {
            ThCategory category = categoryMapper.selectById(post.getCategoryId());
            if (category == null || category.getDeleted() == 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "分类不存在");
            }
        }
        post.setStatus(1); // 默认审核通过（可手动打回）
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setReportCount(0);
        post.setIsTop(0);
        post.setIsAnonymous(post.getIsAnonymous() != null ? post.getIsAnonymous() : 0);
        postMapper.insert(post);
        log.info("Created post id={} userId={}", post.getId(), post.getUserId());
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
            try {
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
                log.debug("Created LIKE notification for user={} from={}", post.getUserId(), userId);
            } catch (Exception e) {
                // Alibaba-Java: 异常日志【强制】异常信息应包括案发现场信息和异常堆栈信息
                log.error("Failed to create LIKE notification for postId={}", id, e);
            }
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
            likeMapper.deleteById(like.getId());
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

    /**
     * 批量填充帖子作者名和分类名，避免 N+1 查询
     */
    private void fillPostExtras(List<ThPost> posts) {
        if (posts == null || posts.isEmpty()) return;

        // 收集所有用户ID和分类ID
        Set<Long> userIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        for (ThPost post : posts) {
            if (post.getIsAnonymous() == null || post.getIsAnonymous() != 1) {
                if (post.getUserId() != null) userIds.add(post.getUserId());
            }
            if (post.getCategoryId() != null) categoryIds.add(post.getCategoryId());
        }

        // 批量查询用户
        Map<Long, ThUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<ThUser> users = userMapper.selectBatchIds(userIds);
            for (ThUser user : users) {
                userMap.put(user.getId(), user);
            }
        }

        // 批量查询分类
        Map<Long, ThCategory> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<ThCategory> categories = categoryMapper.selectBatchIds(categoryIds);
            for (ThCategory cat : categories) {
                categoryMap.put(cat.getId(), cat);
            }
        }

        // 填充
        for (ThPost post : posts) {
            if (post.getIsAnonymous() != null && post.getIsAnonymous() == 1) {
                post.setAuthorName("匿名用户");
            } else {
                ThUser user = userMap.get(post.getUserId());
                post.setAuthorName(user != null ? user.getNickname() : "未知用户");
            }
            if (post.getCategoryId() != null) {
                ThCategory cat = categoryMap.get(post.getCategoryId());
                post.setCategoryName(cat != null ? cat.getName() : "未分类");
            }
        }
    }
}
