# HTTP 通信与核心机制详解

> **覆盖范围**：前后端通信协议、JWT 生命周期、跨域、Session、Redis、AOP、集合  
> **前端项目**：permission-ui（Vue 3 + Pinia + Axios + Element Plus）  
> **后端项目**：permission-admin（Spring Boot 3.2 + Spring Security 6 + MyBatis-Plus + Redis）  
> **生成时间**：2026-07-15

---

## 目录

- [一、前后端通信整体架构](#一前后端通信整体架构)
- [二、前端 Axios 拦截器设计](#二前端-axios-拦截器设计)
- [三、JWT Token 生命周期（含图示）](#三jwt-token-生命周期含图示)
- [四、JWT 在前端存在哪里](#四jwt-在前端存在哪里)
- [五、Cookie 与 Session：完全不用](#五cookie-与-session完全不用)
- [六、跨域（CORS）配置](#六跨域cors配置)
- [七、Redis 在后端的 6 大作用](#七redis-在后端的-6-大作用)
- [八、AOP 操作日志切面详解](#八aop-操作日志切面详解)
- [九、「获取方法签名」是什么](#九获取方法签名是什么)
- [十、全项目集合类型详解](#十全项目集合类型详解)
- [十一、完整 HTTP 时序图](#十一完整-http-时序图)
- [十二、关键设计决策 FAQ](#十二关键设计决策-faq)
- [十三、代码快速定位索引](#十三代码快速定位索引)

---

## 一、前后端通信整体架构

```
┌────────────────────────────────────────────────────────────────────────┐
│                              浏览器 (Vue 3 SPA)                         │
│                                                                        │
│  ┌──────────────────┐    ┌──────────────────┐    �┌──────────────────┐ │
│  │  Pinia Store     │    │  localStorage     │    │  Vue Router      │ │
│  │  (响应式内存)     │◄──►│  (磁盘持久化)     │    │  (动态路由)      │ │
│  │                  │    │                   │    │                  │ │
│  │  token           │    │  accessToken      │    │  静态路由        │ │
│  │  refreshToken    │    │  refreshToken     │    │  动态 addRoute   │ │
│  │  permissions[]   │    └──────────────────┘    └──────────────────┘ │
│  │  roles[]         │                                                  │
│  └──────────────────┘                                                  │
│         ▲                                  ▲                            │
│         │ setToken()                       │ generateRoutes()          │
│         │                                  │                            │
│  ┌──────┴──────────────────────────────────┴──────┐                    │
│  │              Axios 实例 + 拦截器                 │                    │
│  │                                                 │                    │
│  │  请求拦截器: localStorage → Authorization 头    │                    │
│  │  响应拦截器: code===401 → 清空 + 跳登录        │                    │
│  └──────────────────────┬──────────────────────────┘                    │
└─────────────────────────┼───────────────────────────────────────────────┘
                          │
                          │ HTTP / HTTPS (RESTful JSON)
                          │ baseURL: '/api'
                          │
          ┌───────────────┤───────────────┐
          │   开发环境     │   生产环境     │
          │   Vite Proxy  │   nginx 反代   │
          │   /api→:8080  │   统一域名     │
          └───────────────┘───────────────┘
                          │
                          ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     Spring Boot 后端 (:8080)                            │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │                  Spring Security Filter Chain                      │ │
│  │                                                                    │ │
│  │   WebMvcConfig (CORS) ──► SecurityConfig (规则) ──►              │ │
│  │   JwtAuthenticationFilter (Token 解析 + 黑名单 + 加载权限)         │ │
│  │                                                                    │ │
│  │   放行: /login /refresh /captcha /doc.html /api/auth/**           │ │
│  │   需认证: .anyRequest().authenticated()                            │ │
│  │   方法级: @PreAuthorize("hasAnyAuthority('xxx', 'admin')")         │ │
│  └──────────────────────────────────────────────────────────────────┘ │
│                                    │                                    │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │                   Controller 层 (6 个)                             │ │
│  │   Auth / User / Role / Menu / Dept / Log                         │ │
│  │   @RestController + @RequestMapping                              │ │
│  └──────────────────────────────────────────────────────────────────┘ │
│                                    │                                    │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │                   Service 层 (6 接口 + 6 实现)                     │ │
│  │   业务逻辑 + @Transactional + Redis 操作                          │ │
│  └────────────────────────┬─────────────────────┬───────────────────┘ │
│                           │                     │                       │
│              ┌────────────┤                     ├────────────┐          │
│              ▼            │                     │            ▼          │
│        ┌──────────┐       │                     │      ┌──────────┐    │
│        │  MySQL   │       │                     │      │  Redis   │    │
│        │  持久化   │       │                     │      │  临时状态 │    │
│        └──────────┘       │                     │      └──────────┘    │
│  ┌────────────────────────┴─────────────────────────────────────────┐  │
│  │              AOP 切面: OperationLogAspect                        │  │
│  │              @Around("@annotation(OperationLog)")                │  │
│  │              自动记录操作日志                                    │  │
│  └──────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

**一句话总结**：前后端完全分离、无状态、纯 JWT + localStorage 鉴权、不依赖 Session/Cookie。

---

## 二、前端 Axios 拦截器设计

### 文件位置：`permission-ui/src/api/index.ts`

```typescript
const service = axios.create({
  baseURL: '/api',       // 所有请求前缀 /api
  timeout: 30000,        // 30 秒超时
});

// ====== 请求拦截器 ======
service.interceptors.request.use(
  (config) => {
    // 每次请求前从 localStorage 取 Token，放进 Authorization 头
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ====== 响应拦截器 ======
service.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== 200) {          // 业务错误码非 200
      ElMessage.error(res.message || '请求失败');
      if (res.code === 401) {        // 未授权
        localStorage.clear();
        window.location.href = '/login';
      }
      return Promise.reject(new Error(res.message || '请求失败'));
    }
    return res;
  },
  (error) => {                       // HTTP 层面错误
    if (error.response?.status === 401) {  // HTTP 401
      localStorage.clear();
      window.location.href = '/login';
    }
    ElMessage.error(error.message || '网络异常');
    return Promise.reject(error);
  }
);
```

### 效果

| 场景 | 拦截器行为 |
|------|-----------|
| 发请求前 | 自动带上 `Authorization: Bearer eyJhbG...` |
| 返回 `code=200` | 正常放行，业务拿到 `res.data` |
| 返回 `code=401` | 清空 localStorage，跳 `/login` |
| 返回 `code=4xx/5xx`（非 200） | `ElMessage.error` 弹提示 |
| HTTP 401 | 清空 localStorage，跳 `/login` |
| 网络断开 | 弹"网络异常" |

---

## 三、JWT Token 生命周期（含图示）

### 3.1 Access Token vs Refresh Token

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Access Token (访问令牌)                       │
│                                                                     │
│   有效期: 2 小时 (7200 秒)                                           │
│   包含: userId (sub), username, permissions, roles, iat, exp        │
│   存放: localStorage + 每次请求头                                    │
│   用途: 鉴权原材料，前端每个请求都带                                  │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                        Refresh Token (刷新令牌)                      │
│                                                                     │
│   有效期: 7 天 (604800 秒)                                          │
│   包含: userId (sub), type="refresh"                                │
│   存放: localStorage（不出现在请求头）                               │
│   用途: Access 过期后换取新的双 Token                                │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 Token 状态流转

```
               用户登录成功
                  │
                  ▼
┌─────────────────────────────────────┐
│  accessToken  ◄─────── 活跃 (2h)     │─────── 每次 API 请求带上
│  refreshToken ─────── 休眠 (7d)     │
└─────────────────────────────────────┘
                  │
              2h 后 access 过期
                  │
                  ▼
         ┌──────────────────┐
   401   │  前端自动调用     │
   ◄──── │  refreshAction() │
         └──────────────────┘
                  │
                  ▼    POST /api/auth/refresh { refreshToken }
         ┌──────────────────┐
         │ 后端校验旧 RT     │
         │  签发新的双 Token │
         └──────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│  newAccessToken  ◄─── 新活跃 (2h)   │   旧 AT 加入 Redis 黑名单
│  newRefreshToken ◄─── 新休眠 (7d)   │   旧 RT 仍然有效(未作废)
└─────────────────────────────────────┘
                  │
        用户退出登录 → 旧 AT 加入黑名单(直至过期)
```

### 3.3 Redis 黑名单机制

```
退出时:
  key   = blacklist:{accessToken}
  value = "1"
  TTL   = Token 剩余有效期 (秒)

每次请求:
  JwtAuthenticationFilter:
    ├─ EXISTS blacklist:{token}
    │   ├─ true  → 返回 401（已被吊销）
    │   └─ false → 继续校验签名 + 加载权限
    ▼
```

---

## 四、JWT 在前端存在哪里

### 答案：**localStorage（磁盘持久化） + Pinia Store（内存响应式）**

### 4.1 双写机制

```typescript
// permission-ui/src/stores/user.ts

export const useUserStore = defineStore('user', () => {
  // Pinia 响应式变量（内存，页面刷新时丢失）
  const token = ref<string>(localStorage.getItem('accessToken') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')

  function setToken(accessToken: string, refreshTokenVal: string) {
    token.value = accessToken                                    // 更新内存
    refreshToken.value = refreshTokenVal
    localStorage.setItem('accessToken', accessToken)              // 持久化
    localStorage.setItem('refreshToken', refreshTokenVal)
  }

  function clearToken() {
    token.value = ''                                            // 清内存
    refreshToken.value = ''
    localStorage.removeItem('accessToken')                       // 清磁盘
    localStorage.removeItem('refreshToken')
  }

  // ...
})
```

### 4.2 生命周期对比

| 操作 | Pinia Store | localStorage | 效果 |
|------|-------------|--------------|------|
| 登录 `setToken()` | ✅ 写入 | ✅ 写入 | 登录态建立 |
| 页面内跳转 | ✅ 保持 | ✅ 保持 | Token 仍在 |
| **F5 刷新页面** | ❌ 丢失 | ✅ 保持 | 初始化时从 localStorage 读取 |
| **关闭标签页再打开** | ❌ 丢失 | ✅ 保持 | 重新读取 localStorage，用户保持登录 |
| **退出 `clearToken()`** | ✅ 清空 | ✅ 清空 | 用户回到未登录 |
| 收到 401 | — | ✅ `.clear()` | 拦截器强制清空 + 跳登录 |

### 4.3 为什么不用 Cookie / SessionStorage / Vuex？

| 方案 | 本项目是否使用 | 原因 |
|------|--------------|------|
| **Cookie** | ❌ 不用 | 会被浏览器自动携带（CSRF 风险），且 `httpOnly` Cookie JS 读不到，无法在 Axios 中灵活操作 |
| **SessionStorage** | ❌ 不用 | 关闭标签页即丢失，用户每次打开都要重新登录 |
| **Vuex** | ❌ 不用 | Vue 3 官方推荐 Pinia，更轻量、TypeScript 友好 |
| **内存变量 (ref) 单独用** | ❌ 不够 | 刷新即丢 |
| **Pinia + localStorage** ✅ 组合用 | ✅ | Pinia 管响应式，localStorage 管持久化，互补 |

---

## 五、Cookie 与 Session：完全不用

### 5.1 后端 Session 禁用配置

```java
// SecurityConfig.java
http
  .sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // ← 禁用 Session
```

```java
// Spring Security 默认行为:
//   SessionCreationPolicy.IF_REQUIRED (默认:需要时创建)
//   SessionCreationPolicy.NEVER     (不创建但如果有会读)
//   SessionCreationPolicy.ALWAYS     (总是创建)
//   SessionCreationPolicy.STATELESS  (← 本项目:完全禁用)
```

### 5.2 后端 CSRF 禁用

```java
http.csrf(AbstractHttpConfigurer::disable)  // ← 禁用 CSRF 防护
```

因为 CSRF 防护依赖 Cookie 中的 `XSRF-TOKEN` 对比请求头，既然不用 Cookie，CSRF 就无需开启。

### 5.3 禁用 Session 的影响（全链路）

```
            无 Session 环境
                 │
    ┌────────────┼────────────┐
    ▼            ▼            ▼
 Session      HttpSession    JSESSIONID
 不创建       对象不存在      Cookie不发
    │
    ├─ SecurityContextHolder 每次都从 Token 重新加载
    ├─ 服务端无需存任何 "当前用户" 信息
    ├─ 多台后端实例无需共享 Session (天然分布式)
    └─ 水平扩展只需加机器，无 Session 同步问题
```

### 5.4 为什么不用 Session？

| 指标 | Session + Cookie | JWT + localStorage（当前） |
|------|-----------------|---------------------------|
| 服务端状态 | 有（需 Session 存储） | 无（Token 自包含） |
| 分布式部署 | 需 Session 共享/粘滞 | 天然支持 |
| CSRF 风险 | 有（需额外防护） | 无（手动带 header） |
| XSS 风险 | 较低（httpOnly cookie） | 较高（JS 可读 localStorage） |
| 服务端吊销 | 易（清 Session） | 难（需 Token 黑名单） |
| 跨域 | 复杂（SameSite + 凭证） | 简单（CORS 放行） |
| 接口鉴权 | 请求头 Cookie 自动发送 | 手动在请求拦截器设 header |

> **结论**：前后端分离 + 分布式场景下更适合 JWT + localStorage；单体应用 + 高安全需求更适合 Session + httpOnly Cookie。

---

## 六、跨域（CORS）配置

### 6.1 后端实际配置

#### WebMvcConfig（实际生效的跨域配置）

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")                // 仅 /api/** 允许跨域
                .allowedOriginPatterns("*")           // 任意来源（正则）
                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)               // 允许凭证
                .maxAge(3600);                        // OPTIONS 缓存 1h
    }
}
```

#### SecurityConfig（显式关闭 Security 层 CORS）

```java
http.cors(AbstractHttpConfigurer::disable)  // ← 关闭 Security 的 CORS 处理
```

**为什么两处都要配？** Spring Security 如果检测到跨域请求会走自己的 CORS 处理流程，可能与 WebMvc 配置冲突，因此干脆关 Security 层，统一由 WebMvc 处理。

### 6.2 `allowedOriginPatterns("*")` vs `allowedOrigins("*")` 的区别

```java
// ❌ 当 allowCredentials(true) 时:
.allowedOrigins("*")
// → Spring 抛异常: "When allowCredentials is true, allowedOrigins cannot contain '*'"

// ✅ 正确写法:
.allowedOriginPatterns("*")  // 正则匹配模式，可以与 credentials=true 共存
```

> **Spring 5.3+** 引入的 `allowedOriginPattern` 解决了 `credentials + 通配符` 冲突问题。

### 6.3 浏览器跨域完整流程

```
                 浏览器                               服务器
                   │                                   │
   简单请求(GET/POST): │                                   │
                   │──── POST /api/user ───────────────>│
                   │    Origin: http://localhost:5173    │
                   │                                   │
                   │<── Access-Control-Allow-Origin: * ─│
                   │    Access-Control-Allow-Credentials: true
                   │                                   │
   预检请求(OPTIONS): │                                   │
                   │──── OPTIONS /api/user ────────────>│
                   │    Origin: http://localhost:5173   │
                   │    Access-Control-Request-Method: PUT
                   │                                   │
                   │<── Access-Control-Allow-Methods: * ─│
                   │    Access-Control-Allow-Headers: * │
                   │    Access-Control-Max-Age: 3600    │
                   │                                   │
   后续请求:         │                                   │
                   │──── PUT /api/user/1 ─────────────>│  (1h 内不再预检)
                   │    Origin: http://localhost:5173   │
```

### 6.4 生产环境建议

```java
// 不要 *，而是精确指定:
.allowedOriginPatterns(
    "https://admin.example.com",
    "https://*.example.com"
)
```

---

## 七、Redis 在后端的 6 大作用

### 7.1 Token 缓存加速鉴权

```java
// SysUserServiceImpl.login() 签发 Token 后:
redisTemplate.opsForValue().set(
    SecurityConstants.TOKEN_CACHE_PREFIX + accessToken,  // key: "token:eyJ..."
    loginUser,                                           // value: LoginUser (JSON)
    SecurityConstants.TOKEN_EXPIRE,                      // TTL: 7200s
    TimeUnit.SECONDS);
```

> **注意**：当前实现每次请求都重新 `loadUserById` 从 DB 查权限，所以这块缓存**没有直接参与鉴权**。它作为"可加速"储备，未来可以扩展成 Token → User 的快速查找。

### 7.2 Token 黑名单（退出即吊销）

```
key:    blacklist:{accessToken}
value:  "1"
TTL:    Token 剩余有效期 (秒)

流程:
  1. 退出登录 → Redis SET blacklist:xxx "1" TTL=剩余秒数
  2. JwtAuthenticationFilter: EXISTS blacklist:xxx → true → 401
  3. TTL 到后 Redis 自动删除，节省内存

必要性: JWT 本身无法废止，黑名单是最轻量的吊销手段
```

### 7.3 登录限流（防暴力破解）

```
key:    rate_limit:login:{clientIP}
value:  递增整数 (INCR)
TTL:    60 秒

逻辑:
  第 1 次请求 → INCR 1 → SET EX 60
  第 5 次请求 → INCR 5 → 放行
  第 6 次请求 → INCR 6 > 5 → 抛出 RATE_LIMITED
  60 秒后 → key 自动过期，计数清零

必要性: 无需数据库、纳秒级响应、原子性保证(INCR)
```

### 7.4 账号临时锁定（连续失败保护）

```
key:    login_fail:{username}
value:  递增整数 (INCR)
TTL:    30 分钟

逻辑:
  密码错误 → INCR → 检查是否 >= 5 → 是则拒绝
  登录成功 → DEL
  30 分钟内未继续失败 → 自动过期
```

### 7.5 验证码缓存

```
key:    captcha:{uuid}                (UUID.randomUUID())
value:  验证码文本 ("AbCd")
TTL:    5 分钟

流程:
  1. GET /api/auth/captcha → 生成 + SET captcha:uuid "AbCd" EX 300
  2. POST /api/auth/login → GET captcha:uuid → 比较 → DEL captcha:uuid
  3. 验证成功即删除（一次性使用）
```

### 7.6 前端对应的 Redis Key 一览表

| 业务 | Key 格式 | Value | TTL | 相关常量 |
|------|---------|-------|-----|---------|
| Token 缓存 | `token:{jwt}` | LoginUser JSON | 7200s | TOKEN_CACHE_PREFIX |
| Token 黑名单 | `blacklist:{jwt}` | "1" | 剩余秒数 | TOKEN_BLACKLIST_PREFIX |
| 登录限流 | `rate_limit:login:{ip}` | 整数 | 60s | LOGIN_RATE_LIMIT_PREFIX |
| 登录失败计数 | `login_fail:{user}` | 整数 | 1800s | LOGIN_FAIL_PREFIX |
| 验证码 | `captcha:{uuid}` | 验证码文本 | 300s | CAPTCHA_PREFIX |

### 7.7 Redis 序列化配置

```java
// RedisConfig.java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    // Key 使用 String 序列化（redis-cli 可直接阅读）
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    // Value 使用 JSON 序列化（可读性远高于 JDK 序列化）
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.afterPropertiesSet();
    return template;
}
```

### 7.8 Redis 丢失了所有数据会怎样？

| 数据 | 丢失影响 | 严重度 |
|------|---------|--------|
| Token 缓存 | 每次多查一次 DB（功能正常） | 低 |
| Token 黑名单 | 退出登录的 Token 恢复可用 | **高** |
| 登录限流 | 暴力破解防护失效 | **高** |
| 登录失败计数 | 账号锁定失效 | **高** |
| 验证码 | 用户无法登录 | **高** |

> **结论**：Redis 在此项目是"快查层"，不承载持久化数据，但丢失会严重影响安全能力。

---

## 八、AOP 操作日志切面详解

### 8.1 切面结构图

```
                ┌──────────────────────────────────────────┐
                │        @Aspect @Component               │
                │   Pointcut: @Around("@annotation(       │
                │              operationLog)")             │
                │                                          │
                │  触发条件: 方法标注了 @OperationLog 注解   │
                │  匹配范围: Controller 中带此注解的方法     │
                └──────────────┬───────────────────────────┘
                               │
   ┌──────────────────────────────────────────────────────────┐
   │  @Around (环绕通知)                                      │
   │                                                           │
   │  1. startTime = System.currentTimeMillis()                 │
   │  2. module / value 来自 @OperationLog 注解                │
   │  3. 获取方法签名 → logEntry.method = 类名.方法名           │
   │  4. RequestContextHolder 取 request → URL / IP             │
   │  5. 参数 → JSON → 脱敏 password → logEntry.requestParams  │
   │  6. SecurityContextHolder → LoginUser → operator          │
   │     ── joinPoint.proceed() ──→ 执行业务方法               │
   │     ←── 拿到 return 值                                    │
   │  7. executeTime = 当前时间 - startTime                    │
   │  8. responseResult = JSON(返回值)                         │
   │  9. status = 1(成功) / 0(失败)                             │
   │ 10. insert(logEntry)，try-catch 保底(不阻塞业务)         │
   └──────────────────────────────────────────────────────────┘

### 8.2 核心代码逻辑


### 8.3 什么时候触发这个切面

有 `@OperationLog` 注解的方法 -> 触发切面；没有 -> 不触发。
规律：增/删/改操作标注解，读操作不标。

### 8.4 AOP 解决了什么问题

不用 AOP：每个写方法都要手动写日志代码（重复、易遗漏）。
用 AOP：一个注解搞定，业务代码零侵入，切面逻辑集中可维护。

### 8.5 AOP 与系统其他组件的交互

- RequestContextHolder  -> 取 HTTP 请求信息
- SecurityContextHolder -> 取当前登录用户
- JSONUtil              -> 参数序列化 + 脱敏
- joinPoint.proceed()   -> 执行目标方法
- operationLogMapper    -> 写入 DB

---

## 九、「获取方法签名」是什么

### 9.1 问题背景

`OperationLogAspect` 中这段代码的含义：

```java
MethodSignature signature = (MethodSignature) joinPoint.getSignature();
logEntry.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());
```

### 9.2 一句话回答

**「获取方法签名」就是拿到被 AOP 切面拦截的那个方法的「全限定类名 + 方法名」**，
用于在操作日志里记录「哪个类的哪个方法被调用了」。

### 9.3 逐步拆解

```
ProceedingJoinPoint joinPoint
   |
   +-.getSignature()       -> Signature 接口（通用）
   |
   +-(MethodSignature)     -> 强转（AOP 专用子接口）
   |
   +- 可用方法:
       +-.getDeclaringTypeName() -> "com.permission.controller.UserController"
       +-.getName()              -> "create"
       +-.getReturnType()        -> R.class
       +-.getParameterNames()    -> ["user"]

拼接结果: "com.permission.controller.UserController.create"
```

### 9.4 实际日志内容示例

```
controller.UserController.create    OK    耗时 23ms  操作人 admin
controller.RoleController.delete   FAIL   耗时 5ms   操作人 admin (角色已分配用户)
service.impl.SysUser.login         OK     耗时 120ms 操作人 unknown
```

运维出问题一眼能定位到代码位置。

### 9.5 为什么需要 Signature？

Spring AOP 运行期生成的代理对象拦截调用 -> 执行切面 -> proceed() -> 真正方法。
切面不知道谁在调用它，joinPoint.getSignature() 就是答案。
Method 签名 -> 方法名/类名/参数类型 -> 唯一标识一个方法。

### 9.6 为什么强转安全？

本项目全是方法拦截 `@Around("@annotation(xxx)")`，
所以 getSignature() 必是 MethodSignature（方法维度），强转安全。


---

## 十、全项目集合类型详解

### 10.1 java.util.List

- MyBatis-Plus 查询返回 selectList(wrapper) — Service 层所有查询
- IPage.getRecords() 获取当前页数据 — pageUsers(), pageRoles()
- 构建树形结构递归收集 — buildTree() x2 (Menu/Dept)
- Aspect 提取请求参数 — joinPoint.getArgs() 也是 Object[]

### 10.2 java.util.Set

- LoginUser.permissions 权限标识不重复 — DTO 实体
- LoginUser.roles 角色编码不重复 — DTO 实体
- getAuthorities() 合并去重 — LoginUser.getAuthorities()
- Service 返回角色/菜单 ID — getUserRoleIds(), getRoleMenuIds()
- Controller 接收分配角色入参 — @RequestBody Map<String, Set<Long>>

### 10.3 java.util.Map

- JWT Claims 扩展信息 — createAccessToken() 中 .claim(key, value)
- 请求体 refreshToken — @RequestBody Map<String, String>
- 修改密码/状态请求 — Map<String, String/Integer>
- 分配角色请求 — Map<String, Set<Long>>
- 部门名称缓存映射 — Stream -> Collectors.toMap()

### 10.4 IPage — MyBatis-Plus 分页

- getRecords() -> List<T>  当前页数据
- getTotal()   -> long     总记录数
- getSize()    -> long     每页大小
- getCurrent() -> long     当前页码
- getPages()   -> long     总页数

### 10.5 前端集合对应 TS 类型

- ref<string[]>([])  -> Java List<String>
- reactive({})       -> Java 对象 (DTO/VO)
- Record<string, Function> -> Java Map<String, Object>

### 10.6 集合使用总结表

| 集合 | Java 用途 | 前端对应 |
|------|---------|---------|
| ArrayList | selectList() / buildTree() | ref([]) |
| HashSet | permissions / roles 去重 | ref([]) |
| HashMap | Claims / @RequestBody | Record |
| Set<String> | getUserRoleIds() | ref([]) |
| IPage | selectPage() | PageResult<T> |
| UUID | UUID.randomUUID() | — |


---

## 十一、完整 HTTP 时序图

### 11.1 一次「新增用户」的完整生命周期

```
用户浏览器                    Axios                  Spring 后端                  Redis/DB
    |                          |                        |                          |
    | 点击新增用户              | POST /api/user         |                          |
    |------------------------->| Authorization: Bearer  |                          |
    |                          | { username, ... }      |                          |
    |                          |----------------------->| JwtAuthenticationFilter: |
    |                          |                        |   resolveToken() -> 解析  |
    |                          |                        |   EXISTS blacklist       |-> Redis
    |                          |                        |   loadUserById()         |
    |                          |                        |   SELECT sys_user        |-> DB
    |                          |                        |   SecurityContext 设置   |
    |                          |                        | @PreAuthorize(admin)?    |
    |                          |                        | UserController.create()  |
    |                          |                        |   password=null          |
    |                          |                        |   createUser()           |
    |                          |                        |   validatePassword()     |
    |                          |                        |   INSERT sys_user        |-> DB
    |                          |                        |                          |
    |                          |                        | OperationLogAspect:      |
    |                          |                        |   @Around 织入           |
    |                          |                        |   脱敏 password -> ***   |
    |                          |                        |   INSERT sys_operation_log|->DB
    |                          |<-----------------------| R.ok() JSON              |
    |<-------------------------| res.code===200 放行    |                          |
    | ElMessage 成功提示        |                        |                          |
```

### 11.2 401 自动跳登录

```
Axios 请求                    后端返回 401
    |                              |
    |  GET /api/user/page          |
    |  Authorization: Bearer eyJ.. |
    |----------------------------->|
    |                              | Token 黑名单 / 签名无效 / 过期
    |<-----------------------------| HTTP 401
    |                              |
    | 响应拦截器:
    |  error.response.status === 401
    |  localStorage.clear()
    |  window.location.href = '/login'
```

---

## 十二、关键设计决策 FAQ

### Q1: 为什么不用 Session？
> 无状态、分布式友好、多台实例无需共享 Session。

### Q2: 为什么是 localStorage 不是 Cookie？
> localStorage 不会被浏览器自动携带（无 CSRF 风险），纯前端控制灵活。

### Q3: 为什么每次 request 都重新 loadUserById？
> 正确性优先——角色被管理员修改后立即生效。

### Q4: Redis 丢失了所有数据会怎样？
> Token 缓存丢失 -> 功能正常（多查 DB）；黑名单丢失 -> 退出 Token 恢复可用；
> 验证码/限流/锁定丢失 -> 对应防护失效。

### Q5: AOP 日志写失败为什么不抛异常？
> 切面是横切关注点，绝不应该影响主业务。已用 try-catch 包裹，失败只打 WARN。

### Q6: MethodSignature 强转安全吗？
> 本项目全是方法拦截 @Around(@annotation(xxx))，必是 MethodSignature，强转安全。

### Q7: @PreAuthorize 怎么校验？
> LoginUser.getAuthorities() 把 permissions + roles 都转成 SimpleGrantedAuthority，
> hasAnyAuthority(system:user:add, admin) 任一匹配就放行。

### Q8: 前端按钮权限怎么实现？
> 自定义指令 v-permission：mounted 时检查 hasPermission()，无权直接从 DOM 移除。

---

## 十三、代码快速定位索引

| 模块 | 文件 | 作用 |
|------|------|------|
| 前端 | src/api/index.ts | Axios 实例、拦截器、Authorization 头注入 |
| 前端 | src/api/auth.ts | login/captcha/refresh/logout |
| 前端 | src/stores/user.ts | Pinia token/permissions/roles |
| 前端 | src/stores/permission.ts | 动态路由生成 |
| 前端 | src/router/index.ts | Hash 路由 + addRoute |
| 前端 | src/directives/permission.ts | v-permission 按钮权限指令 |
| 前端 | src/views/login/index.vue | 登录页、验证码刷新 |
| 前端 | src/views/layout/index.vue | 主框架、菜单渲染、修改密码 |
| 后端 | framework/security/JwtTokenProvider.java | 密钥@Value+HS256 |
| 后端 | framework/filter/JwtAuthenticationFilter.java | Token 解析+黑名单 |
| 后端 | framework/config/SecurityConfig.java | 无状态+permitAll+@EP |
| 后端 | framework/config/RedisConfig.java | 序列化 |
| 后端 | system/service/impl/SysUserServiceImpl.java | 缓存/限流/锁定/黑名单 |
| 后端 | common/annotation/OperationLog.java | 自定义注解 |
| 后端 | system/aspect/OperationLogAspect.java | 切面实现 |
| 后端 | common/dto/LoginUser.java | Set<String> permissions/roles |
| 后端 | entity/SysMenu/SysDept | List<T> children |
