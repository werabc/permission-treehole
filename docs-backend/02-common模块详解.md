# permission-common 模块详解

> **模块名称**：Permission Common  
> **Artifact ID**：`permission-common`  
> **包名根路径**：`com.permission.common`  
> **职责**：提供整个项目共享的实体类、DTO、枚举、常量、注解、异常、通用响应封装等基础组件  
> **依赖**：spring-boot-starter-web、spring-boot-starter-validation、spring-boot-starter-security、mybatis-plus-spring-boot3-starter、jjwt-api、hutool-all、lombok

---

## 模块文件清单

| 文件 | 类型 | 说明 |
|------|------|------|
| `BaseEntity.java` | 抽象基类 | 实体公共字段（id、createTime、updateTime、deleted） |
| `R.java` | 通用响应 | 统一返回结果封装 |
| `ResultCode.java` | 枚举 | 业务状态码枚举 |
| `annotation/OperationLog.java` | 注解 | 操作日志标记注解 |
| `constant/SecurityConstants.java` | 常量接口 | 安全相关常量 |
| `dto/LoginDTO.java` | DTO | 登录请求参数 |
| `dto/LoginUser.java` | DTO | Spring Security 用户详情实现 |
| `dto/PageDTO.java` | DTO | 分页查询参数 |
| `dto/TokenVO.java` | VO | Token 响应对象 |
| `entity/SysDept.java` | 实体 | 部门 |
| `entity/SysLoginLog.java` | 实体 | 登录日志 |
| `entity/SysMenu.java` | 实体 | 菜单/权限 |
| `entity/SysOperationLog.java` | 实体 | 操作日志 |
| `entity/SysRole.java` | 实体 | 角色 |
| `entity/SysRoleMenu.java` | 实体 | 角色-菜单关联 |
| `entity/SysUser.java` | 实体 | 用户 |
| `entity/SysUserRole.java` | 实体 | 用户-角色关联 |
| `enums/DataScope.java` | 枚举 | 数据范围 |
| `enums/MenuType.java` | 枚举 | 菜单类型 |
| `enums/UserStatus.java` | 枚举 | 用户状态 |
| `exception/BusinessException.java` | 异常 | 业务异常 |

---

## 一、BaseEntity.java — 实体基类

**路径**：`com.permission.common.BaseEntity`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Getter` / `@Setter` | Lombok | 自动生成 getter/setter |
| `implements Serializable` | Java | 序列化支持（Redis 缓存需要） |
| `@TableId(type = IdType.AUTO)` | MyBatis-Plus | 主键自增策略 |
| `@TableField(fill = FieldFill.INSERT)` | MyBatis-Plus | 插入时自动填充 |
| `@TableField(fill = FieldFill.INSERT_UPDATE)` | MyBatis-Plus | 插入和更新时自动填充 |
| `@TableLogic` | MyBatis-Plus | 逻辑删除标记 |

### 字段说明
```java
@TableId(type = IdType.AUTO)
private Long id;                                  // 主键，数据库自增

@TableField(fill = FieldFill.INSERT)
private LocalDateTime createTime;                 // 创建时间，插入时由 MetaObjectHandler 自动填充

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updateTime;                 // 更新时间，插入和更新时自动填充

@TableLogic
@TableField(fill = FieldFill.INSERT)
private Integer deleted;                          // 逻辑删除：0-正常，1-删除（自动填充默认值 0）
```

### 设计意图
所有业务实体（SysUser、SysRole、SysMenu、SysDept）继承此基类，获得统一的：
- 主键策略（AUTO 自增）
- 审计字段（createTime、updateTime）自动填充
- 逻辑删除能力（`deleted` 字段 + `@TableLogic` 使 MyBatis-Plus 的 `delete` 变为 `UPDATE SET deleted=1`，`select` 自动加 `WHERE deleted=0`）

---

## 二、R.java — 统一响应封装

**路径**：`com.permission.common.R`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Data` | Lombok | 生成 getter/setter/toString/equals/hashCode |
| `@NoArgsConstructor` | Lombok | 无参构造 |
| `@AllArgsConstructor` | Lombok | 全参构造 |
| `implements Serializable` | Java | 序列化 |

### 核心结构
```java
public class R<T> implements Serializable {
    private int code;       // 状态码（200 成功，其他见 ResultCode）
    private String message; // 提示消息
    private T data;         // 业务数据（泛型）
}
```

