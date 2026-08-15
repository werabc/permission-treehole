# 数据库与 SQL 详解

> **数据库**：MySQL 8.0.33  
> **字符集**：utf8mb4 / utf8mb4_unicode_ci  
> **存储引擎**：InnoDB  
> **数据库名**：`permission_admin`  
> **表数量**：8 张

---

## 一、数据库表总览

| # | 表名 | 说明 | 类型 |
|---|------|------|------|
| 1 | `sys_dept` | 部门表 | 业务表 |
| 2 | `sys_user` | 用户表 | 业务表 |
| 3 | `sys_role` | 角色表 | 业务表 |
| 4 | `sys_menu` | 菜单/权限表 | 业务表 |
| 5 | `sys_user_role` | 用户-角色关联表 | 关联表 |
| 6 | `sys_role_menu` | 角色-菜单关联表 | 关联表 |
| 7 | `sys_operation_log` | 操作日志表 | 日志表 |
| 8 | `sys_login_log` | 登录日志表 | 日志表 |

### ER 关系图
```
                    ┌──────────────┐
                    │  sys_dept    │
                    │  (部门)      │
                    └──────┬───────┘
                           │ 1:N (dept_id)
                           ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  sys_role    │◄──┤  sys_user    │   │  sys_menu    │
│  (角色)      │   │  (用户)      │   │  (菜单/权限) │
└──────┬───────┘   └──────────────┘   └──────┬───────┘
       │                                      │
       │         ┌──────────────┐             │
       └────────►│ sys_user_role│◄────────────┘
       │         │ (用户-角色)  │
       │         └──────────────┘
       │
       │         ┌──────────────┐
       └────────►│ sys_role_menu│
                 │ (角色-菜单)  │
                 └──────────────┘

┌──────────────┐   ┌──────────────┐
│sys_operation_│   │ sys_login_log│
│log (操作日志)│   │ (登录日志)   │
└──────────────┘   └──────────────┘
```

---

## 二、表结构详解

### 2.1 sys_dept — 部门表

```sql
CREATE TABLE sys_dept (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '部门ID',
    dept_name   VARCHAR(100) NOT NULL COMMENT '部门名称',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父部门ID',
    ancestors   VARCHAR(500) NOT NULL DEFAULT '0' COMMENT '祖级列表',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    leader      VARCHAR(50)  DEFAULT NULL COMMENT '负责人',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_ancestors (ancestors)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';
```

#### 字段说明
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| dept_name | VARCHAR(100) | 部门名称 |
| parent_id | BIGINT | 父部门 ID，顶级为 0 |
| ancestors | VARCHAR(500) | 祖级路径，如 "0,1,2"（从根到父节点） |
| sort | INT | 排序序号 |
| leader | VARCHAR(50) | 负责人姓名 |
| phone | VARCHAR(20) | 联系电话 |
| email | VARCHAR(100) | 邮箱 |
| status | TINYINT | 0-禁用，1-启用 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间（自动更新） |
| deleted | TINYINT | 逻辑删除标记 |

#### 索引
- `idx_parent_id`：加速按父部门查询子部门
- `idx_ancestors`：加速按祖级路径查询所有子孙部门

#### 初始数据
```
1. 总公司 (parent_id=0, ancestors="0")
   ├── 2. 技术部 (parent_id=1, ancestors="0,1")
   │   ├── 5. 前端组 (parent_id=2, ancestors="0,1,2")
   │   └── 6. 后端组 (parent_id=2, ancestors="0,1,2")
   ├── 3. 产品部 (parent_id=1, ancestors="0,1")
   └── 4. 运营部 (parent_id=1, ancestors="0,1")
```

---

### 2.2 sys_user — 用户表

