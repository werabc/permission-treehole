package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("th_category")
public class ThCategory extends BaseEntity {

    private String name;

    private String code;

    private String icon;

    private String description;

    private Integer sort;

    private Integer status;

    private Integer postCount;
}
