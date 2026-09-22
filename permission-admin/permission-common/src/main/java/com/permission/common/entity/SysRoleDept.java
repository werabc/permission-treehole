package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色-自定义数据范围部门关联（sys_role_dept）
 * 注意：该表无 deleted 字段，不继承 BaseEntity
 */
@Data
@TableName("sys_role_dept")
public class SysRoleDept {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;

    private Long deptId;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
}
