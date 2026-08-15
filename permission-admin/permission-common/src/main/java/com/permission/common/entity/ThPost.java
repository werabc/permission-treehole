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

    private Long categoryId;

    @TableField(exist = false)
    private String categoryName;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    @TableField("content")
    private String content;

    private Integer isAnonymous;

    private Integer isTop;

    /** 0-待审核 1-已通过 2-已拒绝 */
    private Integer status;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private String ip;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
