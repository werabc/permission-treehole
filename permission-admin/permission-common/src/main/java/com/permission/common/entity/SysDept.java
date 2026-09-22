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
    /**
     * 组织层级：1-集团 2-公司 3-部门 4+-小组
     * 数据权限（本集团/本公司/限N级）依赖该字段确定边界
     */
    private Integer deptLevel;
    private Integer sort;
    private String leader;
    private String phone;
    private String email;
    private Integer status;

    @TableField(exist = false)
    private List<SysDept> children;
}

