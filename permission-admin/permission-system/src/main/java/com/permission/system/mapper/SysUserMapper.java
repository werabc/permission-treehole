package com.permission.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.permission.common.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按主键取用户（**不做数据范围过滤**）
     *
     * 为什么需要它：DataPermissionInterceptor 会把数据范围拼进 selectById，
     * 导致"不存在"与"越权访问"都表现为 0 行，守门员无法区分该返回 404 还是 403。
     * 本方法用原生 SQL 绕过拦截器，仅用于权限判定（拿 dept_id 判断归属），
     * **禁止在业务查询中直接使用**——业务读取必须走带数据权限的路径。
     */
    @Select("SELECT id, username, nickname, dept_id, status, deleted FROM sys_user WHERE id = #{id}")
    SysUser selectRawById(@Param("id") Long id);

    @Select("SELECT DISTINCT r.role_code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT m.permission FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "INNER JOIN sys_role r ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.status = 1 AND m.deleted = 0 " +
            "AND r.status = 1 AND r.deleted = 0 " +
            "AND m.permission IS NOT NULL AND m.permission != ''")
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);
}

