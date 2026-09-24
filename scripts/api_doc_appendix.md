---

# 附录 A：错误码

非 200 的业务码（`code` 字段），前端据此提示用户，**不要用 HTTP 状态码判断业务结果**。

| code | 含义 | 常见触发场景 |
|---|---|---|
| 200 | 成功 | — |
| 400 | 请求参数错误 | 参数缺失/格式错；敏感词 L1 命中；上传格式或大小不合规 |
| 401 | 未授权 | 未带 Token、Token 过期或已吊销 |
| 403 | 无权限 | Token 有效但缺少该接口要求的权限码 |
| 404 | 资源不存在 | 帖子已删除/未过审；文件不存在 |
| 500 | 服务器内部错误 | 未预期异常，应查日志 |
| 1001 | 用户名或密码错误 | 登录 |
| 1002 | 账号已被锁定 | — |
| 1003 | 账号已被禁用 | 管理端封号 |
| 1004 | Token 已过期 | 需重新登录 |
| 1005 | Token 无效 | 伪造/已拉黑 |
| 1006 | 原密码错误 | 修改密码 |
| 1007 | 验证码错误 | 管理端登录 |
| 1008 | 密码强度不足 | 需 8 位以上且含大小写字母、数字、特殊字符 |
| 1009 | 请求过于频繁 | 登录失败次数过多 |
| 1010 | 账号已被临时锁定 | 30 分钟后再试 |
| 2001 / 2002 | 角色名 / 角色编码已存在 | 角色管理 |
| 2003 | 角色已分配用户，无法删除 | 角色管理 |
| 3001 | 菜单名称已存在 | 菜单管理 |
| 4001 | 部门名称已存在 | 部门管理 |
| 4002 / 4003 | 部门存在子部门 / 存在用户，无法删除 | 部门管理 |
| 5001 | 数据已存在 | — |
| 5002 | 无权操作此数据 | 例如删除他人的帖子/评论（**不是 HTTP 403**） |

> ⚠️ 注意 `5002`：C 端删除他人内容返回的是 **HTTP 200 + code 5002**，
> 浏览器端必须以 `code` 为准。管理端权限不足则由框架直接返回 **HTTP 403**，两者语义不同。

---

# 附录 B：树洞管理权限码（v1.1 起）

改造前所有树洞管理端点都要求 `admin` 角色码，导致**只有超级管理员能管树洞**。
现在拆成下列细粒度权限码，并在判断里保留 `admin` 兼容。

| 模块 | 权限码 | 说明 |
|---|---|---|
| 首页 | `system:dashboard:view` | 管理端首页统计 |
| 用户管理 | `th:user:list` | 查看用户列表 |
| | `th:user:view` | 用户详情 / 行为日志 / 其发帖评论 |
| | `th:user:mute` | 禁言、解除禁言 |
| | `th:user:ban` | 封号、解封 |
| | `th:user:release` | 一键解除全部处罚 |
| | `th:user:violation` | 手动增减违规分 |
| 内容审核 | `th:moderation:list` | 看待审帖子/评论与统计 |
| | `th:moderation:audit` | 单条或批量通过/拒绝 |
| 帖子管理 | `th:post:list` / `th:post:view` | 列表 / 详情 |
| | `th:post:audit` / `th:post:pin` / `th:post:hide` / `th:post:delete` | 审核 / 置顶 / 隐藏 / 删除 |
| 评论管理 | `th:comment:list` / `th:comment:view` | 列表 / 详情 |
| | `th:comment:hide` / `th:comment:delete` | 隐藏 / 删除 |
| 举报管理 | `th:report:list` / `th:report:view` | 列表 / 详情 |
| | `th:report:handle` | 处理举报（成立会触发下架+计分+通知闭环） |
| 在线用户 | `th:online:list` / `th:online:kick` | 查看 / 强制下线 |
| 分类管理 | `th:category:list` / `th:category:add` / `th:category:edit` / `th:category:delete` | — |
| 公告管理 | `th:announcement:list` / `th:announcement:add` / `th:announcement:edit` / `th:announcement:delete` | — |
| 敏感词 | `th:sensitive:list` / `th:sensitive:add` / `th:sensitive:edit` / `th:sensitive:delete` / `th:sensitive:import` / `th:sensitive:refresh` | — |
| 数据分析 | `th:analytics:view` | 数据看板 |
| 站点配置 | `th:settings:view` / `th:settings:edit` | 读取 / 修改 |
| 操作日志 | `th:logs:view` | — |

