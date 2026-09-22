# 升级实施清单 · 第一阶段（A 内容治理闭环 + B C 端功能补全）

> 基线 `b3a51af` | 目标：让产品从"能发帖的 Demo"变成"可运营的社区"
> 原则：**能补逻辑就不改表**；新增表一律遵循现有约定（utf8mb4_unicode_ci / InnoDB / `deleted` 逻辑删除 / `status` 状态）

---

## 🅰 内容治理闭环（后端为主）

### A1 治理基础设施（新增 3 个组件 + 1 张表）

| 组件 | 路径 | 说明 |
|------|------|------|
| `th_sensitive_word` 表 | `permission-admin/sql/migration_v1.1.0.sql` | 字段：`id/word/level(1拦截 2警告)/category/status/deleted/create_time`；`uk_word` 唯一键 |
| `SensitiveWordService` | `permission-system/.../service/impl/` | Trie(DFA) 构建 + 缓存（`@PostConstruct` 加载 + 管理端增删后刷新）；提供 `check(text)` 返回命中词与等级 |
| `ThSettingReader` | `permission-system/.../support/` | 统一读 `th_setting`（带本地缓存 + 失效刷新），消灭各 Service 里硬编码配置 |
| `RateLimiter` | `permission-system/.../support/` | 基于 Redis 的日计数：`th:rate:post:{userId}:{yyyyMMdd}`，读 `max_post_per_day` / `max_comment_per_day` |
| `ThUserGuard` | `permission-system/.../support/` | 统一校验：`ban_until`（登录拦截）、`mute_until`（发帖/评论/点赞拦截）、`violation_count` 计分与自动处罚 |

### A2 接入主链路（改 4 个 Service）

- `ThAuthController.login` → `ThUserGuard.checkBanned()`（封号直接拒绝，返回剩余天数）
- `ThPostServiceImpl.createPost` → 敏感词检查 → 频率限额 → 禁言检查 → 按 `post_need_audit` 决定 `status`（1 通过 / 0 待审）
- `ThCommentServiceImpl.createComment` → 同上（用 `comment_need_audit` / `max_comment_per_day`）
- 点赞/举报 → 禁言检查（禁言用户不得互动）

> 全部抛 `BusinessException` + 明确文案，前端已有全局错误提示，无需改 UI 框架

### A3 举报闭环（改 `ThReportServiceImpl.handleReport`）

处理举报时按结果分支：
- 举报成立 → 通知举报人（`th_notification` type=`REPORT_RESULT`）+ 被举报内容下线/驳回 + 作者 `violation_count += 权重` + 写 `th_user_log`
- 达阈值自动处罚：`violation_count ≥ 5` → `mute_until = now + 3d`；`≥ 10` → `ban_until = now + 7d`（阈值放 `th_setting` 可配）
- 举报驳回 → 同样通知举报人处理结果

### A4 管理端（补 1 个页面 + 联动）

- 新增敏感词管理页：`permission-ui/src/views/th-admin/sensitive/index.vue`（列表/新增/批量导入/启用停用）
- 菜单初始化 SQL 追加到 `migration_v1.1.0.sql`（`sys_menu` + `sys_role_menu` 授权给 admin）
- `th-admin/user` 页面补"禁言/封号/解除"操作按钮（后端处罚接口补齐）

---

## 🅱 C 端功能补全

### B1 消息通知中心（后端已就绪，纯前端 · **最先做，最高性价比**）

- 新增 `treehole-web/src/views/Notifications.vue`：通知列表（点赞/评论/系统/举报结果分 tab）+ 点击跳转帖子 + 全部已读
- 路由 `/notifications`（`meta.auth`）+ 顶部导航铃铛入口（带未读数红点）
- 复用已封装的 `getNotifications()` / `getUnreadCount()` / `markNotificationsRead()`

### B2 搜索

- 后端：`GET /api/th/search?keyword=&type=post|user&pageNum=&pageSize=` → `ThSearchService`（帖子标题/内容 LIKE + 作者昵称，返回高亮片段，分页）
- 前端：首页顶部搜索框 + `SearchResults.vue`（帖子/用户分 tab）

### B3 我的收藏（新增 1 张表）

- `th_collect` 表：`id/user_id/target_type/target_id/create_time/deleted`，`uk_user_target`
- 接口：`POST/DELETE /api/th/collect`、`GET /api/th/user/collects`
- 前端：卡片收藏图标 + Profile 新增"我的收藏"页签

### B4 作者自管理

- `DELETE /api/th/post/{id}`、`DELETE /api/th/comment/{id}`（仅本人或 admin；帖子删除级联逻辑删评论/点赞/收藏/通知计数修正，加 `@Transactional`）
- 前端：我的帖子/评论列表加删除按钮 + 二次确认

### B5 他人主页

- 新增 `/user/:id` 路由，复用 `Profile.vue`（`readonly` 模式，仅展示帖子 + 资料，不显示通知/设置入口）
- 帖子卡片作者名可点击跳转（匿名帖不跳转）

### B6 用户设置页

- 后端：`PUT /api/th/auth/password`（旧密码校验 + BCrypt）、`PUT /api/th/user/profile` 扩展 `avatar/bio/gender/email`（`avatar` 暂支持外链，上传能力留给 🅲 方向）
- 前端：`Settings.vue`（资料 / 账号安全 两个 tab）

### B7 公共组件抽取（解决 `components/` 空目录）

- `PostCard.vue`（列表卡片，Home/Category/Search/Profile 复用）
- `EmptyState.vue`、`LoadingSkeleton.vue`、`Pagination.vue`

---

## 执行顺序（8 个批次）

```
1. A1 治理基础设施（新表 + 4 个组件）
2. A2 接入登录/发帖/评论/点赞主链路
3. A3 举报闭环 + 违规计分 + 通知举报人
4. A4 管理端敏感词页 + 用户处罚按钮 + 菜单 SQL
5. B2+B3 后端新接口（搜索 / 收藏 / 删帖删评论 / 改密码）
6. B1 通知中心页（前端，低垂果实）
7. B4/B5/B6 前端：删帖入口 / 他人主页 / 设置页
8. B7 抽公共组件 + 全链路自测 + 更新文档
```

## 验收与回归

- 每个批次结束跑：后端 `mvn -q test`（新增治理逻辑必须有单测）、前端 `npm run build` 通过
- 治理逻辑单元测试至少覆盖：敏感词命中/未命中、限额第 N+1 次拦截、禁言用户发帖被拒、举报成立后通知与计分、阈值触发自动禁言
- 每批次单独提交，原子化可回滚（避免再次出现"改坏没人发现"）

## 风险与对策

| 风险 | 对策 |
|------|------|
| 新增表用 `CREATE TABLE IF NOT EXISTS` + 独立 migration 文件 | 不动 `create_tables.sql`/`init.sql`，已有库可平滑升级 |
| 敏感词误拦截正常内容 | 分等级：L1 直接拦、L2 转人工待审；词库可后台秒改 |
| 频率限额误伤活跃用户 | 阈值全部走 `th_setting` 可配，默认给足（10 帖/50 评） |
| 通知/计分失败影响主流程 | 与点赞通知同策略：try-catch 隔离，主流程优先 |
| 前端改动破坏现有 7 个页面 | 批次内先抽组件再改页面，`npm run build` 作为守门人 |
