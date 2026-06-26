package com.permission.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.NovelCategory;

public interface NovelCategoryService extends IService<NovelCategory> {

    void createCategory(NovelCategory category);

    void updateCategory(NovelCategory category);

    void deleteCategories(java.util.List<Long> ids);
}
