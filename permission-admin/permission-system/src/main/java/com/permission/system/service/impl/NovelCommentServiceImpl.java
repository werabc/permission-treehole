package com.permission.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.ResultCode;
import com.permission.common.entity.NovelComment;
import com.permission.common.exception.BusinessException;
import com.permission.system.mapper.NovelCommentMapper;
import com.permission.system.service.NovelCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelCommentServiceImpl extends ServiceImpl<NovelCommentMapper, NovelComment> implements NovelCommentService {

    @Override
    @Transactional
    public void addComment(NovelComment comment) {
        if (StrUtil.isBlank(comment.getContent())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论内容不能为空");
        }
        if (comment.getParentId() == null) {
            comment.setParentId(0L);
        }
        if (comment.getLikeCount() == null) {
            comment.setLikeCount(0);
        }
        baseMapper.insert(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long id, Long userId) {
        NovelComment comment = baseMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己的评论");
        }
        baseMapper.deleteById(id);
    }

    @Override
    public List<NovelComment> listByNovel(Long novelId) {
        List<NovelComment> all = baseMapper.selectList(new LambdaQueryWrapper<NovelComment>()
                .eq(NovelComment::getNovelId, novelId)
                .orderByDesc(NovelComment::getCreateTime));

        // Build tree: top-level comments with children
        List<NovelComment> roots = all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .collect(Collectors.toList());
        List<NovelComment> replies = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() > 0)
                .collect(Collectors.toList());

        roots.forEach(root -> {
            List<NovelComment> children = replies.stream()
                    .filter(r -> r.getParentId().equals(root.getId()))
                    .collect(Collectors.toList());
            root.setChildren(children);
        });
        return roots;
    }
}
