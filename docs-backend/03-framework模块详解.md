# permission-framework 模块详解

> **模块名称**：Permission Framework  
> **Artifact ID**：`permission-framework`  
> **包名根路径**：`com.permission.framework`  
> **职责**：提供安全框架（Spring Security + JWT）、全局配置（MyBatis-Plus、Redis、Web MVC）、全局异常处理、安全处理器等基础设施  
> **依赖**：permission-common、spring-boot-starter-security、spring-boot-starter-data-redis、spring-boot-starter-aop、jjwt-api/impl/jackson、hutool-all、lombok

---

## 模块文件清单

| 文件 | 类型 | 说明 |
|------|------|------|
| `config/MyBatisPlusConfig.java` | 配置类 | MyBatis-Plus 分页、拦截器、自动填充 |
| `config/RedisConfig.java` | 配置类 | Redis 序列化配置 |
| `config/SecurityConfig.java` | 配置类 | Spring Security 核心配置 |
| `config/WebMvcConfig.java` | 配置类 | CORS 跨域配置 |
| `filter/JwtAuthenticationFilter.java` | 过滤器 | JWT 认证过滤器 |
| `handler/GlobalExceptionHandler.java` | 处理器 | 全局异常处理 |
| `handler/SecurityAccessDeniedHandler.java` | 处理器 | 403 拒绝访问处理 |
| `handler/SecurityAuthenticationEntryPoint.java` | 处理器 | 401 未认证处理 |
| `security/CustomUserDetailsService.java` | 接口 | 自定义 UserDetailsService 接口 |
| `security/JwtTokenProvider.java` | 工具类 | JWT Token 生成与解析 |

---

## 一、MyBatisPlusConfig.java — MyBatis-Plus 配置

**路径**：`com.permission.framework.config.MyBatisPlusConfig`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Configuration` | Spring | 声明为配置类 |
| `@EnableTransactionManagement` | Spring | 开启注解事务管理（使 `@Transactional` 生效） |
| `@MapperScan("com.permission.system.mapper")` | MyBatis-Plus | 扫描 Mapper 接口包路径 |
| `@Bean` | Spring | 声明 Bean |

### 核心逻辑

#### 1. MyBatisPlusInterceptor（分页 + 防全表更新）
```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    // 分页拦截器（MySQL 方言）
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    // 防全表更新/删除拦截器（阻止无 WHERE 条件的 UPDATE/DELETE）
    interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
    return interceptor;
}
```
- `PaginationInnerInterceptor`：自动将 `Page` 对象转为 `LIMIT` 分页 SQL
- `BlockAttackInnerInterceptor`：防止恶意全表更新/删除操作

#### 2. MetaObjectHandler（自动填充审计字段）
```java
@Bean
public MetaObjectHandler metaObjectHandler() {
    return new MetaObjectHandler() {
        @Override
        public void insertFill(MetaObject metaObject) {
            this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
            this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        }
    };
}
```
- **插入时**：自动填充 `createTime`、`updateTime` 为当前时间，`deleted` 为 0
- **更新时**：自动填充 `updateTime` 为当前时间
- `strictInsertFill`：严格模式，如果字段已有值则不覆盖

### 设计意图
将 MyBatis-Plus 的核心配置集中管理，使所有实体继承 `BaseEntity` 后自动获得审计字段填充和逻辑删除能力。

---

## 二、RedisConfig.java — Redis 配置

**路径**：`com.permission.framework.config.RedisConfig`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Configuration` | Spring | 配置类 |
| `@Bean` | Spring | 声明 Bean |

### 核心逻辑
```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    // Key 和 HashKey 使用 String 序列化
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    // Value 和 HashValue 使用 JSON 序列化
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.afterPropertiesSet();
    return template;
}
```

### 设计意图
- **Key 使用 String 序列化**：便于 Redis 客户端直接查看 key
- **Value 使用 JSON 序列化**：支持存储复杂对象（如 `LoginUser`），可读性好
- 默认的 `JdkSerializationRedisSerializer` 会产生二进制不可读数据，此处替换为 JSON

### Redis 在本项目中的用途
| 用途 | Key 模式 | 过期时间 |
|------|---------|---------|
| Token 缓存 | `token:{accessToken}` | 7200s（2h） |
| Token 黑名单 | `blacklist:{accessToken}` | 剩余有效期 |
| 验证码 | `captcha:{uuid}` | 300s（5min） |
| 登录失败计数 | `login_fail:{username}` | 1800s（30min） |
| 登录限流 | `rate_limit:login:{ip}` | 60s |

---

