package com.permission.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.Novel;

public interface NovelService extends IService<Novel> {

    IPage<Novel> pageNovels(long pageNum, long pageSize, String keyword, Long categoryId, Integer status, Long authorId);

    IPage<Novel> pagePublishedNovels(long pageNum, long pageSize, String keyword, Long categoryId);

    Novel getNovelDetail(Long id);

    void createNovel(Novel novel, Long authorId, String authorName);

    void updateNovel(Novel novel, Long authorId);

    void deleteNovels(java.util.List<Long> ids, Long userId);
}
