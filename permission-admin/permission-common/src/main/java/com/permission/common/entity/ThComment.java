package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("th_comment")
public class ThComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long parentId;

    private String content;

    private Integer isAnonymous;

    private Integer likeCount;

    /** 0-隐藏 1-显示 */
    private Integer status;

    private String ip;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