```sql
CREATE TABLE sys_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username        VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password        VARCHAR(200) NOT NULL COMMENT '密码(BCrypt加密)',
    nickname        VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    email           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    avatar          VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    sex             TINYINT      DEFAULT 0 COMMENT '性别: 0-未知 1-男 2-女',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    dept_id         BIGINT       DEFAULT NULL COMMENT '部门ID',
    last_login_time DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip   VARCHAR(50)  DEFAULT NULL COMMENT '最后登录IP',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_dept_id (dept_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

#### 字段说明
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(50) | 用户名（唯一约束） |
| password | VARCHAR(200) | BCrypt 加密后的密码（60 字符） |
| nickname | VARCHAR(50) | 昵称 |
| email | VARCHAR(100) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| avatar | VARCHAR(500) | 头像 URL |
| sex | TINYINT | 0-未知，1-男，2-女 |
| status | TINYINT | 0-禁用，1-启用 |
| dept_id | BIGINT | 所属部门 ID |
| last_login_time | DATETIME | 最后登录时间 |
| last_login_ip | VARCHAR(50) | 最后登录 IP |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | TINYINT | 逻辑删除标记 |

#### 初始数据
| id | username | nickname | dept_id | status |
|----|----------|----------|---------|--------|
| 1 | admin | 超级管理员 | 1 | 1 |
| 2 | tech | 技术负责人 | 2 | 1 |
| 3 | backend | 后端开发 | 2 | 1 |

密码均为 `Admin@1234`（BCrypt 加密）

---

### 2.3 sys_role — 角色表

```sql
CREATE TABLE sys_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    role_code   VARCHAR(50)  NOT NULL UNIQUE COMMENT '角色编码',
    role_desc   VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
    data_scope  TINYINT      DEFAULT 5 COMMENT '数据范围: 1-全部 2-本部门及子部门 3-本部门 4-自定义 5-仅本人',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';
```

#### 字段说明
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| role_name | VARCHAR(50) | 角色名称 |
| role_code | VARCHAR(50) | 角色编码（唯一，如 "admin"） |
| role_desc | VARCHAR(200) | 角色描述 |
| data_scope | TINYINT | 数据范围（1-全部，2-本部门及子部门，3-本部门，4-自定义，5-仅本人） |
| status | TINYINT | 0-禁用，1-启用 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | TINYINT | 逻辑删除标记 |

#### 初始数据
| id | role_name | role_code | data_scope | status |
|----|-----------|-----------|------------|--------|
| 1 | 超级管理员 | admin | 1（全部） | 1 |
| 2 | 技术负责人 | tech_lead | 2（本部门及子部门） | 1 |
| 3 | 普通用户 | user | 5（仅本人） | 1 |

---

### 2.4 sys_menu — 菜单/权限表

```sql
CREATE TABLE sys_menu (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '菜单ID',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    menu_name   VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    menu_type   VARCHAR(10)  NOT NULL DEFAULT 'MENU' COMMENT '菜单类型: CATALOG-目录 MENU-菜单 BUTTON-按钮',
    path        VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
    component   VARCHAR(200) DEFAULT NULL COMMENT '组件路径',
    icon        VARCHAR(100) DEFAULT NULL COMMENT '图标',
    permission  VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    visible     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否可见: 0-隐藏 1-可见',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_permission (permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';
```

#### 字段说明
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| parent_id | BIGINT | 父菜单 ID，顶级为 0 |
| menu_name | VARCHAR(50) | 菜单名称 |
| menu_type | VARCHAR(10) | CATALOG-目录，MENU-菜单，BUTTON-按钮 |
| path | VARCHAR(200) | 前端路由地址（如 "/system/user"） |
| component | VARCHAR(200) | 前端组件路径（如 "system/user/index"） |
| icon | VARCHAR(100) | 图标名称 |
| permission | VARCHAR(100) | 权限标识（如 "system:user:list"） |
| sort | INT | 排序序号 |
| status | TINYINT | 0-禁用，1-启用 |
| visible | TINYINT | 0-隐藏，1-可见 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | TINYINT | 逻辑删除标记 |

#### 初始数据（25 条）
```
1.  系统管理 (CATALOG, /system, Setting)
2.  ├── 用户管理 (MENU, /system/user, User, system:user:list)
3.  │   ├── 用户查询 (BUTTON, system:user:query)
4.  │   ├── 用户新增 (BUTTON, system:user:add)
5.  │   ├── 用户编辑 (BUTTON, system:user:edit)
6.  │   ├── 用户删除 (BUTTON, system:user:delete)
7.  │   └── 重置密码 (BUTTON, system:user:reset-pwd)
8.  ├── 角色管理 (MENU, /system/role, UserFilled, system:role:list)
9.  │   ├── 角色查询 (BUTTON, system:role:query)
10. │   ├── 角色新增 (BUTTON, system:role:add)
11. │   ├── 角色编辑 (BUTTON, system:role:edit)
12. │   └── 角色删除 (BUTTON, system:role:delete)
13. ├── 菜单管理 (MENU, /system/menu, Menu, system:menu:list)
14. │   ├── 菜单查询 (BUTTON, system:menu:query)
15. │   ├── 菜单新增 (BUTTON, system:menu:add)
16. │   ├── 菜单编辑 (BUTTON, system:menu:edit)
17. │   └── 菜单删除 (BUTTON, system:menu:delete)
18. ├── 部门管理 (MENU, /system/dept, OfficeBuilding, system:dept:list)
19. │   ├── 部门查询 (BUTTON, system:dept:query)
20. │   ├── 部门新增 (BUTTON, system:dept:add)
21. │   ├── 部门编辑 (BUTTON, system:dept:edit)
22. │   └── 部门删除 (BUTTON, system:dept:delete)
23. └── 日志管理 (CATALOG, /log, Document)
24.     ├── 操作日志 (MENU, /log/operation, Tickets, system:log:list)
25.     └── 登录日志 (MENU, /log/login, Key, system:log:list)
```

---

### 2.5 sys_user_role — 用户-角色关联表

```sql
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';
```

#### 字段说明
| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | BIGINT | 用户 ID |
| role_id | BIGINT | 角色 ID |

#### 初始数据
| user_id | role_id | 说明 |
|---------|---------|------|
| 1 | 1 | admin → 超级管理员 |
| 2 | 2 | tech → 技术负责人 |
| 2 | 3 | tech → 普通用户 |
| 3 | — | backend 无角色（仅 tech 有普通用户角色） |

---

### 2.6 sys_role_menu — 角色-菜单关联表

```sql
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id),
    INDEX idx_role_id (role_id),
    INDEX idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-菜单关联表';
