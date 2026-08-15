package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "th_post", autoResultMap = true)
public class ThPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    @TableField(exist = false)
    private String authorName;

    private Long categoryId;

    @TableField(exist = false)
    private String categoryName;

    private String title;

    private String content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    private Integer isAnonymous;

    private Integer isTop;

    private Integer status;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer reportCount;

    private String ip;

    private String auditRemark;

    private Long auditorId;

    private LocalDateTime auditTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
