package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("th_notification")
public class ThNotification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long senderId;

    private String type;

    private String targetType;

    private Long targetId;

    private String content;

    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
