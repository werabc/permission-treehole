# permission-api 模块详解

> **模块名称**：Permission API  
> **Artifact ID**：`permission-api`  
> **包名根路径**：`com.permission`  
> **职责**：应用启动入口、Controller 接口层、配置文件  
> **依赖**：permission-system（传递依赖 common + framework）、knife4j、lombok  
> **Spring Boot Maven Plugin**：打包为可执行 JAR

---

## 模块文件清单

| 文件 | 类型 | 说明 |
|------|------|------|
| `PermissionApplication.java` | 启动类 | Spring Boot 应用入口 |
| `controller/AuthController.java` | 控制器 | 认证管理（验证码、登录、刷新、退出、用户信息） |
| `controller/UserController.java` | 控制器 | 用户管理 |
| `controller/RoleController.java` | 控制器 | 角色管理 |
| `controller/MenuController.java` | 控制器 | 菜单管理 |
| `controller/DeptController.java` | 控制器 | 部门管理 |
| `controller/LogController.java` | 控制器 | 日志管理 |
| `resources/application.yml` | 配置 | 应用配置文件 |

---

## 一、PermissionApplication.java — 应用启动类

**路径**：`com.permission.PermissionApplication`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@SpringBootApplication` | Spring Boot | 组合注解（@Configuration + @ComponentScan + @EnableAutoConfiguration） |

```java
@SpringBootApplication
public class PermissionApplication {
    public static void main(String[] args) {
        SpringApplication.run(PermissionApplication.class, args);
    }
}
```

### 设计意图
- 默认扫描 `com.permission` 包及其所有子包（覆盖 common、framework、system、api 所有模块）
- 自动装配 Spring Boot 内置组件（Tomcat、Jackson、Validation 等）

---

## 二、application.yml — 应用配置

**路径**：`src/main/resources/application.yml`

### 配置详解

```yaml
server:
  port: 8080                      # 服务端口 8080

spring:
  application:
    name: permission-admin        # 应用名称
  profiles:
    active: dev                   # 激活 dev 配置文件

  # ====== 数据源配置 ======
  datasource:
    url: jdbc:mysql://localhost:3306/permission_admin?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456abc
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:                       # HikariCP 连接池
      minimum-idle: 5             # 最小空闲连接
      maximum-pool-size: 20       # 最大连接数
      idle-timeout: 300000        # 空闲超时 5 分钟
      max-lifetime: 1200000       # 最大生命周期 20 分钟
      connection-timeout: 30000   # 连接超时 30 秒

  # ====== Redis 配置 ======
  data:
    redis:
      host: localhost
      port: 6379
      password:                   # 无密码
      database: 0                 # 使用 0 号库
      lettuce:                    # Lettuce 客户端连接池
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms

  # ====== Jackson 序列化配置 ======
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss   # 日期格式
    time-zone: Asia/Shanghai           # 时区
    default-property-inclusion: non_null  # 不序列化 null 字段

# ====== MyBatis-Plus 配置 ======
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true              # 下划线转驼峰
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # SQL 日志输出到控制台
  global-config:
    db-config:
      logic-delete-field: deleted                   # 逻辑删除字段名
      logic-delete-value: 1                         # 删除值
      logic-not-delete-value: 0                     # 未删除值

# ====== SpringDoc / Swagger 配置 ======
springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs

# ====== Knife4j 配置 ======
knife4j:
  enable: true                    # 启用 Knife4j
  setting:
    language: zh_cn               # 中文界面
```

---

## 三、AuthController.java — 认证管理

**路径**：`com.permission.controller.AuthController`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Tag(name = "认证管理")` | Swagger/OpenAPI 3 | API 文档分组标签 |
| `@RestController` | Spring MVC | 控制器（@Controller + @ResponseBody） |
| `@RequestMapping("/api/auth")` | Spring MVC | 路由前缀 |
| `@RequiredArgsConstructor` | Lombok | 构造器注入 |
| `@Operation(summary = "...")` | Swagger/OpenAPI 3 | 接口描述 |
| `@GetMapping / @PostMapping` | Spring MVC | HTTP 方法映射 |
| `@Valid` | Jakarta Validation | 触发参数校验 |
| `@RequestBody` | Spring MVC | 绑定请求体 JSON |
| `@RequestHeader` | Spring MVC | 绑定请求头 |
| `@AuthenticationPrincipal` | Spring Security | 注入当前登录用户 |

