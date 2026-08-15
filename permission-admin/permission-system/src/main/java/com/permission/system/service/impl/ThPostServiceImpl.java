package com.permission.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.entity.ThCategory;
import com.permission.common.entity.ThLike;
import com.permission.common.entity.ThPost;
import com.permission.system.mapper.ThCategoryMapper;
import com.permission.system.mapper.ThLikeMapper;
import com.permission.system.mapper.ThPostMapper;
import com.permission.system.service.ThPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThPostServiceImpl extends ServiceImpl<ThPostMapper, ThPost> implements ThPostService {

    private final ThPostMapper postMapper;
    private final ThCategoryMapper categoryMapper;
    private final ThLikeMapper likeMapper;

    @Override
    public IPage<ThPost> pagePosts(long pageNum, long pageSize, Long categoryId, String keyword) {
        Page<ThPost> page = new Page(pageNum, pageSize);
        LambdaQueryWrapper<ThPost> wrapper = new LambdaQueryWrapper<ThPost>()
                .eq(ThPost::getDeleted, 0)
                .eq(ThPost::getStatus, 1)
                .eq(categoryId != null, ThPost::getCategoryId, categoryId);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(ThPost::getContent, keyword);
        }
        wrapper.orderByDesc(ThPost::getIsTop).orderByDesc(ThPost::getCreateTime);

        IPage<ThPost> result = postMapper.selectPage(page, wrapper);

        // 填充分类名称
        List<Long> categoryIds = result.getRecords().stream()
                .map(ThPost::getCategoryId).distinct().filter(id -> id != null).toList();
        if (!categoryIds.isEmpty()) {
            LambdaQueryWrapper<ThCategory> catWrapper = new LambdaQueryWrapper<ThCategory>()
                    .in(ThCategory::getId, categoryIds);
            categoryMapper.selectList(catWrapper).forEach(cat ->
                    result.getRecords().stream()
                            .filter(p -> cat.getId().equals(p.getCategoryId()))
                            .forEach(p -> p.setCategoryName(cat.getName())));
        }

        return result;
    }

    @Override
    public void createPost(ThPost post) {
        post.setStatus(0); // 默认待审核
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setIsAnonymous(1); // 默认匿名
        post.setIsTop(0);
        save(post);
    }

    @Override
    public void likePost(Long id, String ip) {
        ThPost post = getById(id);
        if (post == null) return;

        // 检查是否已点赞
        Long count = likeMapper.selectCount(new LambdaQueryWrapper<ThLike>()
                .eq(ThLike::getTargetType, "POST")
                .eq(ThLike::getTargetId, id)
                .eq(ThLike::getIp, ip)
                .eq(ThLike::getDeleted, 0));

        if (count > 0) return; // 已点赞

        ThLike like = new ThLike();
        like.setTargetType("POST");
        like.setTargetId(id);
        like.setIp(ip);
        likeMapper.insert(like);

        post.setLikeCount(post.getLikeCount() + 1);
        updateById(post);
    }

    @Override
    public void unlikePost(Long id, String ip) {
        ThPost post = getById(id);
        if (post == null) return;

        // 删除点赞记录
        LambdaQueryWrapper<ThLike> wrapper = new LambdaQueryWrapper<ThLike>()
                .eq(ThLike::getTargetType, "POST")
                .eq(ThLike::getTargetId, id)
                .eq(ThLike::getIp, ip);
        ThLike like = likeMapper.selectOne(wrapper);
        if (like != null) {
            like.setDeleted(1);
            likeMapper.updateById(like);
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            updateById(post);
        }
    }

    @Override
    public boolean isLiked(Long id, String ip) {
        return likeMapper.selectCount(new LambdaQueryWrapper<ThLike>()
                .eq(ThLike::getTargetType, "POST")
                .eq(ThLike::getTargetId, id)
                .eq(ThLike::getIp, ip)
                .eq(ThLike::getDeleted, 0)) > 0;
    }

    @Override
    public void incrementViewCount(Long id) {
        ThPost post = getById(id);
        if (post == null) return;
        post.setViewCount(post.getViewCount() + 1);
        updateById(post);
    }
}
