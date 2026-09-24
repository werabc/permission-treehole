package com.permission.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.permission.common.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 按主键取部门（**不做数据范围过滤**）
     *
     * 用于数据权限守门员区分"不存在"(404) 与"越权访问"(403)，
     * 因为带数据权限的 selectById 两种情况都会返回 0 行。
     * **禁止在业务查询中直接使用**——业务读取必须走带数据权限的路径。
     */
    @Select("SELECT id, dept_name, parent_id, ancestors, dept_level, status, deleted FROM sys_dept WHERE id = #{id}")
    SysDept selectRawById(@Param("id") Long id);
}

