package com.permission.framework.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 允许的跨域 Origin 白名单，多个以逗号分隔。
     * 生产环境务必通过环境变量 CORS_ALLOWED_ORIGINS 配置真实域名，
     * 例如：CORS_ALLOWED_ORIGINS=https://www.example.com,https://admin.example.com
     * 留空则禁止所有跨域请求（最安全，前后端同域部署时推荐）。
     */
    @Value("${cors.allowed.origins:}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 解析白名单：按逗号分割并去除空白
        List<String> origins = allowedOrigins == null || allowedOrigins.isBlank()
                ? List.of()
                : List.of(allowedOrigins.split(",")).stream().map(String::trim).filter(s -> !s.isBlank()).toList();

        if (origins.isEmpty()) {
            // 未配置白名单时：仅允许同源请求，不反射任意 Origin（安全修复 CRITICAL-01）。
            // 使用 allowedOriginPatterns("*") 但不设置 allowCredentials，
            // 浏览器不会在跨域请求中携带 Cookie/Token，因此不会造成凭证泄露风险。
            // 生产环境务必通过 cors.allowed.origins 配置真实域名白名单。
            registry.addMapping("/api/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(false)  // 关键：不与 * 共用，防止凭证泄露
                    .maxAge(3600);
            return;
        }

        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(new String[0]))  // 精确白名单，禁止反射
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

