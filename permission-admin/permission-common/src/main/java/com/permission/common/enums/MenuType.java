package com.permission.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MenuType {
    CATALOG(0, "目录"),
    MENU(1, "菜单"),
    BUTTON(2, "按钮");

    private final int code;
    private final String desc;
}

