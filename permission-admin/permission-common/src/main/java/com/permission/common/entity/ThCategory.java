package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("th_category")
public class ThCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String icon;

    private String description;

    private Integer sort;

    private Integer status;

    private Integer postCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
