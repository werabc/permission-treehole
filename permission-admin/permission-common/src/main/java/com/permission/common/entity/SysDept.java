package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    private String deptName;
    private Long parentId;
    private String ancestors;
    private Integer sort;
    private String leader;
    private String phone;
    private String email;
    private Integer status;

    @TableField(exist = false)
    private List<SysDept> children;
}

