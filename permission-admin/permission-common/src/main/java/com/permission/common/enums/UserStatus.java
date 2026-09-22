package com.permission.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus {
    ENABLED(1, "启用"),
    DISABLED(0, "禁用");

    private final int code;
    private final String desc;
}