### 静态工厂方法
| 方法 | 用途 |
|------|------|
| `R.ok()` | 成功，无数据 |
| `R.ok(T data)` | 成功，带数据 |
| `R.ok(String message, T data)` | 成功，自定义消息 + 数据 |
| `R.fail()` | 失败，默认 500 |
| `R.fail(String message)` | 失败，自定义消息 |
| `R.fail(ResultCode resultCode)` | 失败，使用枚举 |
| `R.fail(ResultCode resultCode, String message)` | 失败，枚举 + 自定义消息 |
| `R.fail(int code, String message)` | 失败，自定义 code + 消息 |

### 设计意图
所有 Controller 返回 `R<T>`，保证前后端交互格式一致。`GlobalExceptionHandler` 也统一返回 `R<Void>`。

---

## 三、ResultCode.java — 业务状态码枚举

**路径**：`com.permission.common.ResultCode`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Getter` | Lombok | 生成 getter |
| `@AllArgsConstructor` | Lombok | 全参构造（枚举构造用） |

### 状态码定义
```java
// HTTP 标准码
SUCCESS(200, "操作成功"),
BAD_REQUEST(400, "请求参数错误"),
UNAUTHORIZED(401, "未授权，请先登录"),
FORBIDDEN(403, "无权限访问"),
NOT_FOUND(404, "资源不存在"),
INTERNAL_ERROR(500, "服务器内部错误"),

// 用户认证相关 1001-1010
USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误"),
USER_ACCOUNT_LOCKED(1002, "账号已被锁定"),
USER_ACCOUNT_DISABLED(1003, "账号已被禁用"),
TOKEN_EXPIRED(1004, "Token已过期"),
TOKEN_INVALID(1005, "Token无效"),
OLD_PASSWORD_ERROR(1006, "原密码错误"),
CAPTCHA_ERROR(1007, "验证码错误"),
PASSWORD_WEAK(1008, "密码强度不足：至少8位，包含大小写字母、数字和特殊字符"),
RATE_LIMITED(1009, "请求过于频繁，请稍后重试"),
ACCOUNT_TEMP_LOCKED(1010, "账号已被临时锁定，请30分钟后再试"),

// 角色相关 2001-2003
ROLE_NAME_EXISTS(2001, "角色名称已存在"),
ROLE_CODE_EXISTS(2002, "角色编码已存在"),
ROLE_HAS_USERS(2003, "角色已分配用户，无法删除"),

// 菜单相关 3001
MENU_NAME_EXISTS(3001, "菜单名称已存在"),

// 部门相关 4001-4003
DEPT_NAME_EXISTS(4001, "部门名称已存在"),
DEPT_HAS_CHILDREN(4002, "部门存在子部门，无法删除"),
DEPT_HAS_USERS(4003, "部门下存在用户，无法删除"),

// 通用业务 5001-5002
DATA_EXISTS(5001, "数据已存在"),
DATA_FORBIDDEN(5002, "无权操作此数据");
```

### 设计意图
将业务错误码集中管理，避免魔法数字。`BusinessException` 和 `GlobalExceptionHandler` 都引用此枚举。

---

## 四、OperationLog.java — 操作日志注解

**路径**：`com.permission.common.annotation.OperationLog`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Target(ElementType.METHOD)` | Java | 注解只能用于方法 |
| `@Retention(RetentionPolicy.RUNTIME)` | Java | 运行时保留，可通过反射读取 |
| `@Documented` | Java | 包含在 Javadoc 中 |

### 注解定义
```java
public @interface OperationLog {
    String value() default "";    // 操作描述（用于 action 字段）
    String module() default "";   // 模块名称
    String action() default "";   // 操作类型（备用）
}
```

### 使用方式
```java
@OperationLog(module = "用户管理", value = "新增用户")
public R<Void> create(@Valid @RequestBody SysUser user) { ... }
```

### 设计意图
配合 `OperationLogAspect`（AOP 切面），在标注了 `@OperationLog` 的方法执行前后自动记录操作日志（模块、方法、URL、参数、返回值、耗时、操作人、IP 等），实现非侵入式审计。

---

## 五、SecurityConstants.java — 安全常量接口

**路径**：`com.permission.common.constant.SecurityConstants`

