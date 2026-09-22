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
import com.permission.system.mapper.ThCollectMapper;
import com.permission.system.mapper.ThCommentMapper;
import com.permission.system.mapper.ThLikeMapper;
import com.permission.system.mapper.ThNotificationMapper;
import com.permission.system.mapper.ThPostMapper;
import com.permission.system.mapper.ThUserMapper;
import com.permission.system.service.SensitiveWordService;
import com.permission.system.service.ThPostService;
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
public class ThPostServiceImpl extends ServiceImpl<ThPostMapper, ThPost> implements ThPostService {

    private final ThPostMapper postMapper;
    private final ThCategoryMapper categoryMapper;
    private final ThUserMapper userMapper;
    private final ThLikeMapper likeMapper;
    private final ThNotificationMapper notificationMapper;
    private final ThCommentMapper commentMapper;
    private final ThCollectMapper collectMapper;
    private final SensitiveWordService sensitiveWordService;
    private final ThUserGuard userGuard;
    private final ThRateLimiter rateLimiter;
    private final ThSettingsService settingsService;

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

        // ========== 内容治理三道闸门（禁言 → 限额 → 敏感词） ==========
        // 1. 禁言/封号校验
        userGuard.checkMuted(post.getUserId());
        // 2. 每日发帖限额
        rateLimiter.checkDaily(post.getUserId(), "post",
                settingsService.getMaxPostPerDay(), "今日发帖");
        // 3. 敏感词分级处理：L1 直接拦截，L2 转人工审核
        int status = settingsService.isPostNeedAudit() ? 0 : 1;
        if (sensitiveWordService.isEnabled()) {
            SensitiveWordService.SensitiveHit hit = sensitiveWordService.check(post.getContent());
            if (hit != null) {
                if (hit.level() == 1) {
                    log.warn("发帖命中拦截词 userId={} word={}", post.getUserId(), hit.word());
                    throw new BusinessException(ResultCode.BAD_REQUEST,
                            "内容包含违规词「" + hit.word() + "」，请修改后重新发布");
                }
                status = 0;
                log.info("发帖命中转审词 userId={} word={}，转人工审核", post.getUserId(), hit.word());
            }
        }

        post.setStatus(status);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setReportCount(0);
        post.setIsTop(0);
        post.setIsAnonymous(post.getIsAnonymous() != null ? post.getIsAnonymous() : 0);
        postMapper.insert(post);

        // 维护作者统计（原实现遗漏，导致个人主页发帖数永远为 0）
        userMapper.incrementPostCount(post.getUserId());

        log.info("Created post id={} userId={} status={}", post.getId(), post.getUserId(), status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id, Long userId, boolean isAdmin) {
        ThPost post = postMapper.selectById(id);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!isAdmin && !Objects.equals(post.getUserId(), userId)) {
            throw new BusinessException(ResultCode.DATA_FORBIDDEN, "只能删除自己发布的帖子");
        }

        // 1. 逻辑删除帖子本体
        postMapper.deleteById(id);
        // 2. 级联逻辑删除评论
        commentMapper.delete(new LambdaQueryWrapper<ThComment>().eq(ThComment::getPostId, id));
        // 3. 级联删除点赞记录
        likeMapper.delete(new LambdaQueryWrapper<ThLike>()
                .eq(ThLike::getTargetType, "POST")
                .eq(ThLike::getTargetId, id));
        // 4. 级联删除收藏记录，避免收藏夹出现"幽灵帖子"
        collectMapper.delete(new LambdaQueryWrapper<ThCollect>()
                .eq(ThCollect::getTargetType, "POST")
                .eq(ThCollect::getTargetId, id));
        // 5. 清理指向该帖的通知（th_notification 无逻辑删除字段，物理删除）
        notificationMapper.delete(new LambdaQueryWrapper<ThNotification>()
                .eq(ThNotification::getTargetType, "POST")
                .eq(ThNotification::getTargetId, id));
        // 6. 修正作者发帖数
        if (post.getUserId() != null) {
            userMapper.decrementPostCount(post.getUserId());
        }

        log.info("Deleted post id={} operator={} admin={}", id, userId, isAdmin);
    }

    @Override
    public IPage<ThPost> searchPosts(String keyword, long pageNum, long pageSize) {
        if (StrUtil.isBlank(keyword)) {
            return new Page<>(pageNum, Math.min(pageSize, MAX_PAGE_SIZE));
        }
        if (pageSize > MAX_PAGE_SIZE) pageSize = MAX_PAGE_SIZE;

        // 转义 LIKE 通配符，防止通配符注入
        String safeKeyword = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");

        // 作者昵称命中的用户ID（昵称在 th_user 表，先查出候选作者）
        List<Long> authorIds = userMapper.selectList(new LambdaQueryWrapper<ThUser>()
                        .eq(ThUser::getDeleted, 0)
                        .like(ThUser::getNickname, safeKeyword))
                .stream().map(ThUser::getId).collect(Collectors.toList());

        Page<ThPost> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ThPost> wrapper = new LambdaQueryWrapper<ThPost>()
                .eq(ThPost::getDeleted, 0)
                .eq(ThPost::getStatus, 1)
                .and(w -> {
                    w.like(ThPost::getContent, safeKeyword);
                    if (!authorIds.isEmpty()) {
                        w.or().in(ThPost::getUserId, authorIds);
                    }
                })
                .orderByDesc(ThPost::getCreateTime);

        IPage<ThPost> result = postMapper.selectPage(page, wrapper);
        fillPostExtras(result.getRecords());
        return result;
    }

    @Override
    public IPage<ThPost> getCollectedPosts(Long userId, long pageNum, long pageSize) {
        if (pageSize > MAX_PAGE_SIZE) pageSize = MAX_PAGE_SIZE;

        // 先分页查收藏记录，再按收藏顺序取帖子，保证"最近收藏在最前"
        Page<ThCollect> collectPage = new Page<>(pageNum, pageSize);
        IPage<ThCollect> collects = collectMapper.selectPage(collectPage,
                new LambdaQueryWrapper<ThCollect>()
                        .eq(ThCollect::getUserId, userId)
                        .eq(ThCollect::getTargetType, "POST")
                        .eq(ThCollect::getDeleted, 0)
                        .orderByDesc(ThCollect::getCreateTime));

        Page<ThPost> result = new Page<>(pageNum, pageSize, collects.getTotal());
        if (collects.getRecords().isEmpty()) {
            return result;
        }

        List<Long> postIds = collects.getRecords().stream()
                .map(ThCollect::getTargetId).collect(Collectors.toList());
        Map<Long, ThPost> postMap = postMapper.selectBatchIds(postIds).stream()
                .filter(p -> p.getDeleted() == 0 && p.getStatus() == 1)
                .collect(Collectors.toMap(ThPost::getId, p -> p));

        List<ThPost> ordered = postIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        fillPostExtras(ordered);
        result.setRecords(ordered);
        return result;
    }

    @Override
    public void likePost(Long id, Long userId) {
        userGuard.checkMuted(userId);
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
                post.setUserId(null);  // 匿名帖子清除 userId，防止去匿名化
            } else {
                ThUser user = userMap.get(post.getUserId());
                post.setAuthorName(user != null ? user.getNickname() : "未知用户");
            }
            if (post.getCategoryId() != null) {
                ThCategory cat = categoryMap.get(post.getCategoryId());
                post.setCategoryName(cat != null ? cat.getName() : "未分类");
            }
            // 公开接口不返回敏感字段
            post.setIp(null);
            post.setAuditRemark(null);
            post.setAuditorId(null);
        }
    }
}
