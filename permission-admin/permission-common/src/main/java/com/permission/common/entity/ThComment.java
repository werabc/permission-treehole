package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("th_comment")
public class ThComment extends BaseEntity {

    private Long postId;

    private Long userId;

    @TableField(exist = false)
    private String authorName;

    private Long parentId;

    private Long replyUserId;

    @TableField(exist = false)
    private String replyUserName;

    private String content;

    private Integer isAnonymous;

    private Integer likeCount;

    private Integer status;

    private String ip;
}
