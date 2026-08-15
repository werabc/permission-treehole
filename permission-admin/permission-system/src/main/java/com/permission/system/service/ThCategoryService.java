package com.permission.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.ThCategory;

import java.util.List;

public interface ThCategoryService extends IService<ThCategory> {

    List<ThCategory> listEnabled();
}
