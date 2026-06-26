package com.permission.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.ResultCode;
import com.permission.common.entity.Novel;
import com.permission.common.entity.NovelChapter;
import com.permission.common.entity.UserBookshelf;
import com.permission.common.exception.BusinessException;
import com.permission.system.mapper.NovelChapterMapper;
import com.permission.system.mapper.NovelMapper;
import com.permission.system.mapper.UserBookshelfMapper;
import com.permission.system.service.UserBookshelfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBookshelfServiceImpl extends ServiceImpl<UserBookshelfMapper, UserBookshelf> implements UserBookshelfService {

    private final NovelMapper novelMapper;
    private final NovelChapterMapper chapterMapper;

    @Override
    @Transactional
    public void addToBookshelf(Long userId, Long novelId) {
        UserBookshelf exist = baseMapper.selectOne(new LambdaQueryWrapper<UserBookshelf>()
                .eq(UserBookshelf::getUserId, userId)
                .eq(UserBookshelf::getNovelId, novelId));
        if (exist != null) {
            return; // already in bookshelf
        }
        Novel novel = novelMapper.selectById(novelId);
        if (novel == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "小说不存在");
        }
        UserBookshelf shelf = new UserBookshelf();
        shelf.setUserId(userId);
        shelf.setNovelId(novelId);
        baseMapper.insert(shelf);

        // Increment novel like count
        Novel update = new Novel();
        update.setId(novelId);
        update.setLikeCount((novel.getLikeCount() != null ? novel.getLikeCount() : 0) + 1);
        novelMapper.updateById(update);
    }

    @Override
    @Transactional
    public void removeFromBookshelf(Long userId, Long novelId) {
        baseMapper.delete(new LambdaQueryWrapper<UserBookshelf>()
                .eq(UserBookshelf::getUserId, userId)
                .eq(UserBookshelf::getNovelId, novelId));
    }

    @Override
    public boolean isInBookshelf(Long userId, Long novelId) {
        return baseMapper.selectCount(new LambdaQueryWrapper<UserBookshelf>()
                .eq(UserBookshelf::getUserId, userId)
                .eq(UserBookshelf::getNovelId, novelId)) > 0;
    }

    @Override
    public List<UserBookshelf> listByUser(Long userId) {
        List<UserBookshelf> list = baseMapper.selectList(new LambdaQueryWrapper<UserBookshelf>()
                .eq(UserBookshelf::getUserId, userId)
                .orderByDesc(UserBookshelf::getCreateTime));
        if (list.isEmpty()) return list;

        // Fill novel info
        Set<Long> novelIds = list.stream().map(UserBookshelf::getNovelId).collect(Collectors.toSet());
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
