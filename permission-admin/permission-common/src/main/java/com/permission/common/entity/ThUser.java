package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("th_user")
public class ThUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String nickname;

    private String password;

    private String avatar;

    private String bio;

    private Integer gender;

    /** 0-封禁 1-正常 */
    private Integer status;

    private LocalDateTime muteUntil;

    private LocalDateTime banUntil;

    private Integer postCount;

    private Integer commentCount;

    private Integer violationCount;

    private LocalDateTime lastPostTime;

    private String lastLoginIp;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
