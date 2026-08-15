package com.permission.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.SysMenu;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {

    List<SysMenu> getMenuTree(String keyword, Integer status);

    SysMenu getMenuById(Long id);

    void createMenu(SysMenu menu);

    void updateMenu(SysMenu menu);

    void deleteMenu(Long id);

    List<SysMenu> getUserMenus(Long userId);

    List<SysMenu> getMenuTreeSelect();
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.service.SysMenuService
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - IService<SysMenu> — MyBatis-Plus，继承通用 Service 接口，提供菜单基础 CRUD 能力
//
// 【关键依赖】
//   - 依赖 SysMenu 实体 → 菜单业务操作的数据载体
//
// 【关联文件】
//   - 被 SysMenuServiceImpl 实现，封装菜单业务逻辑
//   - 被 MenuController 调用，提供菜单管理 API
//   - 被 UserDetailsServiceImpl 调用（间接通过 Mapper），加载用户菜单
//
// 【核心作用】
//   菜单业务服务接口，定义菜单树查询、用户菜单查询、增删改、菜单树下拉选择等业务方法。
//
// 【设计必要性】
//   接口与实现分离，便于在实现层统一处理菜单树构建、名称唯一性校验、关联清理等业务细节。
//
// 【注意事项/安全提示】
//   - getUserMenus 返回按 sort 排序的菜单树，供前端动态渲染路由
//   - 删除菜单时需级联清理角色-菜单关联（通过业务层实现）
// ============================================================
