# 项目长期约定（treehole / permission 平台）

## 环境
- JDK：`C:\Users\Lenovo\.jdks\ms-17.0.17`
- MySQL：`/d/MYsql1/bin/mysql.exe`，127.0.0.1:3306，root / 123456abc
- Redis：`/d/redis/redis-cli`，127.0.0.1:6379，密码 redis123456
- 项目路径含中文，**bash 里跑 mvnw 会 ClassNotFoundException** → 用 PowerShell 直接 `java -classpath .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain`
- 浏览器自动化：`playwright-core` 在 `D:/software/nodejs/node_global/node_modules/@playwright/mcp/node_modules`，Chromium 在 `~/AppData/Local/ms-playwright/chromium-1243/chrome-win64/chrome.exe`

## 用户偏好
- **喜欢先看方案再动工**：前端/大改类任务，先产出可预览的设计稿或原型，确认后再改正式代码，不要直接大改后返工
- 明确要求「用尽量少的工作量」达成目标 → 优先单文件、零依赖、可一键预览的方案
- 讨厌丑和简陋；要「科技感 / 动画 / 高级背景」。不接受 emoji 当图标
- 拒绝假测试：只看状态码不算验证，要求真实浏览器 E2E + 业务断言
- 要求诚实汇报遗留项，不要粉饰

## 前端约定
- 两个前端：`treehole-web`（C 端，:3000）、`permission-ui`（管理端，:5173）
- 设计语言（2026-09-23 起）：**Deep Hollow / 深空树洞**，见 `treehole-web/design-preview/index.html`
  - 深空底 `#06060D`、玻璃拟态面板、单一强调色极光青 `#5FE0BD`、紫 `#A78BFA` 仅用于背景光晕
  - 字体：Noto Sans SC（正文）/ Fraunces 斜体（诗意短句）/ JetBrains Mono（数据与标签）
  - 图标统一线性 SVG，禁止 emoji
- 中文 POST：Git Bash 下 `curl -d '中文'` 会破坏 UTF-8 → 一律 `printf | curl --data-binary @-`

## 后端启动（重要坑）
- **`SERVER__PORT` 是 WorkBuddy 注入的环境变量，Spring Boot 会把它映射成 `server.port` 并覆盖 yml！**
  启动必须显式指定端口：`--server.port=8081`（e2e profile），否则报
  `Port 62748 already in use`（62748 正是 WorkBuddy 自己的端口，不是你的服务占用）
- 打包前必须先杀掉占用 jar 的 java 进程，否则 `repackage` 报 `Unable to rename xxx.jar.original`
- 运行 jar：`"C:/Users/Lenovo/.jdks/ms-17.0.17/bin/java.exe" -jar permission-api/target/permission-api-1.0.0.jar --spring.profiles.active=e2e --server.port=8081`
- 跑浏览器脚本需 `NODE_PATH="D:/software/nodejs/node_global/node_modules/@playwright/mcp/node_modules"`
- 沙箱禁止 node spawn 子进程（EBUSY）→ 脚本取验证码用 `net` 内联 Redis RESP 客户端，别用 redis-cli

## Git 远端与推送（2026-09-25）
- 远端：`https://github.com/werabc/permission-admin.git`，唯一长期分支 `main`
- **不需要配 `http.proxy`**：本机代理是 MyClash（Clash Plus）**TUN 模式**，
  无任何本地 HTTP 代理端口在监听（7877/7890/10809 全 closed），git 流量已被透明接管。
  判断通路：`curl -m 8 -o /dev/null -w "%{http_code}" https://github.com` 返回 200 即可直接 push。
- 认证走 `credential.helper = !'E:\Git\gh.exe' auth git-credential`，非交互
- ⚠️ **远端 main 的历史被替换过**（2026-09-25 发现）：远端曾是 `27cd1ec` ——
  与本地根提交 `01227da` 消息相同但**无共同祖先**的孤立快照，普通 push 会被 `fetch first` 拒绝。
  该快照内容已被本地历史完全覆盖（缺失文件=0），Novel 模块本地在 `adb7557` 主动删过。
  已存档为远端分支 `remote-main-backup` 并强推（`--force-with-lease`）本地 main。
  **再次遇到 `fetch first` 拒绝时，先按上述步骤核查是否仍是无损强推，不要盲目 merge。**

## 数据权限架构（2026-09-24 定型）
- 读侧：`DataPermissionInterceptor` + `DataPermissionHandler`（只改写 SELECT），豁免清单按 mappedStatementId 精确匹配
- 写侧：`DataScopeGuard`（Service 层守门员），判定委托 `DataScopeHelper.canWriteUser/canWriteDept` —— **读写同源**
- 守门员要用 `selectRawById`（必须进豁免清单）区分 404/403；业务查询严禁走豁免路径
- 超管 id=1 除本人外不得改/删/禁用/重置（`BUILTIN_ADMIN_ID`）

## 测试资产与「已验证」清单（2026-09-24）
- **已全绿的验证套件**（代码未触及则不重跑，见 `docs/接口文档.md` 附录 F）：
  - `scripts/verify_treehole_ui.cjs` —— C 端功能 E2E（真实浏览器）**76/76**
  - `scripts/audit_dom_user_view.cjs` —— C 端用户视角 DOM 巡检 **73/73**
  - `scripts/audit_dom_admin.cjs` —— 管理端逐路由 DOM 巡检 **120/120**
  - `scripts/verify_admin_role_ui.cjs` —— 管理端角色创建/分配/防提权 **10/10**
  - `scripts/verify_avatar_perm.sh` —— 头像上传全链路 + 树洞权限矩阵 **65/65**
  - `scripts/verify_write_scope.cjs` —— 越权写防护攻击验证 **26/26**
  - `scripts/verify_guard_no_overreach.cjs` —— 守门员不误伤验证 **16/16**
  - 后端单测 8 个测试类（含 DataScopeWriteGuardTest 42 组合矩阵、DataPermissionBypassTest）→ **84/84**
- **两个前端的 hash 路由**：C 端 `localhost:3000/#/xxx`、管理端 `localhost:5173/#/xxx`。
  用 path 模式 URL 会静默落到首页/dashboard —— 写 E2E 必踩。
- **C 端 dev server 只监听 IPv6** `[::1]:3000`，curl `127.0.0.1:3000` 会 502，用 `localhost`。
- **管理端若 dev server 被杀**（vite 预构建 commit 触发沙箱 safe-delete >50 文件）：改用 `npm run build` + `vite preview`。
- 管理端登录页断言文案是「权限管理系统」。
- 规模基线：后端 21 个 Controller / 137 个 Java 文件 / 138 个端点；C 端 11 页 + 8 组件；管理端 28 页；权限码 34 个；角色 admin / tech_lead / user。
