package com.permission.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.permission.common.entity.SysMenu;
import com.permission.common.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT * FROM sys_role_menu WHERE role_id = #{roleId}")
    List<SysRoleMenu> selectRoleMenusByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT DISTINCT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.status = 1 AND m.deleted = 0 " +
            "ORDER BY m.sort ASC")
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.mapper.SysMenuMapper
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Mapper — MyBatis-Plus，标识映射接口，被 MapperScan 扫描注册
//   - BaseMapper<SysMenu> — MyBatis-Plus 通用 Mapper，提供 sys_menu 表标准 CRUD
//   - @Select — MyBatis，声明式 SQL 映射，定义自定义查询语句
//   - @Param — MyBatis，参数绑定，将方法参数映射到 SQL 中的命名占位符 #{}
//
// 【关键依赖】
//   - 依赖 SysMenu 实体 → 菜单表（sys_menu）的 ORM 映射载体
//   - 依赖 SysRoleMenu 实体 → 角色-菜单中间表（sys_role_menu），自定义查询返回该表记录
//
// 【关联文件】
//   - 被 SysMenuService / SysMenuServiceImpl 调用，提供菜单树、用户菜单查询
//   - 被 UserDetailsServiceImpl 调用，加载用户权限（selectRoleMenusByRoleId）
//   - 被 SysRoleServiceImpl / SysUserServiceImpl 调用，用于角色/用户菜单关联
//   - 被 @MapperScan 扫描注册
//
// 【核心作用】
//   菜单表（sys_menu）的数据访问接口，额外提供两个自定义查询：
//   - selectRoleMenusByRoleId：根据角色 ID 查询该角色关联的菜单-角色绑定记录
//   - selectMenusByUserId：根据用户 ID 三表联查（user_role → role_menu → menu），
//     获取该用户所有可用菜单
//
// 【设计必要性】
//   菜单与角色、用户之间存在多对多关系，需通过中间表联查获取用户的完整菜单/权限集合，
//   单独的两个自定义查询方法满足这一业务需求。
//
// 【注意事项/安全提示】
//   - selectMenusByUserId 查询已过滤 m.status=1 AND m.deleted=0，仅返回有效菜单
//   - 查询通过 JOIN 实现多对多关联，注意索引覆盖（sys_role_menu 的 role_id/menu_id 字段）
// ============================================================
