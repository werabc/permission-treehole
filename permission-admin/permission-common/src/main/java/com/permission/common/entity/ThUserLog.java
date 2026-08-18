package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 树洞用户行为日志实体 — 注意：th_user_log 表无 deleted 字段，不继承 BaseEntity
 */
@Data
@EqualsAndHashCode
@TableName("th_user_log")
public class ThUserLog {

    @TableField("id")
    private Long id;

    private Long userId;

    @TableField(exist = false)
    private String userName;

    private String action;

    private String targetType;

    private Long targetId;

    private String detail;

    private String ip;

    private LocalDateTime createTime;
}
