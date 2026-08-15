package com.permission.framework.security;

import com.permission.common.dto.LoginUser;

public interface CustomUserDetailsService {
    LoginUser loadUserById(Long userId);
}
// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.framework.security.CustomUserDetailsService
// 【模块】permission-framework（安全与基础设施模块）
//
// 【使用的注解/技术】
//   - 无类级别注解：纯接口（由实现类 @Service 装配）
//
// 【关键依赖/注入】
//   - 依赖 LoginUser — permission-common 模块，已认证用户的身份+权限聚合对象
//   - 由实现类通过 loadUserById(userId) 从数据库/缓存加载用户并填充角色权限
//
// 【关联文件】
//   - 被 JwtAuthenticationFilter 调用 loadUserById(userId) 加载当前请求用户
//   - 实现类在 permission-service 模块（含 @Service 注解），桥接安全层与业务数据层
//   - 与 permission-common.dto.LoginUser、permission-common.dto/R 搭配形成安全闭环
//
// 【核心作用】定义 Spring Security 用户加载契约；按用户 ID 返回权限填充后的 LoginUser。
//
// 【设计必要性】
//   - 与 Spring Security 自带的 UserDetailsService (loadByUsername) 对齐概念但解耦命名，核心原因
//     系统按 JWT 中的 userId 加载（非用户名），方法语义更清晰；
//   - 接口位置在 framework，实现类在 service，保持安全层对业务层的单向依赖。
//
// 【注意事项/安全提示】
//   - 实现类通常需要校验用户是否存在/未冻结/未删除，否则应抛 UsernameNotFoundException；
//   - loadUserById 被 JwtAuthenticationFilter 频繁调用，实现类中建议缓存用户权限并设置合理 TTL；
//   - LoginUser 不可暴露原始密码字段到内存之外；清理敏感信息后再放入 SecurityContext。
// ============================================================
