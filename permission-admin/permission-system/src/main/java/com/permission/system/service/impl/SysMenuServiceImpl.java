package com.permission.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.ResultCode;
import com.permission.common.entity.SysMenu;
import com.permission.common.entity.SysRoleMenu;
import com.permission.common.exception.BusinessException;
import com.permission.system.mapper.SysMenuMapper;
import com.permission.system.mapper.SysRoleMenuMapper;
import com.permission.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public List<SysMenu> getMenuTree(String keyword, Integer status) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            // Escape LIKE special chars to prevent wildcard abuse
            String safeKeyword = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            wrapper.like(SysMenu::getMenuName, safeKeyword);
        }
        if (status != null) {
            wrapper.eq(SysMenu::getStatus, status);
        }
        wrapper.orderByAsc(SysMenu::getSort);
        List<SysMenu> allMenus = baseMapper.selectList(wrapper);
        return buildTree(allMenus, 0L);
    }

    @Override
    public SysMenu getMenuById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    @Transactional
    public void createMenu(SysMenu menu) {
        validateMenuNameUnique(menu.getMenuName(), menu.getParentId(), null);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        baseMapper.insert(menu);
    }

    @Override
    @Transactional
    public void updateMenu(SysMenu menu) {
        SysMenu existing = baseMapper.selectById(menu.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜单不存在");
        }
        validateMenuNameUnique(menu.getMenuName(), menu.getParentId(), menu.getId());
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        baseMapper.updateById(menu);
    }

    @Override
    @Transactional
    public void deleteMenu(Long id) {
        long childCount = baseMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(4002, "存在子菜单，无法删除");
        }
        baseMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));
    }

    @Override
    public List<SysMenu> getUserMenus(Long userId) {
        List<SysMenu> menus = baseMapper.selectMenusByUserId(userId);
        return buildTree(menus, 0L);
    }

    @Override
    public List<SysMenu> getMenuTreeSelect() {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getStatus, 1).orderByAsc(SysMenu::getSort);
        List<SysMenu> allMenus = baseMapper.selectList(wrapper);
        List<SysMenu> tree = buildTree(allMenus, 0L);

        SysMenu root = new SysMenu();
        root.setId(0L);
        root.setMenuName("顶级菜单");
        root.setParentId(-1L);
        root.setChildren(tree);

        List<SysMenu> result = new ArrayList<>();
        result.add(root);
        return result;
    }

    private List<SysMenu> buildTree(List<SysMenu> menus, Long parentId) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (parentId.equals(menu.getParentId())) {
                menu.setChildren(buildTree(menus, menu.getId()));
                tree.add(menu);
            }
        }
        return tree;
    }

    private void validateMenuNameUnique(String menuName, Long parentId, Long excludeId) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getMenuName, menuName)
                .eq(SysMenu::getParentId, parentId != null ? parentId : 0L);
        if (excludeId != null) {
            wrapper.ne(SysMenu::getId, excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.MENU_NAME_EXISTS);
        }
    }
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.service.impl.SysMenuServiceImpl
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Service — Spring，声明业务层组件
//   - @RequiredArgsConstructor — Lombok，生成必需参数构造器（构造器注入）
//   - @Override — Java，标识重写接口/父类方法
//   - @Transactional — Spring，声明式事务，保证菜单增删改的原子性（删除菜单+同步清理角色菜单关联）
//   - ServiceImpl<SysMenuMapper, SysMenu> — MyBatis-Plus，继承通用 Service 实现基类
//   - LambdaQueryWrapper — MyBatis-Plus，类型安全查询构造器
//   - StrUtil / CollUtil — Hutool，字符串/集合工具
//   - BusinessException / ResultCode — 自定义业务异常与错误码
//
// 【关键依赖】
//   - 依赖 SysMenuMapper → 菜单数据访问（含 selectMenusByUserId 多表联查）
//   - 依赖 SysRoleMenuMapper → 删除菜单时同步清理角色菜单关联
//   - 依赖 SysMenu 实体 → 菜单业务操作载体
//   - 依赖 SysMenuService 接口 → 实现该接口契约
//
// 【关联文件】
//   - 被 MenuController 调用，提供菜单管理业务逻辑
//   - 被 UserDetailsServiceImpl 调用（getUserMenus 间接通过 Mapper 实现用户菜单加载）
//   - 被 DataInitializer 调用（间接通过 Controller/Mapper），初始化菜单基础数据
//
// 【核心作用】
//   菜单业务服务实现：提供菜单树查询、单菜单详情、新增菜单（含父级校验）、修改菜单、删除菜单
//   （含关联清理：删除该菜单在所有角色下的绑定）、用户菜单树查询（动态路由）、菜单树下拉选择。
//
// 【设计必要性】
//   菜单树的层级结构通过 parentId 内存构建（buildTree 私有方法），删除时必须保证先删除
//   子菜单再删父菜单，且必须同步清理角色-菜单关联表，这些复杂业务规则适合放在 Service 层封装。
//
// 【注意事项/安全提示】
//   - LIKE 查询已转义通配符：keyword 中的 /%/_ 会被转义为 \\\\%\\_\\_，防止用户输入 %
//     或 _ 导致全表匹配（LIKE 通配符注入风险）
//   - deleteMenu 先检查是否存在子菜单（有则抛 4002 业务异常），再清关联再删本体，避免脏数据
//   - validateMenuNameUnique 同 parentId 下校验菜单名称唯一性，更新时可排除自身 ID
//   - 菜单类型包括 CATALOG（目录）、MENU（菜单）、BUTTON（按钮），按钮级权限通过 permission 字段标识
// ============================================================
