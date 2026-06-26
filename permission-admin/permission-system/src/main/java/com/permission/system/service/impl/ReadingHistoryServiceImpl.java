package com.permission.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.entity.Novel;
import com.permission.common.entity.ReadingHistory;
import com.permission.system.mapper.NovelMapper;
import com.permission.system.mapper.ReadingHistoryMapper;
import com.permission.system.service.ReadingHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingHistoryServiceImpl extends ServiceImpl<ReadingHistoryMapper, ReadingHistory> implements ReadingHistoryService {

    private final NovelMapper novelMapper;

    @Override
    @Transactional
    public void saveOrUpdateHistory(Long userId, Long novelId, Long chapterId, String chapterTitle) {
        ReadingHistory history = baseMapper.selectOne(new LambdaQueryWrapper<ReadingHistory>()
                .eq(ReadingHistory::getUserId, userId)
                .eq(ReadingHistory::getNovelId, novelId));
        if (history == null) {
            history = new ReadingHistory();
            history.setUserId(userId);
            history.setNovelId(novelId);
            history.setChapterId(chapterId);
            history.setChapterTitle(chapterTitle);
            baseMapper.insert(history);
        } else {
            history.setChapterId(chapterId);
            history.setChapterTitle(chapterTitle);
            baseMapper.updateById(history);
        }
    }

    @Override
    public List<ReadingHistory> listByUser(Long userId, int limit) {
        LambdaQueryWrapper<ReadingHistory> wrapper = new LambdaQueryWrapper<ReadingHistory>()
                .eq(ReadingHistory::getUserId, userId)
                .orderByDesc(ReadingHistory::getUpdateTime);
        if (limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        List<ReadingHistory> list = baseMapper.selectList(wrapper);
        if (list.isEmpty()) return list;

        // Fill novel info
        Set<Long> novelIds = list.stream().map(ReadingHistory::getNovelId).collect(Collectors.toSet());
        List<Novel> novels = novelMapper.selectBatchIds(novelIds);
        Map<Long, Novel> novelMap = novels.stream().collect(Collectors.toMap(Novel::getId, n -> n));
        list.forEach(item -> {
            Novel novel = novelMap.get(item.getNovelId());
            if (novel != null) {
                item.setNovelTitle(novel.getTitle());
                item.setCoverUrl(novel.getCoverUrl());
                item.setAuthorName(novel.getAuthorName());
            }
        });
        return list;
    }
}