## B.1 内置示例角色（见 `sql/migration_v1.3.0.sql`）

| 角色 | role_code | 能做什么 | 不能做什么 |
|---|---|---|---|
| 内容审核员 | `th_auditor` | 审核待审内容、处理举报、隐藏帖子和评论 | 删帖删评论、动用户、改站点配置、动词库 |
| 树洞运营 | `th_operator` | 分类、公告、数据看板、在线用户 | 一切内容处置、用户处罚、站点配置 |

演示账号 `th_auditor` / `th_operator`，**初始密码与种子管理员一致（`Admin@1234`）——上线前必须改密或删除**。

## B.2 授权链路

```
角色(role_code) ──┐
                  ├─→ authorities = 角色码 ∪ 菜单/按钮权限码 ─→ @PreAuthorize 判定
角色-菜单(sys_role_menu) ─→ 菜单 permission ──┘
```

- 给角色**分配菜单**既决定了它能看到哪些页面（`/api/menu/user-menus`），
  也决定了它拥有哪些权限码 —— 两者同源，不会出现"看得到页面但调不动接口"的错配。
- `admin` 角色码在所有判断里都放行（`stores/user.hasPermission` 与后端 `hasAnyAuthority` 双侧兼容）。

> ⚠️ `SecurityConfig` 中 `/api/admin/**` **只要求已登录**，具体权限一律交给方法上的 `@PreAuthorize`。
> 曾经这里是 `hasAuthority("admin")`，等于把整个 `/api/admin/**` 一刀切成超管专用，
> 方法级注解根本没机会生效 —— 这是"只有 admin 能管树洞"的根因之一。
> **新增管理端接口时务必补上 `@PreAuthorize`，否则默认所有登录用户都能访问。**

---

# 附录 C：头像上传

## C.1 完整流程

```
1) POST /api/file/avatar    （需登录，multipart/form-data，字段名固定为 file）
   → { "code":200, "data":"/api/file/avatar/3f1c....png" }

2) PUT /api/th/user/profile  { "avatar": "/api/file/avatar/3f1c....png" }

3) <img src="/api/file/avatar/3f1c....png">   （无需登录，浏览器直接渲染）
```

## C.2 约束

| 项 | 值 |
|---|---|
| 支持格式 | JPG / PNG / GIF / WebP（**按文件头魔数判定**，不看扩展名与 Content-Type） |
| 大小上限 | 2MB（业务校验），可通过 `FILE_AVATAR_MAX_SIZE` 调整 |
| 容器上限 | 10MB（`spring.servlet.multipart.max-file-size`），必须大于业务上限 |
| 存储位置 | `FILE_STORAGE_DIR`，默认 `./uploads`，按 `avatar/` 分目录 |
| 文件名 | **服务端生成**（UUID），客户端文件名一律丢弃 |
| 访问前缀 | `FILE_PUBLIC_PREFIX`，默认 `/api/file` |

## C.3 安全设计（每条都对应一类真实攻击）

- **拒绝 SVG**：SVG 可内嵌 `<script>`，作为图片直出即存储型 XSS。
- **魔数判类型**：把 `.txt` 改名成 `.png` 会被拒绝；伪造 Content-Type 也无效。
- **服务端生成文件名**：客户端传 `../../evil.png` 也只会在 `uploads/avatar/` 下落成随机名。
- **读取侧双重校验**：`category` 白名单 + 文件名严格正则（32 位 hex），归一化后再确认位于根目录内。
- **`avatar` 字段格式白名单**：只接受本站上传地址或 `http(s)://` 外链，
  防止 `javascript:` / `data:` 之类值进入 `<img src>`。
