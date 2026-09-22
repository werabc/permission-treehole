package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 树洞用户收藏
 * 当前仅支持收藏帖子（targetType = POST）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("th_collect")
public class ThCollect extends BaseEntity {

    /** 用户ID */
    private Long userId;

    /** 收藏对象类型：POST */
    private String targetType;

    /** 收藏对象ID */
    private Long targetId;
}
