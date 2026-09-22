package com.permission.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.ResultCode;
import com.permission.common.entity.ThCollect;
import com.permission.common.entity.ThPost;
import com.permission.common.exception.BusinessException;
import com.permission.system.mapper.ThCollectMapper;
import com.permission.system.mapper.ThPostMapper;
import com.permission.system.service.ThCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 收藏服务
 *
 * 并发说明：th_collect 上有 uk_user_target 唯一键，重复插入会抛 DuplicateKeyException，
 * 这里捕获后按"已收藏"处理，保证接口幂等（不会因并发点击报 500）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThCollectServiceImpl extends ServiceImpl<ThCollectMapper, ThCollect> implements ThCollectService {

    private final ThCollectMapper collectMapper;
    private final ThPostMapper postMapper;

    @Override
    public boolean toggle(Long userId, Long postId) {
        ThPost post = postMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        ThCollect existing = find(userId, postId);
        if (existing != null) {
            collectMapper.deleteById(existing.getId());
            log.debug("取消收藏 userId={} postId={}", userId, postId);
            return false;
        }

        ThCollect collect = new ThCollect();
        collect.setUserId(userId);
        collect.setTargetType("POST");
        collect.setTargetId(postId);
        try {
            collectMapper.insert(collect);
            log.debug("收藏成功 userId={} postId={}", userId, postId);
        } catch (DuplicateKeyException e) {
            // 并发重复收藏，视为已收藏
            log.debug("重复收藏已忽略 userId={} postId={}", userId, postId);
        }
        return true;
    }

    @Override
    public boolean isCollected(Long userId, Long postId) {
        if (userId == null || postId == null) return false;
        return find(userId, postId) != null;
    }

    @Override
    public long countByUser(Long userId) {
        return collectMapper.selectCount(new LambdaQueryWrapper<ThCollect>()
                .eq(ThCollect::getUserId, userId)
                .eq(ThCollect::getTargetType, "POST")
                .eq(ThCollect::getDeleted, 0));
    }

    private ThCollect find(Long userId, Long postId) {
        return collectMapper.selectOne(new LambdaQueryWrapper<ThCollect>()
                .eq(ThCollect::getUserId, userId)
                .eq(ThCollect::getTargetType, "POST")
                .eq(ThCollect::getTargetId, postId)
                .eq(ThCollect::getDeleted, 0)
                .last("LIMIT 1"));
    }
}