- **匿名不带头像**：匿名帖与匿名评论的 `authorAvatar` 恒为空 —— 头像可反向识别身份。

> 📌 **读取接口是公开的**：`<img>` 标签不会携带 `Authorization` 头，若要求鉴权头像永远渲染不出来。
> 安全性由"不可枚举的随机文件名"保证。**不要把敏感文件放进这个目录。**

## C.4 前端约定

- 上传时**不要手动设置 `Content-Type`**，交给浏览器带 boundary，否则后端解析失败。
- 前端已做一次类型/大小预校验（省掉必然失败的往返），但**唯一可信的校验在后端**。
- 头像缓存：登录接口只返回 `token/nickname`，头像需额外调 `GET /api/th/auth/user-info` 获取。

---

# 附录 D：数据权限（行级隔离）

角色上的 `data_scope` 决定登录用户能看到哪些部门的数据，由 MyBatis-Plus
`DataPermissionInterceptor` 自动为 `sys_user` / `sys_dept` 的查询追加条件。

| 值 | 含义 | 边界依据 |
|---|---|---|
| 1 | 全部数据 | 不限制 |
| 2 | 本集团及以下 | 组织树根节点 |
| 3 | 本公司及以下 | `ancestors` 中第 2 层节点 |
| 4 | 本部门及以下 | 当前部门及其全部子部门 |
| 5 | 本部门及以下（限 N 级） | 向下只看 N 层，N 存于 `sys_role.data_scope_level` |
| 6 | 本部门 | 不含子部门 |
| 7 | 自定义部门 | `sys_role_dept` 中勾选的部门（不含其子部门） |
| 8 | 仅本人 | `sys_user` 恒保留"本人可见" |

- 可见部门集合在**登录时预计算**并写入 `LoginUser`，避免每条 SQL 递归组织树。
- `sys_dept` 查询用的是「可见部门 + 其全部祖先」，否则父节点被过滤会导致组织树在前端整棵消失；
  `sys_user` 用的是**精确的可见部门集合**，不含祖先，避免放大可见范围。
- 解析失败时降级为「仅本人」（最保守），不会放大权限。

> ⚠️ 已知限制：拦截器**只改写 SELECT**。`UPDATE` / `DELETE` 不在此机制覆盖范围内，
> 目前依赖接口层权限码 + 业务校验兜底。这是本版遗留项之一。

---

# 附录 E：本版接口变更（树洞 v1.1）

