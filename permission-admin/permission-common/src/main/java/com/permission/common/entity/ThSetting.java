package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("th_setting")
public class ThSetting {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("config_key")
    private String settingKey;
    @TableField("config_value")
    private String settingValue;
    @TableField("config_desc")
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
