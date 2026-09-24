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
                // Swagger/Doc 文档公开
                .requestMatchers("/doc.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                // 错误页必须放行：Spring 处理错误时会内部转发到 /error，
                // 若这里要求鉴权，匿名请求遇到的 404/500 会被改写成 401（状态码说谎，排查时极易误导）
                .requestMatchers("/error").permitAll()
                // 认证接口：只放行登录相关端点，其余（user-info/logout）必须携带有效 Token
                // 注意：不要用 /api/auth/** 整体放行，否则日后在该路径下新增管理端点会直接公网可达
                .requestMatchers("/api/auth/captcha", "/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                // 树洞认证接口（登录/注册/用户信息）
                .requestMatchers("/api/th/auth/**").permitAll()
                // 管理端 Dashboard 统计接口需要认证
                .requestMatchers("/api/dashboard/**").authenticated()

                // ===== 树洞公开读接口（全部限定 GET，避免误放行写操作） =====
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/th/category/list",
                        "/api/th/announcements",
                        "/api/th/post/page",
                        "/api/th/post/{id}",
                        "/api/th/post/{id}/liked",
                        "/api/th/post/{id}/collected",
                        "/api/th/comment/page",
                        "/api/th/search",
                        "/api/th/user/public/{id}",
                        "/api/th/user/{id}/posts").permitAll()

                // ===== 树洞写操作需要 JWT 认证 =====
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/api/th/post",
                        "/api/th/comment",
                        "/api/th/report",
                        "/api/th/collect/{postId}",
                        "/api/th/post/{id}/like",
                        "/api/th/comment/{id}/like").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                        "/api/th/post/{id}",
                        "/api/th/comment/{id}",
                        "/api/th/post/{id}/like",
                        "/api/th/comment/{id}/like").authenticated()

                // 个人中心需要认证
                .requestMatchers("/api/th/user/**").authenticated()

                // ===== 上传文件的读取需要公开 =====
                // <img src> 不会携带 Authorization 头，若这里要求鉴权，头像永远渲染不出来。
                // 安全性由"服务端生成 32 位随机文件名 + 目录白名单 + 文件名严格正则"保证，
                // 无法枚举，也不存在路径穿越。上传本身（POST /api/file/avatar）仍要求登录。
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/file/**").permitAll()

                // ===== 管理接口：只要求"已登录"，具体权限交给各 Controller 上的 @PreAuthorize =====
                // 原先这里是 hasAuthority("admin")，等于把所有 /api/admin/** 一刀切成"只有超级管理员能用"，
                // 树洞管理页面对非 admin 角色（审核员、运营）永远 403 —— 这正是"好像只有 admin 可以"的根因。
                // 拆细粒度权限码后必须在这里放开，否则方法级注解根本没机会生效。
                .requestMatchers("/api/admin/**").authenticated()
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