## 三、SecurityConfig.java — Spring Security 核心配置

**路径**：`com.permission.framework.config.SecurityConfig`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Configuration` | Spring | 配置类 |
| `@EnableWebSecurity` | Spring Security | 启用 Web 安全 |
| `@EnableMethodSecurity` | Spring Security | 启用方法级安全（`@PreAuthorize`） |
| `@RequiredArgsConstructor` | Lombok | 生成包含 `final` 字段的构造器（用于构造器注入） |
| `@Bean` | Spring | 声明 Bean |

### 核心逻辑

#### 1. SecurityFilterChain（安全过滤器链）
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)                    // 禁用 CSRF（无状态 JWT 不需要）
        .cors(AbstractHttpConfigurer::disable)                    // 禁用 Spring Security 的 CORS（使用 WebMvcConfig）
        .sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // 无状态会话
        .exceptionHandling(ex -> ex
            .accessDeniedHandler(accessDeniedHandler)             // 403 处理
            .authenticationEntryPoint(authenticationEntryPoint))   // 401 处理
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(SecurityConstants.LOGIN_URL, 
                             SecurityConstants.REFRESH_TOKEN_URL).permitAll()  // 登录/刷新放行
            .requestMatchers("/doc.html", "/swagger-ui/**", 
                             "/v3/api-docs/**", "/webjars/**").permitAll()    // Swagger 放行
            .requestMatchers("/api/auth/**").permitAll()                      // 认证接口放行
            .anyRequest().authenticated())                                     // 其他需要认证
        .addFilterBefore(jwtAuthenticationFilter, 
                         UsernamePasswordAuthenticationFilter.class);  // JWT 过滤器加在用户名密码过滤器之前

    return http.build();
}
```

**关键设计点**：
- `SessionCreationPolicy.STATELESS`：不使用 Session，每个请求独立认证
- `addFilterBefore`：将 `JwtAuthenticationFilter` 放在 `UsernamePasswordAuthenticationFilter` 之前，优先使用 JWT 认证
- 放行路径：登录、刷新 Token、Swagger 文档、所有 `/api/auth/**` 接口

#### 2. PasswordEncoder
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```
使用 BCrypt 哈希算法（加盐 + 自适应计算强度），密码存储不可逆。

#### 3. AuthenticationManager
```java
@Bean
public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
}
```
暴露 `AuthenticationManager` 为 Bean，供 `SysUserServiceImpl.login()` 调用 `authenticate()` 方法进行用户名密码认证。

---

## 四、WebMvcConfig.java — CORS 跨域配置

**路径**：`com.permission.framework.config.WebMvcConfig`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Configuration` | Spring | 配置类 |
| `implements WebMvcConfigurer` | Spring MVC | 自定义 MVC 配置 |

### 核心逻辑
```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
            .allowedOriginPatterns("*")                    // 允许所有来源
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")                           // 允许所有请求头
            .allowCredentials(true)                        // 允许携带 Cookie
            .maxAge(3600);                                 // 预检请求缓存 1 小时
}
```

### 设计意图
前后端分离架构下，前端域名与后端不同，需要配置 CORS。`maxAge(3600)` 减少 OPTIONS 预检请求频率。

---

## 五、JwtAuthenticationFilter.java — JWT 认证过滤器

