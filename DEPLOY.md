# 部署说明

## 已预配置（无需修改即可运行）

| 项目 | 值 |
|---|---|
| JWT 密钥 | 已自动生成并写入 docker-compose.yml |
| MySQL 密码 | `123456abc` |
| Redis 密码 | `redis123456` |
| 后端 profile | `prod`（SQL 日志关闭、Swagger 关闭） |

## 上传到服务器

把整个项目目录（`D:\s1`）上传到服务器，保持目录结构：

```
s1/
├── docker-compose.yml
├── permission-admin/
│   ├── Dockerfile
│   ├── sql/create_tables.sql
│   └── permission-api/target/permission-api-1.0.0.jar   ← 已打好
├── permission-ui/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── dist/                                              ← 已打好
```

## 启动

```bash
cd s1
docker compose up -d
```

## 访问

| 服务 | 地址 |
|---|---|
| 前端 | http://服务器IP |
| 后端 API | http://服务器IP:8080/api |
| Swagger（prod 已关闭） | — |

## 默认账号

| 用户名 | 密码 | 角色 |
|---|---|---|
| admin | Admin@1234 | 超级管理员 |
| tech | Admin@1234 | 技术负责人 |
| backend | Admin@1234 | 普通用户 |

## ⚠️ 上线后建议修改

1. **MySQL 密码** — 编辑 `docker-compose.yml` 中所有 `123456abc`
2. **Redis 密码** — 编辑 `docker-compose.yml` 中所有 `redis123456`
3. **JWT 密钥** — 用 `openssl rand -base64 48` 生成新密钥，替换 `JWT_SECRET_KEY` 的值
4. **默认账号密码** — 登录后及时修改 admin/tech/backend 的密码

## 常用命令

```bash
docker compose up -d        # 启动
docker compose down         # 停止
docker compose logs -f      # 查看日志
docker compose logs -f backend   # 只看后端日志
docker compose restart backend   # 重启后端
```
