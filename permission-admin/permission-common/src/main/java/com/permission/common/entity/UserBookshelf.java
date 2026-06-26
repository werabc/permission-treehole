package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_bookshelf")
public class UserBookshelf extends BaseEntity {

    private Long userId;
    private Long novelId;

    @TableField(exist = false)
    private String novelTitle;

    @TableField(exist = false)
    private String coverUrl;

    @TableField(exist = false)
    private String authorName;

    @TableField(exist = false)
    private String lastChapterTitle;
}
