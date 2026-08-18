package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "th_post", autoResultMap = true)
public class ThPost extends BaseEntity {

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
}
