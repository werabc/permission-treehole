package com.permission.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.ResultCode;
import com.permission.common.entity.Novel;
import com.permission.common.entity.NovelCategory;
import com.permission.common.entity.NovelChapter;
import com.permission.common.exception.BusinessException;
import com.permission.system.mapper.NovelChapterMapper;
import com.permission.system.mapper.NovelMapper;
import com.permission.system.mapper.NovelCategoryMapper;
import com.permission.system.service.NovelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelServiceImpl extends ServiceImpl<NovelMapper, Novel> implements NovelService {

    private final NovelCategoryMapper categoryMapper;
    private final NovelChapterMapper chapterMapper;

    @Override
    public IPage<Novel> pageNovels(long pageNum, long pageSize, String keyword, Long categoryId, Integer status, Long authorId) {
        Page<Novel> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Novel> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Novel::getTitle, keyword).or().like(Novel::getAuthorName, keyword));
        }
        if (categoryId != null) {
            wrapper.eq(Novel::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Novel::getStatus, status);
        }
        if (authorId != null) {
            wrapper.eq(Novel::getAuthorId, authorId);
        }
        wrapper.orderByDesc(Novel::getUpdateTime);
        IPage<Novel> result = baseMapper.selectPage(page, wrapper);
        fillNovelExtras(result.getRecords());
        return result;
    }

    @Override
    public IPage<Novel> pagePublishedNovels(long pageNum, long pageSize, String keyword, Long categoryId) {
        return pageNovels(pageNum, pageSize, keyword, categoryId, null, null);
    }

    @Override
    public Novel getNovelDetail(Long id) {
        Novel novel = baseMapper.selectById(id);
        if (novel == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "小说不存在");
        }
        // Increment click count
        Novel update = new Novel();
        update.setId(id);
        update.setClickCount(novel.getClickCount() + 1);
        baseMapper.updateById(update);
        novel.setClickCount(novel.getClickCount() + 1);

        fillNovelExtras(Collections.singletonList(novel));
        return novel;
    }

    @Override
    @Transactional
    public void createNovel(Novel novel, Long authorId, String authorName) {
        novel.setAuthorId(authorId);
        novel.setAuthorName(authorName);
        if (novel.getStatus() == null) {
            novel.setStatus(1);
        }
        if (novel.getWordCount() == null) {
            novel.setWordCount(0L);
        }
        if (novel.getClickCount() == null) {
            novel.setClickCount(0L);
        }
        if (novel.getLikeCount() == null) {
            novel.setLikeCount(0L);
        }
        baseMapper.insert(novel);
    }

    @Override
    @Transactional
    public void updateNovel(Novel novel, Long authorId) {
        Novel existing = baseMapper.selectById(novel.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "小说不存在");
        }
        if (!existing.getAuthorId().equals(authorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能修改自己的小说");
        }
        novel.setAuthorId(null); // don't change author
        novel.setAuthorName(null);
        novel.setClickCount(null);
        novel.setLikeCount(null);
        novel.setWordCount(null);
        baseMapper.updateById(novel);
    }

    @Override
    @Transactional
    public void deleteNovels(List<Long> ids, Long userId) {
        if (CollUtil.isNotEmpty(ids)) {
            for (Long id : ids) {
                Novel novel = baseMapper.selectById(id);
                if (novel != null && !novel.getAuthorId().equals(userId)) {
                    throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己的小说");
                }
            }
            baseMapper.deleteBatchIds(ids);
        }
    }

    private void fillNovelExtras(List<Novel> novels) {
        if (CollUtil.isEmpty(novels)) return;
        // Fill category names
        Set<Long> categoryIds = novels.stream().map(Novel::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (CollUtil.isNotEmpty(categoryIds)) {
            List<NovelCategory> categories = categoryMapper.selectBatchIds(categoryIds);
            Map<Long, String> catMap = categories.stream().collect(Collectors.toMap(NovelCategory::getId, NovelCategory::getCategoryName));
            novels.forEach(n -> {
                if (n.getCategoryId() != null) {
                    n.setCategoryName(catMap.getOrDefault(n.getCategoryId(), ""));
                }
            });
        }
        // Fill last chapter title
        Set<Long> novelIds = novels.stream().map(Novel::getId).collect(Collectors.toSet());
        for (Long novelId : novelIds) {
            LambdaQueryWrapper<NovelChapter> wrapper = new LambdaQueryWrapper<NovelChapter>()
                    .eq(NovelChapter::getNovelId, novelId)
                    .orderByDesc(NovelChapter::getChapterNum)
                    .last("LIMIT 1");
            NovelChapter lastChapter = chapterMapper.selectOne(wrapper);
            novels.stream().filter(n -> n.getId().equals(novelId)).forEach(n -> {
                if (lastChapter != null) {
                    n.setLastChapterTitle(lastChapter.getChapterTitle());
                }
            });
        }
    }
}
