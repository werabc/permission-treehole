package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_category")
public class NovelCategory extends BaseEntity {

    private String categoryName;
    private String categoryDesc;
    private Integer sort;
    private Integer status;
}