### 接口清单

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/auth/captcha` | 获取验证码 | 公开 |
| POST | `/api/auth/login` | 用户登录 | 公开 |
| POST | `/api/auth/refresh` | 刷新 Token | 公开 |
| POST | `/api/auth/logout` | 退出登录 | 需认证 |
| GET | `/api/auth/user-info` | 获取当前用户信息 | 需认证 |

### 核心方法详解

#### captcha() — 获取验证码
```java
@Operation(summary = "获取验证码")
@GetMapping("/captcha")
public R<Map<String, Object>> captcha() {
    LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);  // 宽120 高40 4个字符 20条干扰线
    String captchaKey = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set(
            SecurityConstants.CAPTCHA_PREFIX + captchaKey,  // key: "captcha:{uuid}"
            captcha.getCode(),                              // value: 验证码文本
            5, TimeUnit.MINUTES);                           // 5 分钟过期

    Map<String, Object> result = new HashMap<>();
    result.put("captchaKey", captchaKey);
    result.put("captchaImage", captcha.getImageBase64Data());  // Base64 图片（已含 data:image/png;base64, 前缀）
    return R.ok(result);
}
```
- 使用 Hutool 的 `LineCaptcha` 生成线段干扰验证码
- `getImageBase64Data()` 返回完整的 Data URI 格式（`data:image/png;base64,...`），前端可直接用于 `<img src="">`
- 验证码文本存储在 Redis，Key 由 UUID 生成，5 分钟有效

#### login() — 用户登录
```java
@Operation(summary = "用户登录")
@PostMapping("/login")
public R<TokenVO> login(@Valid @RequestBody LoginDTO loginDTO) {
    return R.ok(userService.login(loginDTO));
}
```
- `@Valid` 触发 `LoginDTO` 的 `@NotBlank` 校验
- 委托给 `SysUserService.login()` 处理完整登录流程

#### refresh() — 刷新 Token
```java
@Operation(summary = "刷新 Token")
@PostMapping("/refresh")
public R<TokenVO> refresh(@RequestBody Map<String, String> body) {
    String refreshToken = body.get("refreshToken");
    return R.ok(userService.refreshToken(refreshToken));
}
```

#### logout() — 退出登录
```java
@Operation(summary = "退出登录")
@PostMapping("/logout")
public R<Void> logout(@RequestHeader(SecurityConstants.TOKEN_HEADER) String header) {
    String token = header.replace(SecurityConstants.TOKEN_PREFIX, "");  // 去掉 "Bearer " 前缀
    userService.logout(token);
    return R.ok();
}
```

#### userInfo() — 获取当前用户信息
```java
@Operation(summary = "获取当前用户信息")
@GetMapping("/user-info")
public R<Map<String, Object>> userInfo(@AuthenticationPrincipal LoginUser loginUser) {
    Map<String, Object> info = new HashMap<>();
    info.put("userId", loginUser.getUserId());
    info.put("username", loginUser.getUsername());
    info.put("nickname", loginUser.getNickname());
    info.put("deptId", loginUser.getDeptId());
    info.put("deptName", loginUser.getDeptName());
    info.put("permissions", loginUser.getPermissions());
    info.put("roles", loginUser.getRoles());
    return R.ok(info);
}
```
- `@AuthenticationPrincipal LoginUser` 注入当前认证用户（由 `JwtAuthenticationFilter` 设置到 SecurityContext）

---

## 四、UserController.java — 用户管理

**路径**：`com.permission.controller.UserController`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Tag(name = "用户管理")` | Swagger | API 文档分组 |
| `@RestController` | Spring MVC | 控制器 |
| `@RequestMapping("/api/user")` | Spring MVC | 路由前缀 |
| `@PreAuthorize("hasAnyAuthority(...)")` | Spring Security | 方法级权限控制 |
| `@OperationLog(module = "用户管理", value = "...")` | 项目自定义 | 操作日志标记 |
| `@PathVariable` | Spring MVC | 路径变量 |
| `@RequestParam` | Spring MVC | 查询参数 |

### 接口清单

