package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("th_like")
public class ThLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** POST/COMMENT */
    private String targetType;

    private Long targetId;

    private String ip;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
