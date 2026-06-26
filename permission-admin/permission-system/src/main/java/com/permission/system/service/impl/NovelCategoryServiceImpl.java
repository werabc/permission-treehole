package com.permission.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.ResultCode;
import com.permission.common.entity.NovelCategory;
import com.permission.common.exception.BusinessException;
import com.permission.system.mapper.NovelCategoryMapper;
import com.permission.system.service.NovelCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelCategoryServiceImpl extends ServiceImpl<NovelCategoryMapper, NovelCategory> implements NovelCategoryService {

    @Override
    @Transactional
    public void createCategory(NovelCategory category) {
        if (StrUtil.isBlank(category.getCategoryName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分类名称不能为空");
        }
        boolean exists = exists(new LambdaQueryWrapper<NovelCategory>()
                .eq(NovelCategory::getCategoryName, category.getCategoryName()));
        if (exists) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "分类名称已存在");
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        baseMapper.insert(category);
    }

    @Override
    @Transactional
    public void updateCategory(NovelCategory category) {
        NovelCategory existing = baseMapper.selectById(category.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }
        if (!existing.getCategoryName().equals(category.getCategoryName())) {
            boolean nameExists = exists(new LambdaQueryWrapper<NovelCategory>()
                    .eq(NovelCategory::getCategoryName, category.getCategoryName()));
            if (nameExists) {
                throw new BusinessException(ResultCode.DATA_EXISTS, "分类名称已存在");
            }
        }
        baseMapper.updateById(category);
    }

    @Override
    @Transactional
    public void deleteCategories(List<Long> ids) {
        if (CollUtil.isNotEmpty(ids)) {
            baseMapper.deleteBatchIds(ids);
        }
    }
}