**路径**：`com.permission.framework.filter.JwtAuthenticationFilter`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Slf4j` | Lombok | 日志 |
| `@Component` | Spring | 声明为组件 |
| `@RequiredArgsConstructor` | Lombok | 构造器注入 |
| `extends OncePerRequestFilter` | Spring | 确保每个请求只执行一次 |

### 核心逻辑

#### doFilterInternal 方法
```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    String token = resolveToken(request);  // 1. 从 Header 提取 Token

    if (StrUtil.isNotBlank(token)) {
        // 2. 检查黑名单
        String blacklistKey = SecurityConstants.TOKEN_BLACKLIST_PREFIX + token;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            filterChain.doFilter(request, response);  // 黑名单中的 Token 直接放行（后续被拒绝）
            return;
        }

        try {
            // 3. 验证 JWT 签名和有效期
            if (jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserId(token);
                // 4. 加载用户详情（含权限）
                LoginUser loginUser = userDetailsService.loadUserById(userId);

                if (loginUser != null) {
                    // 5. 构建认证令牌并设置到 SecurityContext
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                loginUser, null, loginUser.getAuthorities());
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (ExpiredJwtException e) {
            log.debug("Token expired for request: {}", request.getRequestURI());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
        }
    }

    filterChain.doFilter(request, response);  // 继续过滤器链
}
```

#### resolveToken 方法
```java
private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(SecurityConstants.TOKEN_HEADER);  // "Authorization"
    if (StrUtil.isNotBlank(bearerToken) && 
        bearerToken.startsWith(SecurityConstants.TOKEN_PREFIX)) {            // "Bearer "
        return bearerToken.substring(SecurityConstants.TOKEN_PREFIX.length()); // 去掉 "Bearer " 前缀
    }
    return null;
}
```

### 认证流程
1. 从 `Authorization` 请求头提取 `Bearer xxx` 格式的 Token
2. 检查 Redis 黑名单（退出登录的 Token）
3. 验证 JWT 签名和有效期
4. 从数据库加载用户权限信息（`loadUserById`）
5. 将 `LoginUser` 放入 `SecurityContextHolder`
6. 继续过滤器链（后续由 `@PreAuthorize` 进行方法级鉴权）

### 设计意图
- 无状态认证：不依赖 Session，适合分布式部署
- 黑名单机制：退出登录后 Token 在有效期内仍可使用的问题通过 Redis 黑名单解决
- 异常不中断：认证失败不抛异常，继续过滤器链，由后续的 `AuthorizationFilter` 决定放行或拒绝

---

## 六、GlobalExceptionHandler.java — 全局异常处理

**路径**：`com.permission.framework.handler.GlobalExceptionHandler`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Slf4j` | Lombok | 日志 |
| `@RestControllerAdvice` | Spring | 全局异常处理（@ControllerAdvice + @ResponseBody） |
| `@ExceptionHandler(...)` | Spring | 指定处理的异常类型 |
| `@ResponseStatus(...)` | Spring | 指定 HTTP 状态码 |

### 异常处理映射

| 异常类型 | HTTP 状态码 | 返回内容 | 场景 |
|---------|-----------|---------|------|
| `BusinessException` | 200 | `R.fail(code, message)` | 业务逻辑错误 |
| `AccessDeniedException` | 403 | `R.fail(FORBIDDEN)` | 权限不足 |
| `MethodArgumentNotValidException` | 400 | `R.fail(BAD_REQUEST, 字段错误消息)` | `@Valid` 校验失败（@RequestBody） |
| `BindException` | 400 | `R.fail(BAD_REQUEST, 字段错误消息)` | 参数绑定失败（@RequestParam） |
| `ConstraintViolationException` | 400 | `R.fail(BAD_REQUEST, message)` | `@Validated` 校验失败 |
| `MissingServletRequestParameterException` | 400 | `R.fail(BAD_REQUEST, "缺少必要参数")` | 缺少必填参数 |
| `Exception` | 500 | `R.fail(INTERNAL_ERROR)` | 兜底：未知异常 |

### 设计意图
- 所有异常统一返回 `R<T>` 格式，前端只需处理一种响应结构
- 业务异常（`BusinessException`）返回 200 + 业务错误码，由前端根据 code 判断
- 系统异常返回对应的 HTTP 状态码
- 日志分级：业务异常 `warn`，系统异常 `error`

---

## 七、SecurityAccessDeniedHandler.java — 403 处理

**路径**：`com.permission.framework.handler.SecurityAccessDeniedHandler`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Component` | Spring | 声明为组件 |
| `implements AccessDeniedHandler` | Spring Security | 403 处理接口 |

### 核心逻辑
```java
@Override
public void handle(HttpServletRequest request, HttpServletResponse response,
                   AccessDeniedException accessDeniedException) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);  // 403
    response.getWriter().write(JSONUtil.toJsonStr(R.fail(ResultCode.FORBIDDEN)));
}
```

### 设计意图
当已认证用户访问无权限资源时（`@PreAuthorize` 校验失败），返回 JSON 格式的 403 响应，而非默认的 HTML 错误页。

---

## 八、SecurityAuthenticationEntryPoint.java — 401 处理

**路径**：`com.permission.framework.handler.SecurityAuthenticationEntryPoint`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Component` | Spring | 声明为组件 |
| `implements AuthenticationEntryPoint` | Spring Security | 401 处理接口 |

### 核心逻辑
```java
@Override
public void commence(HttpServletRequest request, HttpServletResponse response,
                     AuthenticationException authException) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401
    response.getWriter().write(JSONUtil.toJsonStr(R.fail(ResultCode.UNAUTHORIZED)));
}
```

### 设计意图
当未认证用户访问受保护资源时，返回 JSON 格式的 401 响应。

---

## 九、CustomUserDetailsService.java — 自定义 UserDetailsService 接口