### 常量定义
```java
// Token 相关
String TOKEN_PREFIX = "Bearer ";                    // Token 前缀
String TOKEN_HEADER = "Authorization";              // 请求头名称
String TOKEN_CACHE_PREFIX = "token:";               // Redis 缓存前缀
String TOKEN_BLACKLIST_PREFIX = "blacklist:";       // 黑名单前缀
String CAPTCHA_PREFIX = "captcha:";                 // 验证码前缀
String LOGIN_FAIL_PREFIX = "login_fail:";           // 登录失败计数前缀
String LOGIN_RATE_LIMIT_PREFIX = "rate_limit:login:"; // 限流前缀

// URL
String LOGIN_URL = "/api/auth/login";
String REFRESH_TOKEN_URL = "/api/auth/refresh";
String LOGOUT_URL = "/api/auth/logout";
String CAPTCHA_URL = "/api/auth/captcha";

// JWT
String JWT_SECRET = "permission-admin-secret-key-2024-must-be-long-enough-for-hs256";
long TOKEN_EXPIRE = 7200;              // Access Token 有效期：2 小时（秒）
long REFRESH_TOKEN_EXPIRE = 604800;    // Refresh Token 有效期：7 天（秒）

// 安全策略
int MAX_LOGIN_FAIL_COUNT = 5;          // 最大连续失败次数
int ACCOUNT_LOCK_MINUTES = 30;         // 账户锁定时间（分钟）
int LOGIN_RATE_LIMIT_MAX = 5;          // 登录限流阈值（次）
int LOGIN_RATE_LIMIT_WINDOW = 60;      // 限流时间窗口（秒）
```

### 设计意图
使用 Java `interface` 定义常量（隐式 `public static final`），集中管理所有安全相关配置，避免散落在各处的魔法值。

---

## 六、LoginDTO.java — 登录请求 DTO

**路径**：`com.permission.common.dto.LoginDTO`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Data` | Lombok | 简化代码 |
| `@NotBlank(message = "...")` | Jakarta Validation | 非空校验 |

### 字段
```java
@NotBlank(message = "用户名不能为空")
private String username;

@NotBlank(message = "密码不能为空")
private String password;

private String captchaKey;     // 验证码 Key（可选）
private String captchaCode;    // 验证码值（可选）
```

### 设计意图
封装登录请求参数，`@NotBlank` 配合 `@Valid` 在 Controller 层自动触发参数校验，校验失败由 `GlobalExceptionHandler` 捕获 `MethodArgumentNotValidException` 返回 400。

---

## 七、LoginUser.java — Spring Security 用户详情

**路径**：`com.permission.common.dto.LoginUser`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Data` | Lombok | 简化代码 |
| `@NoArgsConstructor` | Lombok | 无参构造 |
| `@Builder` | Lombok | 建造者模式 |
| `implements UserDetails` | Spring Security | 安全用户详情接口 |

### 核心字段
```java
private Long userId;              // 用户 ID
private String username;          // 用户名
private String password;          // 密码（BCrypt 加密）
private String nickname;          // 昵称
private Long deptId;              // 部门 ID
private String deptName;          // 部门名称（非持久化字段）
private Integer dataScope;        // 数据范围（取角色最小值）
private List<Long> deptIds;       // 数据范围部门 ID 列表
private Set<String> permissions;  // 权限标识集合（如 "system:user:list"）
private Set<String> roles;        // 角色编码集合（如 "admin"）
```

### UserDetails 接口实现
```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    Set<GrantedAuthority> authorities = new HashSet<>();
    if (permissions != null) {
        permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
    }
    if (roles != null) {
        roles.forEach(r -> authorities.add(new SimpleGrantedAuthority(r)));
    }
    return authorities;
}
```
将 `permissions` 和 `roles` 都转为 `SimpleGrantedAuthority`，因此 `@PreAuthorize("hasAnyAuthority('system:user:list', 'admin')")` 可以同时匹配权限标识和角色编码。

```java
@Override
public boolean isAccountNonExpired() { return true; }      // 账户永不过期
@Override
public boolean isAccountNonLocked() { return true; }       // 账户永不锁定（锁定逻辑由 Redis 实现）
@Override
public boolean isCredentialsNonExpired() { return true; }  // 凭证永不过期
@Override
public boolean isEnabled() { return true; }                // 启用状态由登录时检查
```

### 设计意图
`LoginUser` 是 Spring Security 上下文中的核心对象，存储了当前登录用户的所有安全相关信息。`JwtAuthenticationFilter` 将其放入 `SecurityContextHolder`，Controller 通过 `@AuthenticationPrincipal` 注入。

---

## 八、PageDTO.java — 分页查询参数

**路径**：`com.permission.common.dto.PageDTO`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Data` | Lombok | 简化代码 |
| `@Min(value = 1, message = "...")` | Jakarta Validation | 最小值校验 |

### 字段
```java
@Min(value = 1, message = "页码最小为1")
private long pageNum = 1;     // 当前页码，默认 1

@Min(value = 1, message = "每页条数最小为1")
private long pageSize = 10;   // 每页条数，默认 10

