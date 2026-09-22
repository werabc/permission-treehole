package com.permission.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.permission.common.entity.SysRoleDept;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {

    /** 查询多个角色配置的自定义部门ID（去重） */
    @Select("<script>SELECT DISTINCT dept_id FROM sys_role_dept WHERE role_id IN " +
            "<foreach collection='roleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Long> selectDeptIdsByRoleIds(@Param("roleIds") List<Long> roleIds);

    /** 查询单个角色配置的自定义部门ID */
    @Select("SELECT dept_id FROM sys_role_dept WHERE role_id = #{roleId}")
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);

    /** 覆盖式保存：先删后插（由调用方置于事务内） */
    @Delete("DELETE FROM sys_role_dept WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);
}
