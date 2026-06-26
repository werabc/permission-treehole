package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_comment")
public class NovelComment extends BaseEntity {

    private Long novelId;
    private Long chapterId;
    private Long userId;
    private String userName;
    private String content;
    private Long parentId;
    private Integer likeCount;

    @TableField(exist = false)
    private List<NovelComment> children;
}
