package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 树洞敏感词
 * level: 1-直接拦截(内容不入库)  2-警告并转人工审核
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("th_sensitive_word")
public class ThSensitiveWord extends BaseEntity {

    /** 敏感词 */
    private String word;

    /** 等级：1-拦截 2-转审 */
    private Integer level;

    /** 分类：广告/辱骂/色情/违法/其他 */
    private String category;

    /** 状态：1-启用 0-停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createBy;
}
