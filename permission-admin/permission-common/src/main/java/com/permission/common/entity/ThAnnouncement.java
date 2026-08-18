package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("th_announcement")
public class ThAnnouncement extends BaseEntity {

    private String title;

    private String content;

    private String type;

    private Integer status;

    private LocalDateTime publishTime;

    private LocalDateTime expireTime;

    private Long creatorId;

    @TableField(exist = false)
    private String creatorName;
}
