package com.permission.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.entity.ThComment;
import com.permission.common.entity.ThLike;
import com.permission.common.entity.ThPost;
import com.permission.system.mapper.ThCommentMapper;
import com.permission.system.mapper.ThLikeMapper;
import com.permission.system.mapper.ThPostMapper;
import com.permission.system.service.ThCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThCommentServiceImpl extends ServiceImpl<ThCommentMapper, ThComment> implements ThCommentService {

    private final ThCommentMapper commentMapper;
    private final ThPostMapper postMapper;
    private final ThLikeMapper likeMapper;

    @Override
    public IPage<ThComment> pageComments(long pageNum, long pageSize, Long postId) {
        Page<ThComment> page = new Page(pageNum, pageSize);
        LambdaQueryWrapper<ThComment> wrapper = new LambdaQueryWrapper<ThComment>()
                .eq(ThComment::getDeleted, 0)
                .eq(postId != null, ThComment::getPostId, postId)
                .eq(ThComment::getStatus, 1)
                .orderByDesc(ThComment::getCreateTime);
        return commentMapper.selectPage(page, wrapper);
    }

    @Override
    public void createComment(ThComment comment) {
        comment.setStatus(1);
        comment.setLikeCount(0);
        comment.setIsAnonymous(comment.getIsAnonymous() != null ? comment.getIsAnonymous() : 1);
        save(comment);

        // 更新帖子评论数
        ThPost post = postMapper.selectById(comment.getPostId());
        if (post != null) {
            post.setCommentCount(post.getCommentCount() + 1);
            postMapper.updateById(post);
        }
    }

    @Override
    public void likeComment(Long id, String userId) {
        ThComment comment = getById(id);
        if (comment == null) return;

        Long count = likeMapper.selectCount(new LambdaQueryWrapper<ThLike>()
                .eq(ThLike::getTargetType, "COMMENT")
                .eq(ThLike::getTargetId, id)
                .eq(ThLike::getIp, userId)
                .eq(ThLike::getDeleted, 0));

        if (count > 0) return;

        ThLike like = new ThLike();
        like.setTargetType("COMMENT");
        like.setTargetId(id);
        like.setIp(userId);
        likeMapper.insert(like);

        comment.setLikeCount(comment.getLikeCount() + 1);
        updateById(comment);
    }
}