## 新增

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/file/avatar` | 上传头像（登录） |
| GET | `/api/file/{category}/{filename}` | 读取文件（公开，`category` 目前仅 `avatar`） |
| DELETE | `/api/th/comment/{id}/like` | **取消点赞评论**（此前只有点赞、无法取消，是接口缺口） |

## 变更

| 接口 | 变更 | 原因 |
|---|---|---|
| `GET /api/th/comment/page` | 新增可选登录态；响应新增 `liked`、`authorAvatar` | 前端只能靠本地猜点赞态，刷新即丢 |
| `GET /api/th/post/{id}` | 改为走 `getPostDetail`，正确填充 `authorName` / `authorAvatar` / `categoryName` | 原实现直接用 `getById`，**实名帖的 authorName 恒为 null，详情页一直显示"未知用户"** |
| `GET /api/th/post/page` 等列表 | 响应新增 `authorAvatar` | 头像展示 |
| 全部树洞管理接口 | `@PreAuthorize` 由 `admin` 拆为细粒度权限码（保留 `admin`） | 支持按角色授权 |
| `SecurityConfig` | `/api/admin/**` 由 `hasAuthority('admin')` 改为 `authenticated()` | 原来是一道全局硬门，非 admin 角色必然 403 |
| `SecurityConfig` | `GET /api/file/**` 加入白名单；`DELETE /api/th/comment/{id}/like` 纳入需登录列表 | 头像可渲染 / 取消点赞可用 |
| `GET /api/th/auth/user-info` | 沿用原有结构（已含 `avatar`） | — |

## 接口对账结论

用 `scripts/api_audit.py` 把后端全部端点与两个前端的调用逐条比对：

- **前端调用 → 后端端点：全部命中**，没有调用不存在的接口。
- **后端端点 → 前端调用：修复前有 4 个从未被调用**，其中 2 个是真问题：
  - `GET /api/th/post/{id}/liked` —— C 端从未调用，导致刷新后"已抱抱"状态丢失，
    且点赞数会被本地乐观自增算错（服务端幂等直接返回，前端却仍加 1）。**已接入。**
  - `PUT /api/admin/th/post/{id}/audit` —— 管理端单条审核实际走的是 `batch-audit`，
    属冗余端点，保留但不再使用。
  - `GET /api/test/ping`、`GET /api/test/admin-ping` —— 连通性自测端点，保留。

---

# 附录 F：验证状态总览（2026-09-24）

> 用途：标记哪些功能已通过真实环境验证。**已标记 ✅ 的模块，后续改动未触及前不必重复测试。**
> 触及 = 改动了该模块的代码或其依赖的表结构/配置。

## ✅ 已验证通过（可直接采信）

| 模块 | 验证依据 | 结果 | 日期 |
|---|---|---|---|
| 内容治理（敏感词/禁言/举报/限额） | 后端单测 61 个 | 全绿 | 09-24 |
| 数据权限 8 级（SELECT 改写） | `DataScopeHelperTest` + 接口级 E2E 77 用例 | 全绿 | 09-24 |
| 头像上传全链路（上传/恶意文件/穿越/匿名脱敏） | `scripts/verify_avatar_perm.sh` | 65/65 | 09-24 |
| 树洞管理权限矩阵（审核员/运营正反用例） | `scripts/verify_avatar_perm.sh` | 65/65 | 09-24 |
| 帖子/评论点赞态与计数一致性 | `scripts/verify_avatar_perm.sh` + 浏览器 E2E | 通过 | 09-24 |
| C 端全站页面与交互（含头像展示、消息中心） | `scripts/verify_treehole_ui.cjs`（真实浏览器） | 76/76，控制台错误 0 | 09-24 |
| 登录页整屏分屏布局（深/浅主题、移动端） | `scripts/verify_treehole_ui.cjs` §11 + 截图 | 通过 | 09-23 |
| 前端接口调用与后端端点一致性 | `scripts/api_audit.py` 双向对账 | 136 调用全命中 | 09-24 |
| 角色创建 / 菜单分配 / 用户分配角色（管理端 UI） | `scripts/verify_admin_role_ui.cjs`（真实浏览器+真实验证码登录） | 10/10，控制台错误 0 | 09-24 |
| 防提权：创建 role_code=admin 被拒（浏览器实测） | 同上 | 通过 | 09-24 |

## ⚠️ 待验证 / 待升级

| 项 | 状态 | 说明 |
|---|---|---|
| 写操作数据范围校验（UPDATE/DELETE） | **待升级** | 拦截器只改写 SELECT；写操作由 Service 层显式校验兜底（代码已注明）。启用拦截器改写写操作需配套回归测试 |
| 管理端浏览器 E2E 重写 | 待升级 | `browser_e2e.cjs` 选择器过时（已 gitignore），是唯一覆盖管理端流程的测试资产 |
| C 端全量 E2E 复跑（含 4 项快修后） | `scripts/verify_treehole_ui.cjs` | 76/76，控制台错误 0、4xx/5xx 0 | 09-24 |
| 登录页浅色引语对比度（暗角修复） | `docs/login-check/login-light-v2.jpg` 实机截图 | 可读性达标 | 09-24 |
| C 端 `tsconfig.json` 类型检查 | 待补 | build 只剥类型不做检查 |
