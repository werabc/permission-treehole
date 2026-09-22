package com.permission.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.SysDept;

import java.util.List;

public interface SysDeptService extends IService<SysDept> {

    List<SysDept> getDeptTree(String keyword, Integer status);

    SysDept getDeptById(Long id);

    void createDept(SysDept dept);

    void updateDept(SysDept dept);

    void deleteDept(Long id);

    List<SysDept> getDeptTreeSelect();
}