```

#### 初始数据
- **超级管理员 (role_id=1)**：拥有所有 25 个菜单
- **技术负责人 (role_id=2)**：系统管理 + 用户管理 + 用户查询 + 角色管理 + 角色查询 + 菜单管理 + 菜单查询 + 部门管理 + 部门查询 + 日志管理 + 操作日志 + 登录日志
- **普通用户 (role_id=3)**：系统管理 + 用户管理 + 用户查询 + 角色管理 + 角色查询 + 菜单管理 + 菜单查询 + 部门管理 + 部门查询

---

### 2.7 sys_operation_log — 操作日志表

```sql
CREATE TABLE sys_operation_log (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    module         VARCHAR(50)  DEFAULT NULL COMMENT '操作模块',
    action         VARCHAR(100) DEFAULT NULL COMMENT '操作类型',
    method         VARCHAR(200) DEFAULT NULL COMMENT '执行方法',
    request_url    VARCHAR(200) DEFAULT NULL COMMENT '请求URL',
    request_method VARCHAR(10)  DEFAULT NULL COMMENT '请求方式',
    request_params TEXT         DEFAULT NULL COMMENT '请求参数',
    response_result LONGTEXT    DEFAULT NULL COMMENT '响应结果',
    execute_time   BIGINT       DEFAULT NULL COMMENT '执行时长(ms)',
    operator       VARCHAR(50)  DEFAULT NULL COMMENT '操作人',
    operator_ip    VARCHAR(50)  DEFAULT NULL COMMENT '操作IP',
    status         TINYINT      DEFAULT 1 COMMENT '状态: 0-失败 1-成功',
    error_msg      TEXT         DEFAULT NULL COMMENT '错误信息',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_operator (operator),
    INDEX idx_module (module),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
```

#### 设计意图
- 不继承 `BaseEntity`（无逻辑删除、无自动填充）
- `request_params` 和 `response_result` 使用 TEXT/LONGTEXT 存储 JSON
- 由 `OperationLogAspect` 切面自动写入，无需手动插入

---

### 2.8 sys_login_log — 登录日志表

```sql
CREATE TABLE sys_login_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    username   VARCHAR(50)  DEFAULT NULL COMMENT '用户名',
    ip         VARCHAR(50)  DEFAULT NULL COMMENT '登录IP',
    location   VARCHAR(100) DEFAULT NULL COMMENT '登录地点',
    browser    VARCHAR(100) DEFAULT NULL COMMENT '浏览器',
    os         VARCHAR(50)  DEFAULT NULL COMMENT '操作系统',
    status     TINYINT      DEFAULT 1 COMMENT '状态: 0-失败 1-成功',
    message    VARCHAR(200) DEFAULT NULL COMMENT '提示信息',
    login_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    INDEX idx_username (username),
    INDEX idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';
```

#### 设计意图
- `location`、`browser`、`os` 字段预留，当前版本未填充（可由前端或 UA 解析补充）
- 由 `SysLoginLogService.recordLoginLog()` 在登录成功/失败时自动记录

---

## 三、自定义 SQL 查询

### 3.1 SysUserMapper — 用户相关查询

#### selectRoleCodesByUserId
```sql
SELECT DISTINCT r.role_code 
FROM sys_role r 
INNER JOIN sys_user_role ur ON r.id = ur.role_id 
WHERE ur.user_id = #{userId} 
  AND r.status = 1 
  AND r.deleted = 0
```
获取用户的所有**启用且未删除**的角色编码。

#### selectPermissionsByUserId
```sql
SELECT DISTINCT m.permission 
FROM sys_menu m 
INNER JOIN sys_role_menu rm ON m.id = rm.menu_id 
INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id 
WHERE ur.user_id = #{userId} 
  AND m.status = 1 
  AND m.deleted = 0 
  AND m.permission IS NOT NULL 
  AND m.permission != ''
```
获取用户的所有**启用且未删除**的权限标识（去重）。

### 3.2 SysMenuMapper — 菜单相关查询

#### selectRoleMenusByRoleId
```sql
SELECT * FROM sys_role_menu WHERE role_id = #{roleId}
```
获取角色的所有菜单关联。

#### selectMenusByUserId
```sql
SELECT m.* 
FROM sys_menu m 
INNER JOIN sys_role_menu rm ON m.id = rm.menu_id 
INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id 
WHERE ur.user_id = #{userId} 
  AND m.status = 1 
  AND m.deleted = 0 
ORDER BY m.sort ASC
```
获取用户的所有菜单（通过角色关联，去重 + 排序）。

---

## 四、SQL 脚本说明

### 4.1 create_tables.sql
- 使用 `CREATE TABLE IF NOT EXISTS`（幂等，可重复执行）
- 仅创建表结构，不插入数据

### 4.2 init.sql
- 使用 `DROP TABLE IF EXISTS` + `CREATE TABLE`（先删后建）
- 创建数据库 `permission_admin`
- 创建所有 8 张表
- 插入完整的初始化数据（部门、用户、角色、菜单、关联）

### 4.3 DataInitializer.java
- 应用启动时自动执行（`CommandLineRunner`）
- 检查 `sys_user` 是否有数据，有则跳过
- 动态生成 BCrypt 密码哈希（比 SQL 中的静态哈希更安全）

---

## 五、数据库设计要点

### 5.1 逻辑删除
所有业务表（dept、user、role、menu）都有 `deleted` TINYINT 字段：
- `0` = 正常
- `1` = 已删除

MyBatis-Plus 的 `@TableLogic` 使所有 `SELECT` 自动加 `WHERE deleted=0`，`DELETE` 变为 `UPDATE SET deleted=1`。

### 5.2 审计字段
所有业务表继承 `BaseEntity`，自动填充：
- `create_time`：创建时间
- `update_time`：更新时间（数据库 `ON UPDATE CURRENT_TIMESTAMP`）

### 5.3 祖级列表（ancestors）
`sys_dept.ancestors` 存储从根到父节点的 ID 路径，便于：
- 快速查询所有子孙部门（`WHERE ancestors LIKE '0,1,%'`）
- 避免递归查询

### 5.4 联合主键
关联表使用联合主键：
- `sys_user_role`: `PRIMARY KEY (user_id, role_id)`
- `sys_role_menu`: `PRIMARY KEY (role_id, menu_id)`

### 5.5 索引设计
| 表 | 索引 | 用途 |
|----|------|------|
| sys_dept | idx_parent_id | 按父部门查询子部门 |
| sys_dept | idx_ancestors | 按祖级路径查询子孙 |
| sys_user | idx_dept_id | 按部门查询用户 |
| sys_user | idx_status | 按状态筛选 |
| sys_role | idx_role_code | 按角色编码查询 |
| sys_menu | idx_parent_id | 按父菜单查询子菜单 |
| sys_menu | idx_permission | 按权限标识查询 |
| sys_user_role | idx_user_id / idx_role_id | 双向关联查询 |
| sys_role_menu | idx_role_id / idx_menu_id | 双向关联查询 |
| sys_operation_log | idx_operator / idx_module / idx_create_time | 日志查询条件 |
| sys_login_log | idx_username / idx_login_time | 日志查询条件 |