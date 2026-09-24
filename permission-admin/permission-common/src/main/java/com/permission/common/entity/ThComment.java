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

    /**
     * 作者头像。
     * 匿名评论必须留空 —— 头像可用于反向识别身份，属于去匿名化风险。
     */
    @TableField(exist = false)
    private String authorAvatar;

    /**
     * 当前登录用户是否已点赞这条评论。
     * 列表接口若不返回用户态，前端只能靠本地猜测，刷新后状态就丢了。
     * 由 pageComments 根据当前登录用户一次性填充（未登录恒为 false）。
     */
    @TableField(exist = false)
    private Boolean liked;

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
