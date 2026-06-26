package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_chapter")
public class NovelChapter extends BaseEntity {

    private Long novelId;
    private String chapterTitle;
    private Integer chapterNum;
    private String content;
    private Integer wordCount;
    private Integer isFree;

    @TableField(exist = false)
    private String novelTitle;
}
