package com.permission.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.ResultCode;
import com.permission.common.entity.Novel;
import com.permission.common.entity.NovelChapter;
import com.permission.common.exception.BusinessException;
import com.permission.system.mapper.NovelChapterMapper;
import com.permission.system.mapper.NovelMapper;
import com.permission.system.service.NovelChapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelChapterServiceImpl extends ServiceImpl<NovelChapterMapper, NovelChapter> implements NovelChapterService {

    private final NovelMapper novelMapper;

    @Override
    public List<NovelChapter> listByNovel(Long novelId) {
        return baseMapper.selectList(new LambdaQueryWrapper<NovelChapter>()
                .eq(NovelChapter::getNovelId, novelId)
                .orderByAsc(NovelChapter::getChapterNum));
    }

    @Override
    public NovelChapter getChapterDetail(Long id) {
        NovelChapter chapter = baseMapper.selectById(id);
        if (chapter == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "章节不存在");
        }
        return chapter;
    }

    @Override
    @Transactional
    public void createChapter(NovelChapter chapter) {
        Novel novel = novelMapper.selectById(chapter.getNovelId());
        if (novel == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "小说不存在");
        }
        if (StrUtil.isBlank(chapter.getChapterTitle())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "章节标题不能为空");
        }
        // Auto assign chapter number
        if (chapter.getChapterNum() == null) {
            Long count = baseMapper.selectCount(new LambdaQueryWrapper<NovelChapter>()
                    .eq(NovelChapter::getNovelId, chapter.getNovelId()));
            chapter.setChapterNum((int) (count + 1));
        }
        if (chapter.getIsFree() == null) {
            chapter.setIsFree(1);
        }
        if (StrUtil.isNotBlank(chapter.getContent())) {
            chapter.setWordCount(chapter.getContent().length());
        }
        baseMapper.insert(chapter);

        // Update novel word count and update time
        updateNovelStats(chapter.getNovelId());
    }

    @Override
    @Transactional
    public void updateChapter(NovelChapter chapter) {
        NovelChapter existing = baseMapper.selectById(chapter.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "章节不存在");
        }
        if (StrUtil.isNotBlank(chapter.getContent())) {
            chapter.setWordCount(chapter.getContent().length());
        }
        chapter.setNovelId(null); // don't change novel
        chapter.setChapterNum(null);
        baseMapper.updateById(chapter);
        updateNovelStats(existing.getNovelId());
    }

    @Override
    @Transactional
    public void deleteChapter(Long id) {
        NovelChapter chapter = baseMapper.selectById(id);
        if (chapter == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "章节不存在");
        }
        baseMapper.deleteById(id);
        updateNovelStats(chapter.getNovelId());
    }

    @Override
    public NovelChapter getPrevChapter(Long novelId, Integer chapterNum) {
        return baseMapper.selectOne(new LambdaQueryWrapper<NovelChapter>()
                .eq(NovelChapter::getNovelId, novelId)
                .lt(NovelChapter::getChapterNum, chapterNum)
                .orderByDesc(NovelChapter::getChapterNum)
                .last("LIMIT 1"));
    }

    @Override
    public NovelChapter getNextChapter(Long novelId, Integer chapterNum) {
        return baseMapper.selectOne(new LambdaQueryWrapper<NovelChapter>()
                .eq(NovelChapter::getNovelId, novelId)
                .gt(NovelChapter::getChapterNum, chapterNum)
                .orderByAsc(NovelChapter::getChapterNum)
                .last("LIMIT 1"));
    }

    private void updateNovelStats(Long novelId) {
        List<NovelChapter> chapters = listByNovel(novelId);
        long totalWords = chapters.stream().mapToLong(c -> c.getWordCount() != null ? c.getWordCount() : 0).sum();
        Novel novel = new Novel();
        novel.setId(novelId);
        novel.setWordCount(totalWords);
        novel.setUpdateTime(java.time.LocalDateTime.now());
        novelMapper.updateById(novel);
    }
}
