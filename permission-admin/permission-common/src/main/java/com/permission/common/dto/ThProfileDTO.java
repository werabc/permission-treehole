package com.permission.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 树洞用户资料更新 DTO — 白名单字段，防止 Mass Assignment。
 * 仅包含允许用户自行修改的字段，排除 status/muteUntil/banUntil/password 等敏感字段。
 */
@Data
public class ThProfileDTO {

    /**
     * 昵称：2-20字符，仅允许中文、字母、数字、下划线。
     * 服务层还会做 HTML 转义防止 XSS。
     */
    @Size(min = 2, max = 20, message = "昵称长度应为2-20位")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9_]+$", message = "昵称仅允许中文、字母、数字和下划线")
    private String nickname;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;

    /** 性别：0-未知 1-男 2-女 */
    private Integer gender;

    /** 个人简介 */
    @Size(max = 200, message = "简介不能超过200字")
    private String bio;

    /**
     * 头像地址。
     *
     * 只接受两种形态，其余一律拒绝：
     *  1. 本站上传接口返回的相对地址（/api/file/avatar/<32位hex>.<jpg|png|gif|webp>）
     *  2. 外部 http(s) 链接
     *
     * 必须收窄的原因：avatar 会被直接渲染进 <img src>，
     * 若放任任意字符串，javascript:/data:text/html 这类值就是存储型 XSS 的入口。
     * 允许空串是为了支持"清空头像"。
     */
    @Pattern(
            regexp = "^(|/api/file/avatar/[0-9a-f]{32}\\.(?:jpg|png|gif|webp)|https?://[^\\s\"'<>]{1,300})$",
            message = "头像地址不合法，请使用上传功能或填写 http(s) 图片链接"
    )
    @Size(max = 255, message = "头像路径过长")
    private String avatar;
}
