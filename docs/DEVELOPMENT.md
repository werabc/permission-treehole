# 权限管理系统 - 开发文档

## 项目概述

权限管理系统是一个基于 Spring Boot 3 + Vue 3 的企业级后台管理系统，包含：
- **后端**: Spring Boot 3.2.5 + MyBatis-Plus + Spring Security 6 + JWT + Redis + MySQL 8.0
- **管理后台前端**: Vue 3 + Element Plus + Pinia + Vue Router + Vite
- **树洞用户端前端**: Vue 3 + Element Plus + Vite
- **部署**: Docker Compose + Nginx

---

## 项目结构

```
permission-admin/                 # 后端项目
├── permission-common/            # 公共模块 (实体、DTO、工具类)
│   └── src/main/java/com/permission/common/
│       ├── entity/               # 数据库实体
│       ├── dto/                  # 数据传输对象
│       ├── exception/            # 异常处理
│       └── ...
├── permission-framework/         # 框架模块 (安全配置、JWT、Redis)
│   └── src/main/java/com/permission/framework/
│       ├── config/               # 配置类
│       ├── security/             # 安全相关
│       └── filter/               # 过滤器
├── permission-system/            # 系统模块 (服务、Mapper)
│   └── src/main/java/com/permission/system/
│       ├── service/              # 服务接口
│       ├── service/impl/         # 服务实现
│       └── mapper/               # MyBatis Mapper
├── permission-api/               # API模块 (控制器、启动类)
│   └── src/main/java/com/permission/
│       ├── controller/           # 控制器
│       └── PermissionApplication.java
├── sql/                          # SQL脚本
├── Dockerfile                    # Docker构建文件
└── pom.xml

permission-ui/                    # 管理后台前端
├── src/
│   ├── api/                      # API接口
│   ├── views/                    # 页面视图
│   │   ├── dashboard/            # 仪表盘
│   │   ├── system/               # 系统管理
│   │   ├── th-admin/             # 树洞管理
│   │   ├── system/logs/          # 日志管理
│   │   └── ...
│   ├── stores/                   # Pinia状态管理
│   ├── router/                   # 路由配置
│   └── types/                    # TypeScript类型
├── nginx.conf                    # Nginx配置
├── Dockerfile
└── package.json

treehole-web/                     # 树洞用户端前端
├── src/
│   ├── api/                      # API接口
│   ├── views/                    # 页面视图
│   │   ├── Home/                 # 首页
│   │   ├── PostDetail/           # 帖子详情
│   │   ├── Publish/              # 发布帖子
│   │   ├── Profile/              # 个人中心
│   │   └── ...
│   └── ...
├── nginx.conf
├── Dockerfile
└── package.json
```

---

## 数据库设计

### 核心表结构

#### 系统管理表 (sys_*)
| 表名 | 说明 |
|------|------|
| sys_user | 管理员用户表 |
| sys_role | 角色表 |
| sys_menu | 菜单表 |
| sys_dept | 部门表 |
| sys_user_role | 用户角色关联表 |
| sys_role_menu | 角色菜单关联表 |
| sys_login_log | 登录日志表 |
| sys_operation_log | 操作日志表 |

#### 树洞业务表 (th_*)
| 表名 | 说明 |
|------|------|
| th_user | 树洞用户表 |
| th_post | 帖子表 |
| th_comment | 评论表 |
| th_category | 分类表 |
| th_like | 点赞表 |
| th_report | 举报表 |
| th_notification | 通知表 |
| th_setting | 站点配置表 |
| th_announcement | 公告表 |
| th_user_log | 用户操作日志表 |

---

## 安全设计

### 认证流程

1. **管理员登录**: `POST /auth/login` → 返回 accessToken + refreshToken
2. **树洞用户登录**: `POST /th/auth/login` → 返回 token
3. **请求认证**: Header `Authorization: Bearer <token>`

### 权限控制

- **公开接口**: 无需认证 (如登录、注册、帖子列表)
- **认证接口**: 需要JWT Token (如创建帖子、评论、举报)
- **管理员接口**: 需要admin权限 (如用户管理、内容审核)

### SecurityConfig 配置

