package com.permission.common.constant;

public interface SecurityConstants {

    String TOKEN_PREFIX = "Bearer ";
    String TOKEN_HEADER = "Authorization";
    String TOKEN_CACHE_PREFIX = "token:";
    String TOKEN_BLACKLIST_PREFIX = "blacklist:";
    String CAPTCHA_PREFIX = "captcha:";
    String LOGIN_FAIL_PREFIX = "login_fail:";
    String LOGIN_RATE_LIMIT_PREFIX = "rate_limit:login:";
    String LOGIN_URL = "/api/auth/login";
    String REFRESH_TOKEN_URL = "/api/auth/refresh";
    String LOGOUT_URL = "/api/auth/logout";
    String CAPTCHA_URL = "/api/auth/captcha";
    String JWT_SECRET = "permission-admin-secret-key-2024-must-be-long-enough-for-hs256";
    long TOKEN_EXPIRE = 7200;
    long REFRESH_TOKEN_EXPIRE = 604800;
    int MAX_LOGIN_FAIL_COUNT = 5;
    int ACCOUNT_LOCK_MINUTES = 30;
    int LOGIN_RATE_LIMIT_MAX = 5;
    int LOGIN_RATE_LIMIT_WINDOW = 60;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.constant.SecurityConstants
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - interface 常量类 — 所有字段隐式为 public static final，无需重复写修饰符
//
// 【关联文件】
//   - 被 JwtTokenUtil 读取 token 头、前缀、密钥 → framework/security/JwtTokenUtil.java
//   - 被 SecurityConfig 注册登录/注销白名单 → framework/config/SecurityConfig.java
//   - 被 RedisUtil/CaptchaService 拼接缓存 key 时被引用 → common/utils/RedisUtil.java
//   - 被 AuthServiceImpl 锁定/限流时引用前缀 → service/impl/AuthServiceImpl.java
//
// 【核心作用】集中存放鉴权体系相关的所有 Redis 前缀、JWT 参数、URL 白名单和阈值。
//
// 【设计必要性】key 前缀、密钥、过期秒数散落在多处极易改漏；统一收口后，运维调整时
//   只改常量，不动业务代码。interface 常量类天然 final，不可被实例化。
//
// 【注意事项】
//   - JWT_SECRET 线上应从配置文件/密钥管理服务读取，此处硬编码便于开发
//   - 修改 TOKEN_EXPIRE 后已签发 token 仍按原时间生效直到过期
// ============================================================
