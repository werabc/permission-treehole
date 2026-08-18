package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("th_like")
public class ThLike extends BaseEntity {

    private Long userId;

    private String targetType;

    private Long targetId;
}