private String keyword;       // 关键字搜索
```

---

## 九、TokenVO.java — Token 响应对象

**路径**：`com.permission.common.dto.TokenVO`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Data` | Lombok | 简化代码 |
| `@Builder` | Lombok | 建造者模式 |
| `@NoArgsConstructor` / `@AllArgsConstructor` | Lombok | 构造方法 |

### 字段
```java
private String accessToken;    // 访问令牌
private String refreshToken;   // 刷新令牌
private long expiresIn;        // 有效期（秒）
```

---

## 十、实体类详解

### 10.1 SysDept.java — 部门实体

**路径**：`com.permission.common.entity.SysDept`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Data` | Lombok | 简化代码 |
| `@EqualsAndHashCode(callSuper = true)` | Lombok | 包含父类字段的 equals/hashCode |
| `@TableName("sys_dept")` | MyBatis-Plus | 指定数据库表名 |
| `@TableField(exist = false)` | MyBatis-Plus | 非数据库字段 |

### 字段
```java
// 继承自 BaseEntity: id, createTime, updateTime, deleted

private String deptName;     // 部门名称
private Long parentId;       // 父部门 ID（顶级为 0）
private String ancestors;    // 祖级列表（如 "0,1,2"）
private Integer sort;        // 排序序号
private String leader;       // 负责人
private String phone;        // 联系电话
private String email;        // 邮箱
private Integer status;      // 状态：0-禁用，1-启用

@TableField(exist = false)
private List<SysDept> children;  // 子部门列表（非数据库字段，用于树形结构）
```

### 设计意图
`ancestors` 字段存储从根到父节点的 ID 路径，便于快速查询所有子孙部门。`children` 用于在内存中递归构建树形结构。

---

### 10.2 SysUser.java — 用户实体

**路径**：`com.permission.common.entity.SysUser`

### 使用的注解/技术
同 SysDept（`@Data`、`@EqualsAndHashCode(callSuper = true)`、`@TableName`、`@TableField(exist = false)`）

### 字段
```java
// 继承自 BaseEntity: id, createTime, updateTime, deleted

private String username;          // 用户名（唯一）
private String password;          // 密码（BCrypt 加密存储）
private String nickname;          // 昵称
private String email;             // 邮箱
private String phone;             // 手机号
private String avatar;            // 头像 URL
private Integer sex;              // 性别：0-未知，1-男，2-女
private Integer status;           // 状态：0-禁用，1-启用
private Long deptId;              // 所属部门 ID

@TableField(exist = false)
private String deptName;          // 部门名称（非持久化，查询时填充）

private LocalDateTime lastLoginTime;  // 最后登录时间
private String lastLoginIp;           // 最后登录 IP
```

---

### 10.3 SysRole.java — 角色实体

**路径**：`com.permission.common.entity.SysRole`

### 字段
```java
// 继承自 BaseEntity: id, createTime, updateTime, deleted

private String roleName;          // 角色名称
private String roleCode;          // 角色编码（唯一，如 "admin"）
private String roleDesc;          // 角色描述
private Integer dataScope;        // 数据范围：1-全部，2-本部门及子部门，3-本部门，4-自定义，5-仅本人
private Integer status;           // 状态：0-禁用，1-启用

@TableField(exist = false)
private String dataScopeName;     // 数据范围名称（非持久化）

@TableField(exist = false)
private String deptIds;           // 部门 ID 列表（非持久化，逗号分隔）
```

---

### 10.4 SysMenu.java — 菜单/权限实体

**路径**：`com.permission.common.entity.SysMenu`

### 字段
```java
// 继承自 BaseEntity: id, createTime, updateTime, deleted

private Long parentId;            // 父菜单 ID
private String menuName;          // 菜单名称
private String menuType;          // 菜单类型：CATALOG-目录，MENU-菜单，BUTTON-按钮
private String path;              // 路由地址
private String component;         // 组件路径
private String icon;              // 图标
private String permission;        // 权限标识（如 "system:user:list"）
private Integer sort;             // 排序
private Integer status;           // 状态：0-禁用，1-启用
private Integer visible;          // 是否可见：0-隐藏，1-可见

@TableField(exist = false)
private List<SysMenu> children;   // 子菜单列表（非持久化）
```

### 设计意图
`menuType` 区分三级结构：目录（一级导航）→ 菜单（页面）→ 按钮（操作权限）。`permission` 字段存储权限标识，用于 `@PreAuthorize` 方法级鉴权。

---

### 10.5 SysOperationLog.java — 操作日志实体

**路径**：`com.permission.common.entity.SysOperationLog`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Data` | Lombok | 简化代码 |
| `@TableName("sys_operation_log")` | MyBatis-Plus | 指定表名 |
| `@TableId(type = IdType.AUTO)` | MyBatis-Plus | 主键自增（不继承 BaseEntity） |
| `implements Serializable` | Java | 序列化 |

