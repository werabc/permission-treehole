# 企业级权限管理系统 - 部署指南

## 系统架构

```
用户浏览器
    │
    ▼
┌─────────┐
│  Nginx  │ (80端口)
└────┬────┘
     │
     ├── /admin/* ──→ permission-ui (管理后台静态文件)
     ├── /* ────────→ treehole-web (树洞前端静态文件)
     └── /api/* ────→ backend:8080 (Spring Boot)
                          │
                          ├── MySQL (权限/业务数据)
                          └── Redis (缓存/在线状态)
```

## 快速启动

### 前置要求
- Docker 20.10+
- Docker Compose 2.0+

### 启动步骤

```bash
# 1. 进入部署目录
cd deploy-package

# 2. 启动（首次会自动构建镜像）
chmod +x start.sh
./start.sh

# 3. 查看状态
docker-compose ps

# 4. 查看日志
docker-compose logs -f
```

### 访问地址

| 服务 | 地址 | 默认账号 |
|------|------|----------|
| 管理后台 | http://your-server-ip/admin/ | admin / Admin@1234 |
| 树洞前端 | http://your-server-ip/treehole/ | 自主注册 |
| API文档 | http://your-server-ip/api/swagger-ui.html | - |

> 访问 `http://your-server-ip/` 会自动跳转到树洞前端

## 生产环境配置

### 1. 修改 JWT Secret（必须！）

```bash
# 生成新的 secret（Base64 编码的 32+ 字节随机字符串）
openssl rand -base64 48

# 设置环境变量
export JWT_SECRET="你的新secret"
```

### 2. 修改默认密码

首次登录后立即修改 admin 默认密码。

### 3. 配置 HTTPS

编辑 `nginx.conf`，添加 SSL 配置：

```nginx
server {
    listen 443 ssl;
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    # ... 其他配置
}
```

### 4. 数据库备份

```bash
# 备份
docker exec permission_mysql mysqldump -uroot -proot123456 permission_admin > backup.sql

# 恢复
docker exec -i permission_mysql mysql -uroot -proot123456 permission_admin < backup.sql
```

## 目录结构

```
deploy-package/
├── docker-compose.yml    # Docker Compose 配置
├── nginx.conf            # Nginx 配置
├── start.sh              # 启动脚本
├── stop.sh               # 停止脚本
├── backend/              # Spring Boot JAR
│   └── permission-api-1.0.0.jar
├── permission-ui/        # 管理后台前端（构建产物）
├── treehole-web/         # 树洞前端（构建产物）
└── sql/                  # 数据库初始化脚本
    └── init.sql
```

## 常见问题

### 端口被占用
修改 `docker-compose.yml` 中的端口映射。

### 数据库连接失败
检查 MySQL 容器是否正常启动：
```bash
docker-compose logs mysql
```

### 前端页面 404
检查 nginx 配置中的 `alias` 路径是否正确。

## 技术栈

- **后端**: Spring Boot 3.2 + Spring Security 6 + MyBatis-Plus
- **前端**: Vue 3 + Element Plus + Vite
- **数据库**: MySQL 8.0
- **缓存**: Redis 7
- **反向代理**: Nginx
- **容器化**: Docker + Docker Compose
