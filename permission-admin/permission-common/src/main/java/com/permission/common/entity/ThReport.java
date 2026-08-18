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
@TableName(value = "th_report", autoResultMap = true)
public class ThReport extends BaseEntity {

    private Long reporterId;

    @TableField(exist = false)
    private String reporterName;

    private String targetType;

    private Long targetId;

    private String reason;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> evidenceImages;

    private Integer status;

    private String handleResult;

    private Long handlerId;

    private LocalDateTime handleTime;
}
