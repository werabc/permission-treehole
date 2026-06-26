package com.permission.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.NovelComment;

import java.util.List;

public interface NovelCommentService extends IService<NovelComment> {

    void addComment(NovelComment comment);

    void deleteComment(Long id, Long userId);

    List<NovelComment> listByNovel(Long novelId);
}
