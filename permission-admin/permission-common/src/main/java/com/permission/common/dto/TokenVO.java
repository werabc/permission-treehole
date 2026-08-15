package com.permission.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenVO {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.dto.TokenVO
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Data — Lombok，生成 getter/setter/toString
//   - @Builder — Lombok，流式构建
//   - @NoArgsConstructor / @AllArgsConstructor — 提供无参/全参构造器
//
// 【关联文件】
//   - 由 AuthService 登录成功后构建并返回 → service/AuthService.java + impl/AuthServiceImpl.java
//   - AuthController 将其包装进 R<TokenVO> 返回前端 → controller/AuthController.java
//   - 前端 localStorage 保存 accessToken，拦截器带上 Authorization 头
//
// 【核心作用】承载登录/刷新 token 接口返回的双令牌信息。
//
// 【设计必要性】双令牌（短时效 access + 长时效 refresh）支撑无感续期与登出机制；
//   VO 命名明确区别于认证中间对象 LoginUser。
//
// 【注意事项】
//   - expiresIn 单位是秒，前端用于定时刷新
//   - refreshToken 应与 accessToken 在 Redis 中都有对应记录以便支持失效
// ============================================================
