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
