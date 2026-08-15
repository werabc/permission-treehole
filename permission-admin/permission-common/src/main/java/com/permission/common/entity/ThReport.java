package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("th_report")
public class ThReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** POST/COMMENT */
    private String targetType;

    private Long targetId;

    private String reason;

    private String description;

    /** 0-待处理 1-已处理 2-驳回 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
