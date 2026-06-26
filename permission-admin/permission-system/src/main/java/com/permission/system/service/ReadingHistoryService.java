package com.permission.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.ReadingHistory;

import java.util.List;

public interface ReadingHistoryService extends IService<ReadingHistory> {

    void saveOrUpdateHistory(Long userId, Long novelId, Long chapterId, String chapterTitle);

    List<ReadingHistory> listByUser(Long userId, int limit);
}