| 方法 | 路径 | 说明 | 权限标识 |
|------|------|------|---------|
| GET | `/api/user/page` | 分页查询用户 | `system:user:list` 或 `admin` |
| GET | `/api/user/{id}` | 获取用户详情 | `system:user:query` 或 `admin` |
| POST | `/api/user` | 新增用户 | `system:user:add` 或 `admin` |
| PUT | `/api/user/{id}` | 修改用户 | `system:user:edit` 或 `admin` |
| DELETE | `/api/user/{ids}` | 删除用户（批量） | `system:user:delete` 或 `admin` |
| PUT | `/api/user/{id}/status` | 修改用户状态 | `system:user:edit` 或 `admin` |
| PUT | `/api/user/{id}/reset-password` | 重置密码 | `system:user:reset-pwd` 或 `admin` |
| PUT | `/api/user/update-password` | 修改个人密码 | 需认证（无特定权限） |
| PUT | `/api/user/{id}/roles` | 分配角色 | `system:user:edit` 或 `admin` |
| GET | `/api/user/{id}/roles` | 获取用户角色 ID | 需认证 |

### 权限控制示例
```java
@Operation(summary = "新增用户")
@PostMapping
@PreAuthorize("hasAnyAuthority('system:user:add', 'admin')")  // 需要权限标识或 admin 角色
@OperationLog(module = "用户管理", value = "新增用户")           // 自动记录操作日志
public R<Void> create(@Valid @RequestBody SysUser user) {
    userService.createUser(user);
    return R.ok();
}
```

### 设计意图
- `@PreAuthorize` 同时支持权限标识（`system:user:add`）和角色编码（`admin`），因为 `LoginUser.getAuthorities()` 将两者都转为 `SimpleGrantedAuthority`
- `@OperationLog` 注解的方法会被 `OperationLogAspect` 切面拦截，自动记录操作日志
- 修改个人密码（`updatePassword`）不需要特定权限，只需登录（`@AuthenticationPrincipal` 获取当前用户）

---

## 五、RoleController.java — 角色管理

**路径**：`com.permission.controller.RoleController`

### 接口清单

| 方法 | 路径 | 说明 | 权限标识 |
|------|------|------|---------|
| GET | `/api/role/page` | 分页查询角色 | `system:role:list` 或 `admin` |
| GET | `/api/role/{id}` | 获取角色详情 | `system:role:query` 或 `admin` |
| POST | `/api/role` | 新增角色 | `system:role:add` 或 `admin` |
| PUT | `/api/role/{id}` | 修改角色 | `system:role:edit` 或 `admin` |
| DELETE | `/api/role/{ids}` | 删除角色 | `system:role:delete` 或 `admin` |
| PUT | `/api/role/{id}/status` | 修改角色状态 | `system:role:edit` 或 `admin` |
| PUT | `/api/role/{id}/menus` | 分配菜单权限 | `system:role:edit` 或 `admin` |
| GET | `/api/role/{id}/menus` | 获取角色菜单 ID | 需认证 |
| GET | `/api/role/all` | 获取所有角色（下拉框） | 需认证 |

### 核心方法示例
```java
@Operation(summary = "分配菜单权限")
@PutMapping("/{id}/menus")
@PreAuthorize("hasAnyAuthority('system:role:edit', 'admin')")
@OperationLog(module = "角色管理", value = "分配权限")
public R<Void> assignMenus(@PathVariable Long id, @RequestBody Map<String, Set<Long>> body) {
    roleService.assignMenus(id, body.get("menuIds"));
    return R.ok();
}
```

---

## 六、MenuController.java — 菜单管理

**路径**：`com.permission.controller.MenuController`

### 接口清单

| 方法 | 路径 | 说明 | 权限标识 |
|------|------|------|---------|
| GET | `/api/menu/tree` | 获取菜单树 | `system:menu:list` 或 `admin` |
| GET | `/api/menu/tree-select` | 获取菜单树（下拉选择） | 需认证 |
| GET | `/api/menu/user-menus` | 获取用户菜单（动态路由） | 需认证 |
| GET | `/api/menu/{id}` | 获取菜单详情 | `system:menu:query` 或 `admin` |
| POST | `/api/menu` | 新增菜单 | `system:menu:add` 或 `admin` |
| PUT | `/api/menu/{id}` | 修改菜单 | `system:menu:edit` 或 `admin` |
| DELETE | `/api/menu/{id}` | 删除菜单 | `system:menu:delete` 或 `admin` |

