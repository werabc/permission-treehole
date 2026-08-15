package com.permission.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String captchaKey;

    private String captchaCode;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.dto.LoginDTO
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Data — Lombok，生成 getter/setter/toString
//   - @NotBlank — Jakarta Bean Validation，校验 username/password 非空
//
// 【关联文件】
//   - 被 AuthController.login() 接收请求体 → controller/AuthController.java
//   - 被 AuthServiceImpl.login() 校验后构建 Authentication → service/impl/AuthServiceImpl.java
//   - 验证码 key 与 Redis 缓存关联 → security/CaptchaService.java
//
// 【核心作用】登录入口的请求数据传输对象，负责承载并校验用户名/密码/验证码。
//
// 【设计必要性】DTO 与实体分离：登录入参与 sys_user 表结构不同，不应直接暴露实体。
//   @NotBlank 提供控制器层的一站式入参校验，免去 if-null 样板代码。
//
// 【注意事项】
//   - captchaKey/captchaCode 不强制必填，由登录策略决定是否启用验证码
//   - password 为前端传入的明文，服务层负责加密比对，不在此处处理
// ============================================================
