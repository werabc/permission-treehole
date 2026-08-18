package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 树洞通知实体 — 注意：th_notification 表无 deleted 字段，不继承 BaseEntity
 */
@Data
@EqualsAndHashCode
@TableName("th_notification")
public class ThNotification {

    @TableField("id")
    private Long id;

    private Long userId;

    private Long senderId;

    private String type;

    private String targetType;

    private Long targetId;

    private String content;

    private Integer isRead;

    private LocalDateTime createTime;
}
