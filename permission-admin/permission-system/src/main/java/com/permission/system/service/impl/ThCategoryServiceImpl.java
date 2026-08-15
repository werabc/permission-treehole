package com.permission.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.entity.ThCategory;
import com.permission.system.mapper.ThCategoryMapper;
import com.permission.system.service.ThCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ThCategoryServiceImpl extends ServiceImpl<ThCategoryMapper, ThCategory> implements ThCategoryService {

    @Override
    public List<ThCategory> listEnabled() {
        return baseMapper.selectList(new LambdaQueryWrapper<ThCategory>()
                .eq(ThCategory::getDeleted, 0)
                .eq(ThCategory::getStatus, 1)
                .orderByAsc(ThCategory::getSort));
    }
}
