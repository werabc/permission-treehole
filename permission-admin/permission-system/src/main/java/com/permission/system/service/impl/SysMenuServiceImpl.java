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

