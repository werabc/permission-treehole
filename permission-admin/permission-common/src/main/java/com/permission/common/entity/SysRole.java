package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private String roleName;
    private String roleCode;
    private String roleDesc;

    /** 数据权限范围，见 DataScope 枚举（1全部 … 8仅本人） */
    private Integer dataScope;

    /** 数据范围为"本部门及以下(限N级)"时的层级深度 N */
    private Integer dataScopeLevel;

    private Integer status;

    @TableField(exist = false)
    private String dataScopeName;

    /** 自定义数据范围时，前端回显用逗号分隔的部门ID串 */
    @TableField(exist = false)
    private String deptIds;

    /** 是否为内置角色（由 role_code 推导，非库字段） */
    @TableField(exist = false)
    private Boolean builtin;
}

