package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("th_user")
public class ThUser extends BaseEntity {

    private String username;

    @JsonIgnore
    private String password;

    private String nickname;

    private String avatar;

    private String bio;

    private Integer gender;

    private String email;

    private Integer status;

    private LocalDateTime muteUntil;

    private LocalDateTime banUntil;

    private Integer postCount;

    private Integer commentCount;

    private Integer violationCount;

    private LocalDateTime lastPostTime;

    private String lastLoginIp;
}
