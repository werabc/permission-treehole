package com.permission.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.NovelChapter;

import java.util.List;

public interface NovelChapterService extends IService<NovelChapter> {

    List<NovelChapter> listByNovel(Long novelId);

    NovelChapter getChapterDetail(Long id);

    void createChapter(NovelChapter chapter);

    void updateChapter(NovelChapter chapter);

    void deleteChapter(Long id);

    NovelChapter getPrevChapter(Long novelId, Integer chapterNum);

    NovelChapter getNextChapter(Long novelId, Integer chapterNum);
}