### 核心方法示例
```java
@Operation(summary = "获取用户菜单（动态路由）")
@GetMapping("/user-menus")
public R<List<SysMenu>> userMenus(@AuthenticationPrincipal LoginUser loginUser) {
    return R.ok(menuService.getUserMenus(loginUser.getUserId()));
}
```
- 根据用户 ID 查询其拥有的所有菜单，构建为树形结构
- 前端根据此数据动态生成路由和导航菜单

---

## 七、DeptController.java — 部门管理

**路径**：`com.permission.controller.DeptController`

### 接口清单

| 方法 | 路径 | 说明 | 权限标识 |
|------|------|------|---------|
| GET | `/api/dept/tree` | 获取部门树 | `system:dept:list` 或 `admin` |
| GET | `/api/dept/tree-select` | 获取部门树（下拉选择） | 需认证 |
| GET | `/api/dept/{id}` | 获取部门详情 | `system:dept:query` 或 `admin` |
| POST | `/api/dept` | 新增部门 | `system:dept:add` 或 `admin` |
| PUT | `/api/dept/{id}` | 修改部门 | `system:dept:edit` 或 `admin` |
| DELETE | `/api/dept/{id}` | 删除部门 | `system:dept:delete` 或 `admin` |

---

## 八、LogController.java — 日志管理

**路径**：`com.permission.controller.LogController`

### 接口清单

| 方法 | 路径 | 说明 | 权限标识 |
|------|------|------|---------|
| GET | `/api/log/operation/page` | 分页查询操作日志 | `system:log:list` 或 `admin` |
| GET | `/api/log/login/page` | 分页查询登录日志 | `system:log:list` 或 `admin` |

### 核心方法示例
```java
@Operation(summary = "分页查询操作日志")
@GetMapping("/operation/page")
@PreAuthorize("hasAnyAuthority('system:log:list', 'admin')")
public R<?> operationLogPage(@RequestParam(defaultValue = "1") long pageNum,
                              @RequestParam(defaultValue = "10") long pageSize,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String module,
                              @RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate) {
    return R.ok(operationLogService.pageLogs(pageNum, pageSize, keyword, module, startDate, endDate));
}
```

---

## 九、Controller 层设计模式总结

### 9.1 统一响应
所有接口返回 `R<T>` 类型：
```json
{ "code": 200, "message": "操作成功", "data": { ... } }
```

### 9.2 权限控制模式
```java
@PreAuthorize("hasAnyAuthority('system:xxx:yyy', 'admin')")
```
- 每个操作接口都标注权限标识
- `admin` 角色编码作为超级管理员绕过所有权限检查
- 查询类接口（如 `getUserRoleIds`、`getRoleMenuIds`、`getAll`）只需登录

### 9.3 操作日志模式
```java
@OperationLog(module = "模块名", value = "操作描述")
```
- 写操作（增/删/改）标注 `@OperationLog`
- 读操作不标注（避免日志量过大）

### 9.4 参数校验模式
```java
@Valid @RequestBody SysUser user    // @RequestBody 参数
@Min(value = 1) private long pageNum // 参数对象内部校验
```

### 9.5 当前用户注入模式
```java
@AuthenticationPrincipal LoginUser loginUser  // 获取当前登录用户
```

---

## 十、API 端点汇总

| 模块 | 端点数量 | 路径前缀 |
|------|---------|---------|
| 认证 | 5 | `/api/auth/**` |
| 用户 | 10 | `/api/user/**` |
| 角色 | 9 | `/api/role/**` |
| 菜单 | 7 | `/api/menu/**` |
| 部门 | 6 | `/api/dept/**` |
| 日志 | 2 | `/api/log/**` |
| **合计** | **39** | — |

### 公开接口（无需认证）
- `GET /api/auth/captcha`
- `POST /api/auth/login`
- `POST /api/auth/refresh`

### 需认证但无需特定权限
- `POST /api/auth/logout`
- `GET /api/auth/user-info`
- `PUT /api/user/update-password`
- `GET /api/user/{id}/roles`
- `GET /api/role/{id}/menus`
- `GET /api/role/all`
- `GET /api/menu/tree-select`
- `GET /api/menu/user-menus`
- `GET /api/dept/tree-select`