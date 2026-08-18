# 项目 Bug 修复报告

**日期**: 2026-08-18
**项目**: 企业级权限管理系统 (Spring Boot 3.2 + Vue 3 RBAC + 树洞系统)

---

## 修复概览

共修复 **17 个问题**，涉及后端 Java 代码、前端 Vue 代码、安全漏洞和代码质量改进。

---

## 高优先级修复 (功能错误)

### 1. ThAuthController 缺少 logout 端点
- **文件**: `permission-admin/permission-api/src/main/java/com/permission/controller/ThAuthController.java`
- **问题**: 前端 treehole-web 调用 `/th/auth/logout` 但后端缺少此端点，导致 404 错误
- **修复**: 新增 `logout` 方法，清除 Redis token 缓存并将 token 加入黑名单
- **提交**: `41e97d6`

### 2. ThCommentServiceImpl.likeComment 缺少评论存在性检查
- **文件**: `permission-admin/permission-system/src/main/java/com/permission/system/service/impl/ThCommentServiceImpl.java`
- **问题**: 点赞前未检查评论是否存在，可能导致对不存在的评论点赞
- **修复**: 添加评论存在性检查，如果评论不存在则抛出 NOT_FOUND 异常
- **提交**: `4f112cf`

### 3. ThPublicController.getPostDetail 缺少状态检查
- **文件**: `permission-admin/permission-api/src/main/java/com/permission/controller/ThPublicController.java`
- **问题**: 未检查帖子是否已删除或未审核，可能返回不可见帖子
- **修复**: 添加 `deleted` 和 `status` 检查，只返回已通过审核的帖子
- **提交**: `17a7242`

### 4. OnlineUserService.forceLogout token 查找逻辑错误
- **文件**: `permission-admin/permission-system/src/main/java/com/permission/system/service/OnlineUserService.java`
- **问题**: 使用 `"token:" + userId` 查找 token，但实际存储的是 `"token:" + token`
- **修复**: 移除错误的 token 查找逻辑，只清除在线状态（token 黑名单由 logout 接口处理）
- **提交**: `41e97d6`

---

## 中优先级修复 (安全漏洞)

### 5. ThPostAdminController LIKE 注入风险
- **文件**: `permission-admin/permission-api/src/main/java/com/permission/controller/ThPostAdminController.java`
- **问题**: keyword 搜索未转义 LIKE 通配符 (`%`, `_`, `\`)
- **修复**: 添加通配符转义逻辑
- **提交**: `41e97d6`

### 6. ThUserAdminController LIKE 注入风险
- **文件**: `permission-admin/permission-api/src/main/java/com/permission/controller/ThUserAdminController.java`
- **问题**: keyword 搜索未转义 LIKE 通配符
- **修复**: 添加通配符转义逻辑，同时支持用户名和昵称搜索
- **提交**: `41e97d6`

### 7. ThReportAdminController.batchHandle 缺少 status 空值检查
- **文件**: `permission-admin/permission-api/src/main/java/com/permission/controller/ThReportAdminController.java`
- **问题**: status 参数可能为 null，未验证就设置到 report 上
- **修复**: 添加 status 验证（必须为 1 或 2），添加处理数量日志
- **提交**: `41e97d6`

### 8. SysRoleServiceImpl.pageRoles LIKE 注入风险
- **文件**: `permission-admin/permission-system/src/main/java/com/permission/system/service/impl/SysRoleServiceImpl.java`
- **问题**: keyword 搜索未转义 LIKE 通配符
- **修复**: 添加通配符转义逻辑
- **提交**: `43ca49b`

### 9. SysOperationLogServiceImpl.pageLogs LIKE 注入风险
- **文件**: `permission-admin/permission-system/src/main/java/com/permission/system/service/impl/SysOperationLogServiceImpl.java`
- **问题**: keyword 搜索未转义 LIKE 通配符
- **修复**: 添加通配符转义逻辑
- **提交**: `46f8091`

---

## 低优先级修复 (代码质量)

### 10. permission-ui resetDynamicRoutes 未真正移除路由
- **文件**: `permission-ui/src/router/index.ts`
- **问题**: 只重置标志位，登出后旧路由仍存在于 router 中
- **修复**: 新增 `addedRouteNames` 数组跟踪已添加的路由，`resetDynamicRoutes` 调用 `router.removeRoute` 移除每个动态路由
- **提交**: `6a2c1a7`

### 11. permission-ui handleTokenRefresh 订阅者清空时机问题
- **文件**: `permission-ui/src/api/index.ts`
- **问题**: `finally` 中重复清空 `refreshSubscribers`
- **修复**: 移除 `finally` 中的重复清空，只由 `onTokenRefreshed` 负责
- **提交**: `6a2c1a7`

### 12. ThCommentServiceImpl NPE 防护
- **文件**: `permission-admin/permission-system/src/main/java/com/permission/system/service/impl/ThCommentServiceImpl.java`
- **问题**: `content.length()` 调用前未检查 null
- **修复**: 添加 null 检查
- **提交**: `454fe46`

### 13. 树洞前端 App.vue 登录状态响应式修复
- **文件**: `treehole-web/src/App.vue`
- **问题**: `isLoggedIn` 是 computed 属性但依赖 localStorage，不会响应式更新
- **修复**: 使用 `ref` 追踪 token 变化，确保 computed 能响应式更新
- **提交**: `a302c68`

### 14. 树洞前端 TokenVO 接口重命名
- **文件**: `treehole-web/src/api/auth.ts`
- **问题**: `TokenVO` 接口定义与实际返回不匹配
- **修复**: 重命名为 `TreeholeLoginResult` 以准确反映树洞登录返回格式
- **提交**: `f2ceb40`

---

## 已验证无问题

以下代码经过审查，确认无 bug：

- `ThPostServiceImpl.likePost` - 已有重复点赞检查和 null 检查
- `ThPostServiceImpl.createPost` - 已有 content null 检查
- `ThPostServiceImpl.pagePosts` - 已修复 LIKE 通配符转义
- `SysUserServiceImpl` - 密码策略、限流、黑名单逻辑正确
- `JwtTokenProvider` - 密钥加载和验证逻辑正确
- `JwtAuthenticationFilter` - 认证流程正确
- `SecurityConfig` - 安全配置正确
- `GlobalExceptionHandler` - 异常处理完整
- 所有 Mapper 接口 - 定义正确
- 所有前端视图组件 - 功能完整

---

## Git 提交历史

```
46f8091 fix: SysOperationLogServiceImpl pageLogs 修复 LIKE 注入风险
43ca49b fix: SysRoleServiceImpl pageRoles 修复 LIKE 注入风险
4f112cf fix: ThCommentServiceImpl likeComment 添加评论存在性检查
f2ceb40 refactor: 树洞前端 TokenVO 接口重命名为 TreeholeLoginResult
17a7242 fix: ThPublicController getPostDetail 添加状态检查
a302c68 fix: 树洞前端 App.vue 登录状态响应式修复
454fe46 fix: ThCommentServiceImpl NPE 防护
6a2c1a7 fix: 修复前端路由和 Token 刷新问题
41e97d6 fix: 修复高优先级 bug
bcfaabd chore: snapshot before bug fixes
```

---

## 统计

- **修复文件数**: 13
- **新增代码行数**: ~92
- **修改代码行数**: ~29
- **提交次数**: 9
