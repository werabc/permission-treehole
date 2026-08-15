package com.permission.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.framework.config.WebMvcConfig
// 【模块】permission-framework（安全与基础设施模块）
//
// 【使用的注解/技术】
//   - @Configuration — Spring，声明配置类
//   - implements WebMvcConfigurer — Spring MVC，通过重写接口方法自定义 MVC 配置，取代继承
//     已废弃的 WebMvcConfigurerAdapter
//   - @Override addCorsMappings — 注册全局 CORS 映射
//
// 【关键依赖/注入】
//   - 无显式注入，无参数，仅配置 WebMvcConfigurer 接口方法
//   - 操作对象：CorsRegistry — Spring MVC 提供的 CORS 注册表
//
// 【关联文件】
//   - 被 permission-api 模块启动类扫描装配
//   - 与 SecurityConfig 协同：该层配置全局 CORS 映射；Security 层已关闭 cors 以确保
//     两者不冲突。若启用 Security 的 cors()，需在此处放弃由 WebMvc 接管
//
// 【核心作用】为 /api/** 路径前缀统一配置跨域访问策略，支持凭证携带与预检缓存。
//
// 【设计必要性】
//   - 前端独立域名访问后端 API 时，浏览器会强制预检 OPTIONS 请求；不配置 CORS 会报跨域错误；
//   - allowedOriginPatterns("*") + allowCredentials(true) 覆盖凭证场景而不需写死域名。
//
// 【注意事项/安全提示】
//   - allowedOriginPatterns=* 在开发环境方便，但生产环境应收敛为具体域名白名单（如 https://app.example.com），
//     任意域 + allowCredentials 可能导致 CSRF 类风险在浏览器侧被放大；
//   - SecurityConfig 已 disable() 了 Spring Security 的 CORS 过滤器；本处是兜底/全局兜底的 CORS
//     策略，两者只能启其一；
//   - maxAge(3600) 表示预检缓存 3600 秒，变更 CORS 配置后客户端最长 1 小时内仍可能使用旧缓存。
// ============================================================