```java
// 公开接口
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers("/api/th/auth/**").permitAll()
.requestMatchers("/api/th/category/list", "/api/th/post/page", ...).permitAll()

// 认证接口
.requestMatchers("/api/th/post", "/api/th/comment").authenticated()
.requestMatchers("/api/th/report/**").authenticated()

// 管理员接口
.requestMatchers("/api/admin/**").hasAuthority("admin")
.requestMatchers("/api/dashboard/**").authenticated()
```

---

## 新增功能说明

### 1. 树洞公共接口 (ThPublicController)

新增 `ThPublicController.java`，提供树洞用户端所需的公共API：

- 分类列表、帖子列表/详情、创建帖子、点赞/取消点赞
- 评论列表/创建、点赞评论
- 提交举报
- 个人中心 (我的帖子/评论、收到的评论、通知)
- 个人资料管理

### 2. 举报功能完善

- **新增**: `ThReportService.createReport()` - 创建举报
- **新增**: `ThReportAdminController` 处理举报API
- **修复**: 前端举报后页面无响应问题 (添加错误处理)

### 3. 用户服务扩展

- **新增**: `ThUserService` 方法:
  - `getPosts()` - 获取用户帖子
  - `getMyComments()` - 获取我的评论
  - `getReceivedComments()` - 获取收到的评论
  - `getNotifications()` - 获取通知
  - `getUnreadCount()` - 获取未读数量
  - `markNotificationsRead()` - 标记已读
  - `updateProfile()` - 更新个人资料

### 4. 前端视图增强

- **新增**: 操作日志页面 (`system/logs/operation.vue`)
- **新增**: 登录日志页面 (`system/logs/login.vue`)
- **增强**: 仪表盘、用户管理、帖子管理、评论管理、举报管理等9个视图

---

## 部署指南

### Docker Compose 部署

```bash
# 1. 克隆项目
git clone <repository-url>
cd permission-admin

# 2. 启动所有服务
docker-compose up -d

# 3. 查看状态
docker-compose ps

# 4. 查看日志
docker-compose logs -f backend
```

### 端口分配

| 服务 | 端口 | 说明 |
|------|------|------|
| treehole-web | 80 | 树洞用户端 |
| backend | 8081 | 后端API |
| frontend | 8082 | 管理后台 |
| mysql | 3306 | 数据库 |
| redis | 6379 | 缓存 |

### 默认账号

- **管理员**: `admin` / `Admin@1234`
- **树洞用户**: 自行注册

---

## 开发指南

### 后端开发

#### 添加新接口

1. 在 `permission-system/service/` 中添加服务接口
2. 在 `permission-system/service/impl/` 中添加服务实现
3. 在 `permission-api/controller/` 中添加控制器
4. 在 `SecurityConfig` 中添加权限配置

#### 添加新实体

1. 在 `permission-common/entity/` 中添加实体类
2. 在 `permission-system/mapper/` 中添加Mapper接口
3. 在 `sql/create_tables.sql` 中添加建表语句

### 前端开发

#### 添加新页面

1. 在 `src/views/` 中添加视图文件
2. 在 `src/api/` 中添加API接口
3. 在 `src/stores/permission.ts` 中添加路由映射
4. 在数据库中添加菜单权限

#### 构建部署

```bash
# 管理后台
cd permission-ui
npm install
npm run build
docker-compose build frontend

# 树洞用户端
cd treehole-web
npm install
npm run build
docker-compose build treehole-web
```

---

## 常见问题

### 1. 举报后页面无响应

**原因**: 前端缺少错误处理，API调用失败后页面卡住
**修复**: 在 `confirmHandle()` 和 `handleBatch()` 中添加 try-catch 错误处理

### 2. Dashboard API 返回 500

**原因**: 新代码未被编译进JAR
**修复**: 确保上传正确的源文件并重新构建

### 3. 前端页面不更新

**原因**: Nginx缓存或浏览器缓存
**修复**: 强制刷新 (Ctrl+F5) 或清除浏览器缓存

---

## 版本历史

### v1.1.0 (2026-08-17)
- 新增树洞公共接口 (ThPublicController)
- 完善举报功能 (创建举报、错误处理)
- 扩展用户服务 (个人中心、通知)
- 新增日志管理页面 (操作日志、登录日志)
- 增强前端视图 (11个视图)
- 修复举报后页面无响应bug

### v1.0.0 (2026-08-16)
- 初始版本
- 基础权限管理 (用户、角色、菜单、部门)
- 树洞基础功能 (帖子、评论、分类)
- 管理后台基础功能
