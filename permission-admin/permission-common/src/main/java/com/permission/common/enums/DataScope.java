package com.permission.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataScope {
    ALL(1, "全部数据"),
    DEPT_AND_SUB(2, "本部门及子部门"),
    DEPT(3, "本部门"),
    CUSTOM(4, "自定义"),
    SELF(5, "仅本人");

    private final int code;
    private final String desc;
}

