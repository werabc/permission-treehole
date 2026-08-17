package com.permission.framework.config;

import com.permission.common.constant.SecurityConstants;
import com.permission.framework.filter.JwtAuthenticationFilter;
import com.permission.framework.handler.SecurityAccessDeniedHandler;
import com.permission.framework.handler.SecurityAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityAccessDeniedHandler accessDeniedHandler;
    private final SecurityAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(authenticationEntryPoint))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(SecurityConstants.LOGIN_URL, SecurityConstants.REFRESH_TOKEN_URL).permitAll()
                // Dashboard 统计接口需要认证
                .requestMatchers("/api/dashboard/**").authenticated()
                // Swagger/Doc 文档公开
                .requestMatchers("/doc.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                // 认证接口公开
                .requestMatchers("/api/auth/**").permitAll()
                // 树洞公开接口
                .requestMatchers("/api/th/category/list", "/api/th/post/page", "/api/th/post/{id}", "/api/th/comment/page", "/api/th/post/{id}/liked").permitAll()
                // 树洞认证接口（登录/注册/用户信息）
                .requestMatchers("/api/th/auth/**").permitAll()
                // 树洞写操作需要 JWT 认证（通过 JwtAuthenticationFilter 解析 token 中的 userId）
                .requestMatchers("/api/th/post", "/api/th/comment").authenticated()
                .requestMatchers("/api/th/post/{id}/like", "/api/th/comment/{id}/like").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/th/post/{id}/like").authenticated()
                // 个人中心需要认证
                .requestMatchers("/api/th/user/**").authenticated()
                // 举报需要认证
                .requestMatchers("/api/th/report/**").authenticated()
                // 管理接口需要 admin 权限
                .requestMatchers("/api/admin/**").hasAuthority("admin")
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.framework.config.SecurityConfig
// 【模块】permission-framework（安全与基础设施模块）
//
// 【使用的注解/技术】
//   - @Configuration — Spring，声明配置类
//   - @EnableWebSecurity — Spring Security，启用 Spring Security 过滤链并加载其自动配置
//   - @EnableMethodSecurity — Spring Security，启用方法级安全注解（@PreAuthorize / @Secured）
//   - @RequiredArgsConstructor — Lombok，为所有 final 字段生成构造函数实现依赖注入
//
// 【关键依赖/注入】
//   - 注入 JwtAuthenticationFilter — 在 SecurityFilterChain 中注册到 UsernamePasswordAuthenticationFilter 之前
//   - 注入 SecurityAccessDeniedHandler — 处理访问被拒绝（403）异常
//   - 注入 SecurityAuthenticationEntryPoint — 处理未认证（401）请求
//   - 依赖 SecurityConstants — 读取 LOGIN_URL、REFRESH_TOKEN_URL 等 permitAll 路径常量
//
// 【关联文件】
//   - 被 permission-api 模块启动类扫描装配
//   - 与 JwtAuthenticationFilter 配合完成基于 JWT 的无状态认证
//   - 与 CustomUserDetailsService 配合加载用户身份与权限
//   - 硬编码 permitAll：login、refresh、Swagger/doc 端点、/api/auth/**
//
// 【核心作用】定义应用完整的安全策略：关闭 CSRF/CORS/会话，配置放行路径与受保护路径，注册 JWT 过滤器，
//            并暴露 PasswordEncoder 与 AuthenticationManager Bean。
//
// 【设计必要性】
//   - CSRF 与 CORS 在纯 JWT 无状态场景中关闭（前端独立部署、跨域由网关层处理）；
//   - SessionCreationPolicy.STATELESS 避免服务端生成 JSESSIONID，从根源实现无状态；
//   - 暴露 AuthenticationManager Bean 供登录接口手动完成用户名/密码认证。
//
// 【注意事项/安全提示】
//   - ⚠️ 生产环境必须通过 profile 或配置项隐藏 Swagger/doc 端点（当前为 permitAll），避免 API 文档
//     公开暴露；
//   - 生产环境前端跨域应在网关/Nginx 收敛，而非在此处全局放开；
//   - 新增公开接口需在此处同步 requestMatchers().permitAll()，否则默认被 anyRequest().authenticated() 拦截；
//   - BCryptPasswordEncoder 不可直接用于解密（单向哈希）；密码比对使用 PasswordEncoder.matches。
// ============================================================
