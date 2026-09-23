package com.permission.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.entity.ThCategory;
import com.permission.system.mapper.ThCategoryMapper;
import com.permission.system.mapper.ThPostMapper;
import com.permission.system.service.ThCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ThCategoryServiceImpl extends ServiceImpl<ThCategoryMapper, ThCategory> implements ThCategoryService {

    private final ThPostMapper postMapper;

    @Override
    public List<ThCategory> listEnabled() {
        List<ThCategory> list = baseMapper.selectList(new LambdaQueryWrapper<ThCategory>()
                .eq(ThCategory::getDeleted, 0)
                .eq(ThCategory::getStatus, 1)
                .orderByAsc(ThCategory::getSort));

        // post_count 列没人维护，这里改成实时聚合，保证「全部」与各分类的数字口径一致
        Map<Long, Integer> counts = new HashMap<>();
        for (Map<String, Object> row : postMapper.countVisibleGroupByCategory()) {
            Object id = row.get("categoryId");
            Object cnt = row.get("cnt");
            if (id instanceof Number && cnt instanceof Number) {
                counts.put(((Number) id).longValue(), ((Number) cnt).intValue());
            }
        }
        list.forEach(c -> c.setPostCount(counts.getOrDefault(c.getId(), 0)));
        return list;
    }
}