**路径**：`com.permission.framework.security.CustomUserDetailsService`

### 接口定义
```java
public interface CustomUserDetailsService {
    LoginUser loadUserById(Long userId);
}
```

### 设计意图
定义一个通过用户 ID 加载用户详情的接口。`JwtAuthenticationFilter` 依赖此接口（而非通过 Spring Security 的 `UserDetailsService` 加载），因为 JWT 认证时已有 userId，无需再次验证密码。

实现类在 `permission-system` 模块的 `UserDetailsServiceImpl`。

---

## 十、JwtTokenProvider.java — JWT Token 工具类

**路径**：`com.permission.framework.security.JwtTokenProvider`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Slf4j` | Lombok | 日志 |
| `@Component` | Spring | 声明为组件 |

### 核心逻辑

#### 1. 密钥初始化
```java
private final SecretKey secretKey;

public JwtTokenProvider() {
    byte[] keyBytes = Decoders.BASE64.decode(
        "cGVybWlzc2lvbi1hZG1pbi1zZWNyZXQta2V5LTIwMjQtbXVzdC1iZS1sb25nLWVub3VnaC1mb3ItaHMyNTY=");
    this.secretKey = Keys.hmacShaKeyFor(keyBytes);
}
```
- 使用 **HS256**（HMAC-SHA256）算法
- 密钥为 Base64 编码的字符串，解码后生成 `SecretKey`
- 密钥长度满足 HS256 要求（≥ 256 bit）

#### 2. 创建 Access Token
```java
public String createAccessToken(Long userId, String username, Map<String, Object> claims) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + SecurityConstants.TOKEN_EXPIRE * 1000);  // 2h

    JwtBuilder builder = Jwts.builder()
            .subject(String.valueOf(userId))    // sub: 用户 ID
            .issuedAt(now)                      // iat: 签发时间
            .expiration(expiration)             // exp: 过期时间
            .claim("username", username);       // 自定义 claim: 用户名

    if (claims != null) {
        claims.forEach(builder::claim);         // 添加额外 claims（permissions, roles）
    }

    return builder.signWith(secretKey).compact();  // 签名并序列化
}
```

#### 3. 创建 Refresh Token
```java
public String createRefreshToken(Long userId) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + SecurityConstants.REFRESH_TOKEN_EXPIRE * 1000);  // 7d

    return Jwts.builder()
            .subject(String.valueOf(userId))
            .issuedAt(now)
            .expiration(expiration)
            .claim("type", "refresh")           // 标记为 refresh token
            .signWith(secretKey)
            .compact();
}
```

#### 4. 解析 Token
```java
public Claims parseToken(String token) {
    return Jwts.parser()
            .verifyWith(secretKey)              // 验证签名
            .build()
            .parseSignedClaims(token)           // 解析
            .getPayload();                      // 获取 payload（Claims）
}
```

#### 5. 获取用户 ID
```java
public Long getUserId(String token) {
    String subject = parseToken(token).getSubject();
    return Long.valueOf(subject);
}
```

#### 6. 验证 Token
```java
public boolean validateToken(String token) {
    try {
        parseToken(token);
        return true;
    } catch (ExpiredJwtException e) {
        log.debug("JWT token expired: {}", e.getMessage());
        throw e;  // 重新抛出，让上层区分过期和无效
    } catch (JwtException e) {
        log.debug("JWT token invalid: {}", e.getMessage());
        return false;
    }
}
```

#### 7. 获取过期时间
```java
public long getExpiration(String token) {
    return parseToken(token).getExpiration().getTime();
}
```

### 设计意图
- **双 Token 机制**：Access Token 有效期短（2h），Refresh Token 有效期长（7d），平衡安全性和用户体验
- **Refresh Token 标记**：通过 `type=claim` 区分两种 Token，刷新时验证类型
- **异常区分**：`ExpiredJwtException` 重新抛出，让上层返回 `TOKEN_EXPIRED` 而非 `TOKEN_INVALID`

---

## 十一、模块总结

`permission-framework` 是整个项目的**安全基础设施层**，提供了：

1. **认证**：JWT 无状态认证（`JwtTokenProvider` + `JwtAuthenticationFilter`）
2. **授权**：Spring Security + `@PreAuthorize` 方法级权限控制
3. **安全配置**：CSRF 禁用、无状态会话、BCrypt 密码编码
4. **全局异常处理**：统一返回格式 + 分级日志
5. **持久层配置**：MyBatis-Plus 分页、自动填充、逻辑删除
6. **缓存配置**：Redis JSON 序列化
7. **跨域配置**：CORS 全局放行