### 字段
```java
@TableId(type = IdType.AUTO)
private Long id;

private String module;            // 操作模块（如 "用户管理"）
private String action;            // 操作类型（如 "新增用户"）
private String method;            // 执行方法全路径
private String requestUrl;        // 请求 URL
private String requestMethod;     // 请求方式（GET/POST/PUT/DELETE）
private String requestParams;     // 请求参数（JSON 字符串）
private String responseResult;    // 响应结果（JSON 字符串）
private Long executeTime;         // 执行时长（毫秒）
private String operator;          // 操作人用户名
private String operatorIp;        // 操作人 IP
private Integer status;           // 状态：0-失败，1-成功
private String errorMsg;          // 错误信息
private LocalDateTime createTime; // 操作时间
```

### 设计意图
不继承 `BaseEntity`，因为日志表不需要逻辑删除和自动填充。由 `OperationLogAspect` 切面自动写入。

---

### 10.6 SysLoginLog.java — 登录日志实体

**路径**：`com.permission.common.entity.SysLoginLog`

### 字段
```java
@TableId(type = IdType.AUTO)
private Long id;

private String username;          // 登录用户名
private String ip;                // 登录 IP
private String location;          // 登录地点
private String browser;           // 浏览器
private String os;                // 操作系统
private Integer status;           // 状态：0-失败，1-成功
private String message;           // 提示信息
private LocalDateTime loginTime;  // 登录时间
```

---

### 10.7 SysRoleMenu.java — 角色-菜单关联实体

**路径**：`com.permission.common.entity.SysRoleMenu`

### 字段
```java
private Long roleId;   // 角色 ID
private Long menuId;   // 菜单 ID
```
数据库表使用 `(role_id, menu_id)` 联合主键。

---

### 10.8 SysUserRole.java — 用户-角色关联实体

**路径**：`com.permission.common.entity.SysUserRole`

### 字段
```java
private Long userId;   // 用户 ID
private Long roleId;   // 角色 ID
```
数据库表使用 `(user_id, role_id)` 联合主键。

---

## 十一、枚举类详解

### 11.1 DataScope.java — 数据范围枚举

**路径**：`com.permission.common.enums.DataScope`

```java
ALL(1, "全部数据"),           // 可查看所有数据
DEPT_AND_SUB(2, "本部门及子部门"), // 本部门 + 所有下级部门
DEPT(3, "本部门"),            // 仅本部门
CUSTOM(4, "自定义"),          // 自定义部门范围
SELF(5, "仅本人");            // 仅自己的数据
```

### 设计意图
`SysUserServiceImpl.buildLoginUser()` 取用户所有角色中 `dataScope` 值**最小**的作为最终数据范围（值越小权限越大）。

---

### 11.2 MenuType.java — 菜单类型枚举

**路径**：`com.permission.common.enums.MenuType`

```java
CATALOG(0, "目录"),   // 一级导航目录
MENU(1, "菜单"),      // 二级页面菜单
BUTTON(2, "按钮");    // 按钮级操作权限
```

---

### 11.3 UserStatus.java — 用户状态枚举

**路径**：`com.permission.common.enums.UserStatus`

```java
ENABLED(1, "启用"),
DISABLED(0, "禁用");
```

---

## 十二、BusinessException.java — 业务异常

**路径**：`com.permission.common.exception.BusinessException`

### 使用的注解/技术
| 注解 | 来源 | 作用 |
|------|------|------|
| `@Getter` | Lombok | 生成 getter |
| `extends RuntimeException` | Java | 非受检异常 |

### 构造方法
```java
public BusinessException(ResultCode resultCode)                        // 使用枚举消息
public BusinessException(ResultCode resultCode, String message)       // 枚举 code + 自定义消息
public BusinessException(int code, String message)                    // 完全自定义
```

### 设计意图
业务逻辑中抛出 `BusinessException`，由 `GlobalExceptionHandler` 统一捕获并转为 `R.fail(code, message)` 返回给前端。`code` 字段使前端能精确区分错误类型。

---

## 十三、模块总结

`permission-common` 是整个项目的**基石模块**，提供了：

1. **统一响应格式**（`R<T>`）— 前后端交互契约
2. **业务错误码**（`ResultCode`）— 集中管理
3. **实体基类**（`BaseEntity`）— 审计字段 + 逻辑删除
4. **安全用户对象**（`LoginUser`）— Spring Security 集成
5. **业务异常**（`BusinessException`）— 异常驱动的错误处理
6. **操作日志注解**（`@OperationLog`）