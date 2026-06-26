package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel")
public class Novel extends BaseEntity {

    private String title;
    private Long authorId;
    private String authorName;
    private Long categoryId;
    private String coverUrl;
    private String intro;
    private Integer status;
    private Long wordCount;
    private Long clickCount;
    private Long likeCount;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String lastChapterTitle;
}
