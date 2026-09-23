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
