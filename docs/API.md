# 权限管理系统 API 接口文档

## 概述

- **基础URL**: `http://8.218.49.237:8081/api`
- **认证方式**: JWT Token (Header: `Authorization: Bearer <token>`)
- **响应格式**: JSON
- **统一响应结构**:
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": {}
  }
  ```

## 错误码

| Code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 1001 | 用户名或密码错误 |

---

## 1. 认证接口

### 1.1 管理员登录

- **URL**: `POST /auth/login`
- **公开**: 是
- **请求体**:
  ```json
  {
    "username": "admin",
    "password": "Admin@1234",
    "captchaKey": "",
    "captchaCode": ""
  }
  ```
- **响应**:
  ```json
  {
    "code": 200,
    "data": {
      "accessToken": "eyJ...",
      "refreshToken": "eyJ...",
      "expiresIn": 7200
    }
  }
  ```

### 1.2 树洞用户注册

- **URL**: `POST /th/auth/register`
- **公开**: 是
- **请求体**:
  ```json
  {
    "username": "testuser",
    "password": "Test@1234"
  }
  ```

### 1.3 树洞用户登录

- **URL**: `POST /th/auth/login`
- **公开**: 是
- **请求体**:
  ```json
  {
    "username": "testuser",
    "password": "Test@1234",
    "captchaKey": "",
    "captchaCode": ""
  }
  ```
- **响应**:
  ```json
  {
    "code": 200,
    "data": {
      "token": "eyJ...",
      "nickname": "testuser"
    }
  }
  ```

---

## 2. 仪表盘接口

### 2.1 综合仪表盘数据

- **URL**: `GET /dashboard/overview`
- **权限**: admin
- **响应**:
  ```json
  {
    "code": 200,
    "data": {
      "admin": {
        "overview": { "userCount": 150, "roleCount": 100, "menuCount": 32, "deptCount": 110 },
        "userStatus": { "active": 150, "inactive": 0 },
        "deptUserCount": [{ "name": "总公司", "count": 15 }],
        "loginTrend": { "dates": ["08-11", "08-12"], "counts": [6, 6] }
      },
      "treehole": {
        "overview": { "userCount": 562, "postCount": 175, "commentCount": 286, "reportCount": 0 },
        "postStatus": { "approved": 175, "pending": 0, "rejected": 0 },
        "userStatus": { "active": 562, "banned": 0 },
        "pendingReports": 0
      },
      "pending": { "pendingPosts": 0, "pendingReports": 0 },
      "trends": { "dates": ["08-11"], "userTrend": [10], "postTrend": [5], "commentTrend": [8] }
    }
  }
  ```

### 2.2 管理员系统统计

- **URL**: `GET /dashboard/admin-stats`
- **权限**: admin

### 2.3 树洞系统统计

- **URL**: `GET /dashboard/treehole-stats`
- **权限**: admin

### 2.4 待处理事项

- **URL**: `GET /dashboard/pending`
- **权限**: admin
- **响应**:
  ```json
  {
    "code": 200,
    "data": {
      "pendingPosts": 0,
      "pendingReports": 0
    }
  }
  ```

### 2.5 趋势数据

- **URL**: `GET /dashboard/trends?days=7`
- **权限**: admin
- **参数**: `days` (可选, 默认7)

### 2.6 实时统计

- **URL**: `GET /dashboard/realtime`
- **权限**: admin
- **响应**:
  ```json
  {
    "code": 200,
    "data": {
      "newUsersToday": 10,
      "newPostsToday": 5,
      "newCommentsToday": 8
    }
  }
  ```

### 2.7 管理员系统统计(兼容)

- **URL**: `GET /dashboard/statistics`
- **权限**: admin

---

## 3. 用户管理接口

### 3.1 管理员列表

- **URL**: `GET /user/page?pageNum=1&pageSize=10`
- **权限**: admin

### 3.2 树洞用户列表

- **URL**: `GET /admin/th/user/page?pageNum=1&pageSize=10&keyword=&status=`
- **权限**: admin
- **参数**:
  - `keyword` (可选): 搜索用户名
  - `status` (可选): 0-封禁 1-正常

### 3.3 树洞用户详情

- **URL**: `GET /admin/th/user/{id}`
- **权限**: admin

### 3.4 禁言用户

- **URL**: `PUT /admin/th/user/{id}/mute?hours=24`
- **权限**: admin

### 3.5 解除禁言

- **URL**: `PUT /admin/th/user/{id}/unmute`
- **权限**: admin

### 3.6 封号

- **URL**: `PUT /admin/th/user/{id}/ban?days=7`
- **权限**: admin

### 3.7 解封

- **URL**: `PUT /admin/th/user/{id}/unban`
- **权限**: admin

### 3.8 用户发帖记录

- **URL**: `GET /admin/th/user/{id}/posts?pageNum=1&pageSize=10`
- **权限**: admin

### 3.9 用户评论记录

- **URL**: `GET /admin/th/user/{id}/comments?pageNum=1&pageSize=10`
- **权限**: admin

### 3.10 用户操作日志

- **URL**: `GET /admin/th/user/{id}/logs?pageNum=1&pageSize=10`
- **权限**: admin

---

## 4. 帖子管理接口

### 4.1 帖子列表

- **URL**: `GET /admin/th/post/page?pageNum=1&pageSize=10&status=&categoryId=&keyword=`
- **权限**: admin
- **参数**:
  - `status` (可选): 0-待审核 1-已通过 2-已拒绝
  - `categoryId` (可选): 分类ID
  - `keyword` (可选): 搜索关键词

### 4.2 帖子详情

- **URL**: `GET /admin/th/post/{id}`
- **权限**: admin

### 4.3 审核帖子

- **URL**: `PUT /admin/th/post/{id}/audit?status=1&remark=审核通过`
- **权限**: admin
- **参数**:
  - `status`: 1-通过 2-拒绝
  - `remark` (可选): 审核备注

### 4.4 置顶/取消置顶

- **URL**: `PUT /admin/th/post/{id}/pin?isTop=1`
- **权限**: admin
- **参数**: `isTop` - 0-取消置顶 1-置顶

### 4.5 隐藏/显示帖子

- **URL**: `PUT /admin/th/post/{id}/hide?status=0`
- **权限**: admin

### 4.6 删除帖子

- **URL**: `DELETE /admin/th/post/{id}`
- **权限**: admin

### 4.7 批量审核

- **URL**: `POST /admin/th/moderation/batch-audit`
- **权限**: admin
- **请求体**:
  ```json
  {
    "type": "post",
    "ids": [1, 2, 3],
    "status": 1
  }
  ```

---

## 5. 评论管理接口

### 5.1 评论列表

- **URL**: `GET /admin/th/comment/page?pageNum=1&pageSize=10&postId=`
- **权限**: admin
- **参数**: `postId` (可选): 按帖子筛选

### 5.2 评论详情

- **URL**: `GET /admin/th/comment/{id}`
- **权限**: admin

### 5.3 隐藏/显示评论

- **URL**: `PUT /admin/th/comment/{id}/hide?status=0`
- **权限**: admin

### 5.4 删除评论

- **URL**: `DELETE /admin/th/comment/{id}`
- **权限**: admin

---

## 6. 举报管理接口

### 6.1 举报列表

- **URL**: `GET /admin/th/report/page?pageNum=1&pageSize=10&status=`
- **权限**: admin
- **参数**: `status` (可选): 0-待处理 1-已成立 2-不成立

### 6.2 举报详情

- **URL**: `GET /admin/th/report/{id}`
- **权限**: admin

### 6.3 处理举报

- **URL**: `PUT /admin/th/report/{id}/handle?status=1&result=处理备注`
- **权限**: admin
- **参数**:
  - `status`: 1-成立 2-不成立
  - `result` (可选): 处理备注

### 6.4 批量处理举报

- **URL**: `POST /admin/th/report/batch-handle`
- **权限**: admin
- **请求体**:
  ```json
  {
    "ids": [1, 2, 3],
    "status": 1,
    "result": "批量处理"
  }
  ```

### 6.5 举报统计

- **URL**: `GET /admin/th/report/stats`
- **权限**: admin
- **响应**:
  ```json
  {
    "code": 200,
    "data": {
      "total": 100,
      "pending": 10,
      "resolved": 80,
      "rejected": 10
    }
  }
  ```

---

## 7. 分类管理接口

### 7.1 分类列表

- **URL**: `GET /admin/th/category/list`
- **权限**: admin

### 7.2 创建分类

- **URL**: `POST /admin/th/category`
- **权限**: admin
- **请求体**:
  ```json
  {
    "name": "分类名称",
    "code": "category_code",
    "sort": 1
  }
  ```

### 7.3 修改分类

- **URL**: `PUT /admin/th/category/{id}`
- **权限**: admin

### 7.4 删除分类

- **URL**: `DELETE /admin/th/category/{id}`
- **权限**: admin

---

## 8. 公告管理接口

### 8.1 公告列表

- **URL**: `GET /admin/th/announcement/page?pageNum=1&pageSize=10&type=`
- **权限**: admin

### 8.2 创建公告

- **URL**: `POST /admin/th/announcement`
- **权限**: admin

### 8.3 修改公告

- **URL**: `PUT /admin/th/announcement/{id}`
- **权限**: admin

### 8.4 删除公告

- **URL**: `DELETE /admin/th/announcement/{id}`
- **权限**: admin

---

## 9. 数据分析接口

### 9.1 数据概览

- **URL**: `GET /admin/th/analytics/overview`
- **权限**: admin

### 9.2 趋势分析

- **URL**: `GET /admin/th/analytics/trends?days=7`
- **权限**: admin

### 9.3 分类热度

- **URL**: `GET /admin/th/analytics/categories`
- **权限**: admin

---

## 10. 系统配置接口

### 10.1 获取配置

- **URL**: `GET /admin/th/settings`
- **权限**: admin

### 10.2 更新配置

- **URL**: `PUT /admin/th/settings`
- **权限**: admin
- **请求体**:
  ```json
  {
    "site_name": "树洞",
    "site_description": "匿名分享平台"
  }
  ```

---

## 11. 日志管理接口

### 11.1 操作日志

- **URL**: `GET /log/operation/page?pageNum=1&pageSize=10&keyword=&module=&startDate=&endDate=`
- **权限**: admin

### 11.2 登录日志

- **URL**: `GET /log/login/page?pageNum=1&pageSize=10=&keyword=&status=&startDate=&endDate=`
- **权限**: admin

---

## 12. 树洞公共接口

### 12.1 分类列表

- **URL**: `GET /th/category/list`
- **公开**: 是

### 12.2 帖子列表

- **URL**: `GET /th/post/page?pageNum=1&pageSize=10&categoryId=&keyword=`
- **公开**: 是

### 12.3 帖子详情

- **URL**: `GET /th/post/{id}`
- **公开**: 是

### 12.4 创建帖子

- **URL**: `POST /th/post`
- **公开**: 否 (需要JWT)
- **请求体**:
  ```json
  {
    "content": "帖子内容",
    "categoryId": 1,
    "isAnonymous": 1
  }
  ```

### 12.5 点赞帖子

- **URL**: `POST /th/post/{id}/like`
- **公开**: 否 (需要JWT)

### 12.6 取消点赞帖子

- **URL**: `DELETE /th/post/{id}/like`
- **公开**: 否 (需要JWT)

### 12.7 检查是否点赞

- **URL**: `GET /th/post/{id}/liked`
- **公开**: 否 (需要JWT)

### 12.8 评论列表

- **URL**: `GET /th/comment/page?pageNum=1&pageSize=10&postId=1`
- **公开**: 是

### 12.9 创建评论

- **URL**: `POST /th/comment`
- **公开**: 否 (需要JWT)
- **请求体**:
  ```json
  {
    "postId": 1,
    "content": "评论内容",
    "isAnonymous": 1
  }
  ```

### 12.10 点赞评论

- **URL**: `POST /th/comment/{id}/like`
- **公开**: 否 (需要JWT)

### 12.11 提交举报

- **URL**: `POST /th/report`
- **公开**: 否 (需要JWT)
- **请求体**:
  ```json
  {
    "targetType": "POST",
    "targetId": 1,
    "reason": " spam",
    "description": "详细说明"
  }
  ```

### 12.12 我的帖子

- **URL**: `GET /th/user/posts?pageNum=1&pageSize=10`
- **公开**: 否 (需要JWT)

### 12.13 我的评论

- **URL**: `GET /th/user/my-comments?pageNum=1&pageSize=10`
- **公开**: 否 (需要JWT)

### 12.14 收到的评论

- **URL**: `GET /th/user/received-comments?pageNum=1&pageSize=10`
- **公开**: 否 (需要JWT)

### 12.15 通知列表

- **URL**: `GET /th/user/notifications?pageNum=1&pageSize=10&unreadOnly=false`
- **公开**: 否 (需要JWT)

### 12.16 未读通知数量

- **URL**: `GET /th/user/unread-count`
- **公开**: 否 (需要JWT)

### 12.17 标记通知已读

- **URL**: `PUT /th/user/notifications/read`
- **公开**: 否 (需要JWT)
- **请求体**:
  ```json
  {
    "ids": [1, 2, 3]
  }
  ```

### 12.18 更新个人资料

- **URL**: `PUT /th/user/profile`
- **公开**: 否 (需要JWT)
- **请求体**:
  ```json
  {
    "nickname": "新昵称",
    "bio": "个人简介",
    "gender": 1
  }
  ```

---

## 13. 审核中心接口

### 13.1 待审核帖子

- **URL**: `GET /admin/th/moderation/posts?pageNum=1&pageSize=10`
- **权限**: admin

### 13.2 待审核评论

- **URL**: `GET /admin/th/moderation/comments?pageNum=1&pageSize=10`
- **权限**: admin

### 13.3 审核统计

- **URL**: `GET /admin/th/moderation/stats`
- **权限**: admin
